package kr.co.takeaseat.app.domain

import java.text.NumberFormat
import java.util.Locale

// 웹 `formatPrice`(toLocaleString('ko-KR') + '원') · `formatTel` 이식.

private val koNumber: NumberFormat = NumberFormat.getIntegerInstance(Locale.KOREA)

fun formatPrice(price: Int): String = koNumber.format(price) + "원"

/** 11자리 010-1234-5678, 10자리 02x-xxx-xxxx 꼴, 나머지는 그대로. */
fun formatTel(tel: String): String {
    val digits = tel.filter { it.isDigit() }
    return when (digits.length) {
        11 -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
        10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
        else -> tel
    }
}
