package kr.co.takeaseat.app.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.addCustomer
import kr.co.takeaseat.app.domain.validateCustomerAdd
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.TasTextField

/**
 * 웹 `address/CustomerAddModal` — 고객명(자동 초점) · 연락처 · 오류 한 줄 · 취소/추가.
 * 규칙은 `domain/CustomerAdd.kt`. 게스트는 기기에 바로 저장되니 웹의 '추가 중…'(서버 대기)은 없다.
 */
@Composable
fun CustomerAddSheet(store: GuestStore, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var tel by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var errorSeq by rememberSaveable { mutableIntStateOf(0) } // 같은 오류로 다시 눌러도 다시 읽히게
    var added by remember { mutableStateOf(false) } // 시트가 닫히기 전 두 번 눌러도(엔터 + 버튼) 한 명만 — 웹 isSaving

    fun submit() {
        if (added) return
        val message = validateCustomerAdd(name, tel)
        if (message != null) {
            error = message
            errorSeq++
            return
        }
        added = true
        store.update { addCustomer(it, name, tel) }
        onDismiss()
    }

    DetailSheet(
        title = "고객 추가",
        onDismiss = onDismiss,
        footer = {
            ActionButton("취소", onClick = onDismiss)
            ActionButton("추가", tone = ActionTone.Primary, onClick = ::submit)
        },
    ) {
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() } // 웹 autoFocus
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TasTextField(
                label = "고객명",
                value = name,
                onValueChange = { name = it; error = null },
                placeholder = "이름 입력",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                // 오류는 그 칸에 붙인다 — 낭독기가 칸에서 '오류'를 읽게(웹은 칸 아래 한 줄)
                error = error?.takeIf { name.isBlank() },
                errorKey = errorSeq,
                modifier = Modifier.focusRequester(focus),
            )
            TasTextField(
                label = "연락처",
                value = tel,
                onValueChange = { tel = it; error = null },
                placeholder = "01012345678",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                error = error?.takeIf { name.isNotBlank() },
                errorKey = errorSeq,
            )
        }
    }
}
