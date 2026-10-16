package kr.co.takeaseat.app.data

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kr.co.takeaseat.app.domain.LocalDbSnapshot
import kr.co.takeaseat.app.domain.emptyLocalDbSnapshot

/**
 * 게스트(로그인 전) 데이터 — 웹 `features/local-db/storage.ts` 의 localStorage `takeaseat.local-db.v1` 자리.
 * 앱 내부 파일 하나에 웹과 같은 모양의 JSON 으로 둔다.
 */
class GuestStore(private val file: File) {
    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<LocalDbSnapshot> = _snapshot.asStateFlow()

    private fun load(): LocalDbSnapshot {
        if (!file.exists()) return emptyLocalDbSnapshot().also(::write)
        return try {
            json.decodeFromString(LocalDbSnapshot.serializer(), file.readText())
        } catch (e: SerializationException) {
            // 웹과 같이 망가진 데이터는 빈 매장으로 다시 시작한다 — 다만 원본은 옆에 남긴다(웹은 덮어써 잃는다).
            file.copyTo(File(file.parentFile, "${file.name}.broken"), overwrite = true)
            emptyLocalDbSnapshot().also(::write)
        } catch (e: IllegalArgumentException) {
            file.copyTo(File(file.parentFile, "${file.name}.broken"), overwrite = true)
            emptyLocalDbSnapshot().also(::write)
        }
    }

    fun update(transform: (LocalDbSnapshot) -> LocalDbSnapshot) {
        val next = transform(_snapshot.value)
        write(next)
        _snapshot.value = next
    }

    // 임시 파일에 쓰고 바꿔치기 — 쓰다가 죽어도 이전 파일이 남는다.
    private fun write(snapshot: LocalDbSnapshot) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(json.encodeToString(LocalDbSnapshot.serializer(), snapshot))
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    companion object {
        const val FILE_NAME = "local-db.v1.json"

        val json = Json {
            ignoreUnknownKeys = true // 웹에 키가 늘어도 읽힌다
            explicitNulls = false // 없는 선택 값은 키를 쓰지 않는다(웹 JSON.stringify 와 같은 모양)
            coerceInputValues = true // null 이 들어온 기본값 필드는 기본값으로
        }
    }
}
