package kr.co.takeaseat.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.ZoneId
import kr.co.takeaseat.app.MainActivity
import kr.co.takeaseat.app.R
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.DEFAULT_REMINDER_LEAD
import kr.co.takeaseat.app.domain.ReservationReminder
import kr.co.takeaseat.app.domain.reminderFor
import kr.co.takeaseat.app.domain.upcomingReminders

/**
 * 예약 알림 — 알릴 예약(domain/Reminder.kt)마다 기기 알람을 맞추고, 울리면 알림을 띄운다.
 * 정확한 시각이 지시자 결정이라 정확한 알람을 쓰고, 권한이 없는 기기(사용자가 끈 31~32)에서만 대략 알람으로 물러선다.
 * 알람은 예약 id 를 요청 코드로 써서 같은 예약은 덮어쓰고, 맞춰 둔 id 를 기억했다가 빠진 것은 지운다.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "reservation_reminder"
    const val EXTRA_RESERVATION_ID = "reservationId"
    private const val PREFS = "reminder"
    private const val KEY_LEAD = "leadMinutes"
    private const val KEY_SCHEDULED = "scheduledIds"
    private const val KEY_ASKED = "permissionAsked"
    private const val KEY_SIGNATURE = "signature"
    private const val ACTION_REFILL = "kr.co.takeaseat.app.REMINDER_REFILL"
    private const val REFILL_REQUEST = -1
    private const val REFILL_INTERVAL_MS = 24 * 60 * 60 * 1000L

    // 이 프로세스에서 한 번이라도 다 걸었는지. 강제 중지는 알람을 지우고 저장된 서명은 남기므로, 새 프로세스의 첫 sync 는 늘 다시 건다.
    @Volatile private var syncedInProcess = false

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 설정의 알림 시점(분, 0 = 끔). 기기 설정이라 웹과 같은 모양인 게스트 데이터(JSON)에는 넣지 않는다. */
    fun leadMinutes(context: Context): Int = prefs(context).getInt(KEY_LEAD, DEFAULT_REMINDER_LEAD)

    fun setLeadMinutes(context: Context, minutes: Int) {
        prefs(context).edit { putInt(KEY_LEAD, minutes) }
        sync(context, force = true)
    }

    /** 처음 실행 때 한 번만 권한을 묻는다 — 기본이 켜짐(30분 전)이라 묻지 않으면 조용히 안 울린다. */
    fun shouldAskPermissionOnce(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33 || hasNotificationPermission(context) || leadMinutes(context) <= 0) return false
        if (prefs(context).getBoolean(KEY_ASKED, false)) return false
        prefs(context).edit { putBoolean(KEY_ASKED, true) }
        return true
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "예약 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "예약 시각 전에 알려 줍니다."
            })
        }
    }

    /**
     * 지금 데이터·설정에 맞춰 알람을 다시 맞춘다. 데이터가 바뀔 때·알림이 울릴 때·재부팅 뒤·하루 한 번(7일 창 채우기)에 부른다.
     * 맞출 알람이 지난번과 같으면 건너뛴다(메모·서비스만 고쳐도 불려 매번 100번씩 다시 걸지 않게).
     * [force] — 알람이 사라졌을 수 있을 때(재부팅·업데이트·시각 변경·설정 변경·울림 뒤)는 같아도 다시 건다. 강제 중지 뒤엔 프로세스 첫 sync 가 다시 건다.
     */
    fun sync(context: Context, force: Boolean = false) {
        val app = context.applicationContext
        val snapshot = GuestStore.forApp(app).snapshot.value
        val customers = snapshot.customers.associate { it.id to it.name }
        val lead = leadMinutes(app)
        val reminders = upcomingReminders(snapshot.reservations, customers, lead, LocalDateTime.now())
        val signature = "$lead|" + reminders.joinToString(",") { "${it.reservationId}@${it.triggerAt}" }
        if (!force && syncedInProcess && signature == prefs(app).getString(KEY_SIGNATURE, null)) return
        val alarms = app.getSystemService(AlarmManager::class.java)
        val keep = reminders.map { it.reservationId }.toSet()
        prefs(app).getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .filter { it !in keep }
            .forEach { alarms.cancel(alarmIntent(app, it)) }
        reminders.forEach { schedule(app, alarms, it) }
        // 7일 밖 예약은 아직 안 걸려 있다 — 앱을 안 열어도 하루 한 번 창을 다시 채운다(대략 알람이면 충분).
        val refill = refillIntent(app)
        if (lead > 0) alarms.setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + REFILL_INTERVAL_MS, refill)
        else alarms.cancel(refill)
        prefs(app).edit {
            putStringSet(KEY_SCHEDULED, keep.map { it.toString() }.toSet())
            putString(KEY_SIGNATURE, signature)
        }
        syncedInProcess = true
    }

    private fun refillIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REFILL_REQUEST,
        Intent(context, ReminderRestoreReceiver::class.java).setAction(ACTION_REFILL),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun schedule(context: Context, alarms: AlarmManager, reminder: ReservationReminder) {
        val at = reminder.triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = alarmIntent(context, reminder.reservationId)
        val exact = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
        if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
    }

    private fun alarmIntent(context: Context, reservationId: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        reservationId,
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_RESERVATION_ID, reservationId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** 울릴 때 — 그사이 예약이 바뀌었을 수 있어 지금 데이터로 다시 확인하고 띄운다. */
    internal fun fire(context: Context, reservationId: Int) {
        val snapshot = GuestStore.forApp(context.applicationContext).snapshot.value
        val reservation = snapshot.reservations.firstOrNull { it.id == reservationId }
        val customers = snapshot.customers.associate { it.id to it.name }
        val reminder = reservation?.let { reminderFor(it, customers, leadMinutes(context)) }
        // 시각이 옮겨졌으면(1분 넘게 어긋나면) 이번 알람은 버린다 — sync 가 새 시각으로 다시 맞췄다.
        val stale = reminder == null || reminder.triggerAt.isAfter(LocalDateTime.now().plusMinutes(1))
        if (!stale && hasNotificationPermission(context)) notify(context, reminder!!)
        sync(context, force = true)
    }

    private fun notify(context: Context, reminder: ReservationReminder) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            reminder.reservationId,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_RESERVATION_ID, reminder.reservationId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(reminder.reservationId, notification)
        } catch (_: SecurityException) {
            // 권한을 막 거둔 경우 — 알림만 못 띄운다
        }
    }
}

/** 예약 알람이 울릴 때. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ReminderScheduler.EXTRA_RESERVATION_ID, -1)
        if (id >= 0) ReminderScheduler.fire(context, id)
    }
}

/** 재부팅·앱 업데이트·시각/시간대 변경·하루 한 번 채우기 — 기기 알람은 재부팅 때 지워지고, 시각이 바뀌면 울릴 때가 어긋난다. */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.sync(context, force = true)
    }
}
