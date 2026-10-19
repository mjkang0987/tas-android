package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.domain.Assignee
import kr.co.takeaseat.app.domain.HistoryKind
import kr.co.takeaseat.app.domain.ReservationHistoryEntry
import kr.co.takeaseat.app.domain.StoreLabels
import kr.co.takeaseat.app.domain.formatHistoryTimestamp
import kr.co.takeaseat.app.domain.historyDiffs
import kr.co.takeaseat.app.domain.historyKind
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.LabelBadge
import kr.co.takeaseat.app.ui.components.LabelTone
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

// 웹 HISTORY_ITEM_STYLES — 종류별 카드 바탕·테두리(변경은 옅은 회색, 테두리 없음)
private fun cardColors(kind: HistoryKind): Pair<Color, Color> = when (kind) {
    HistoryKind.Cancelled -> Color(241, 245, 249).copy(alpha = 0.92f) to Color(203, 213, 225).copy(alpha = 0.95f)
    HistoryKind.Noshow -> TasColors.DangerBg to TasColors.DangerBorder
    HistoryKind.Completed -> Color(0xFFE6F4EA) to Color(0xFFCDEAD6)
    HistoryKind.Restored -> Color(0xFFEFF6FF) to Color(0xFFBFDBFE)
    HistoryKind.Edit -> TasColors.Black10 to Color.Transparent
}

// 웹 StyledHistoryTypeBadge tone — 취소 neutral · 노쇼 danger · 완료 success · 예약전환 brand(네이버 초록) · 변경 info
private fun badgeTone(kind: HistoryKind) = when (kind) {
    HistoryKind.Cancelled -> LabelTone.Neutral
    HistoryKind.Noshow -> LabelTone.Danger
    HistoryKind.Completed -> LabelTone.Success
    HistoryKind.Restored -> LabelTone.Brand
    HistoryKind.Edit -> LabelTone.Info
}

/**
 * 웹 `ReservationHistoryLayer` — 예약 변경 이력(최근 것부터). 카드마다 시각 + 종류 배지, 아래에 바뀐 칸(예전 → 새 값).
 * 상세 위에 겹쳐 뜨는 시트라 뒤 어둡기를 옅게(웹 rgba(0,0,0,.24)).
 */
@Composable
internal fun ReservationHistorySheet(
    history: List<ReservationHistoryEntry>,
    assignees: List<Assignee>,
    labels: StoreLabels,
    onDismiss: () -> Unit,
) {
    DetailSheet(
        title = "변경 이력",
        subtitle = "예약 상태와 시간, 서비스 변경 흐름을 시간순으로 보여줍니다.",
        onDismiss = onDismiss,
        scrim = Color.Black.copy(alpha = 0.24f),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            history.asReversed().forEach { entry ->
                val kind = historyKind(entry)
                val (bg, border) = cardColors(kind)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(bg, TasShapes.RadiusMd)
                        .border(1.dp, border, TasShapes.RadiusMd)
                        .padding(10.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatHistoryTimestamp(entry.timestamp), color = TasColors.DarkGray, fontSize = TasSizes.XSmallFont, modifier = Modifier.weight(1f))
                        LabelBadge(kind.label, badgeTone(kind), fontSize = TasSizes.TinyFont)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        historyDiffs(entry, assignees, labels).forEach { diff ->
                            // 웹 dt 40 + 사이 10 — '결제상태'처럼 네 글자는 40 을 조금 넘어 그대로 늘어난다(값과 붙지 않게)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(diff.label, color = TasColors.DarkGray, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.widthIn(min = 40.dp))
                                DiffValue(diff, TasSizes.XSmallFont, 6.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
