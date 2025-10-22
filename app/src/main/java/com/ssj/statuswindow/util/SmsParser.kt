package com.ssj.statuswindow.util

import com.ssj.statuswindow.model.CardEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.absoluteValue

/**
 * 카드사별 고급 파서 (한국 카드 메시지/알림 대응)
 *
 * 지원 예시 (요청하신 신한카드 문구):
 * "신한카드(1054)승인 신*진 12,700원(일시불)10/13 15:48 스타벅스 누적1,860,854원"
 *  → 카드: 신한(1054), 금액: +12700, 할부: 0, 시간: 2025-10-13 15:48:00, 가맹점: 스타벅스,
 *    누적: 1860854, 소유자: 신*진
 *
 * 일반 규칙:
 *  - 취소 키워드 감지 → 금액을 음수
 *  - 시간 포맷 없으면 now()
 *  - 가맹점은 시간/금액/키워드/카드사 토큰 제거 후 남은 텍스트로 추정
 *  - "카테고리 : X" 형식이 있으면 그대로 저장. 없으면 간단한 맵핑으로 추정(스타벅스→카페 등)
 */
object SmsParser {

    // 시간: "2025-10-22 14:33", "2025/10/22 14:33", "10/13 15:48", "14:33"
    private val timeRegexFull = Regex("""(\d{4}[-./]\d{2}[-./]\d{2})\s+(\d{2}:\d{2}(:\d{2})?)""")
    private val timeRegexShort = Regex("""(\d{2}[/.-]\d{2})\s+(\d{2}:\d{2}(:\d{2})?)""")
    private val timeOnlyRegex = Regex("""\b(\d{2}:\d{2}(:\d{2})?)\b""")

    // 금액: "1,234", "1234", "1,234,567원", "KRW 12,300", "₩3,000"
    private val moneyRegex = Regex("""(?:KRW|₩)?\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원?""")

    // 취소 판별
    private val cancelRegex = Regex("""(취소|승인취소|취소승인|취소되었습니다|취소됨)""")

    // 카드사/번호: "신한카드(1054)", "국민카드(1234)" 등
    private val cardRegex = Regex("""\b(신한|국민|KB|삼성|현대|롯데|우리|하나|NH|농협|BC|비씨|카카오|씨티|신협|수협|우체국|SC|IBK|기업|제일|새마을|MG)\s*카드\((\d{3,4})\)""")

    // 명의자(마스킹): "신*진", "홍*동" 같은 3~4자 형태
    private val holderRegex = Regex("""\b[가-힣A-Za-z]\*?[가-힣A-Za-z]{1,2}\b""")

    // 할부: "(일시불)" 또는 "(N개월)"
    private val installmentRegex = Regex("""\((일시불|(\d{1,2})개월)\)""")

    // 누적 금액: "누적1,860,854원" 또는 "누적 : 1,860,854원"
    private val cumulativeRegex = Regex("""누적\s*:?[\s]*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")

    // 카테고리 명시: "카테고리 : 간식"
    private val categoryExplicitRegex = Regex("""카테고리\s*[:：]\s*([^\s]+)""")

    // 승인/일반 키워드(가맹점 추정 시 제거용)
    private val noiseTokens = listOf(
        "승인", "이용", "결제", "사용", "매입", "일시불", "국내", "해외",
        "취소", "승인취소", "취소승인", "취소되었습니다", "취소됨",
        "원", "KRW", "₩", "누적", "잔액", "포인트", "카드", "체크", "신용", "알림"
    )

    private val cardVendors = listOf(
        "신한", "국민", "KB", "삼성", "현대", "롯데", "우리", "하나", "NH", "농협",
        "BC", "비씨", "카카오", "씨티", "신협", "수협", "우체국", "SC", "IBK", "기업", "제일", "새마을", "MG"
    )

    // 간단 카테고리 추정 맵
    private val merchantCategoryMap = mapOf(
        "스타벅스" to "카페",
        "투썸" to "카페",
        "이디야" to "카페",
        "맥도날드" to "패스트푸드",
        "버거킹" to "패스트푸드",
        "던킨" to "간식",
        "베스킨" to "간식",
        "편의점" to "편의점"
    )

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun parse(input: String): List<CardEvent> {
        if (input.isBlank()) return emptyList()

        // 줄 단위 처리 (여러 건이 한 번에 들어와도 대응)
        val lines = input
            .split("\n")
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }

        val out = mutableListOf<CardEvent>()

        for (ln in lines) {
            val cancel = cancelRegex.containsMatchIn(ln)

            val moneyMatch = moneyRegex.find(ln)
            val amountAbs = moneyMatch?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0L
            val amount = if (cancel) -amountAbs else amountAbs

            // 카드사/번호
            val cardBrand = cardRegex.find(ln)?.groupValues?.getOrNull(1)
            val cardLast4 = cardRegex.find(ln)?.groupValues?.getOrNull(2)

            // 명의자(가능하면 추출) - 카드사 바로 뒤/이름 위치에서 잘 매칭되는 편
            val holderMasked = holderRegex.find(ln)?.value

            // 할부
            val installmentMonths: Int? = installmentRegex.find(ln)?.let { m ->
                val g1 = m.groupValues.getOrNull(1) ?: ""
                val g2 = m.groupValues.getOrNull(2) ?: ""
                when {
                    g1.contains("일시불") -> 0
                    g2.isNotBlank() -> g2.toIntOrNull()
                    else -> null
                }
            }

            // 누적 금액
            val cumulativeAmount: Long? = cumulativeRegex.find(ln)?.groupValues
                ?.getOrNull(1)?.replace(",", "")?.toLongOrNull()

            // 카테고리(명시 우선)
            val categoryExplicit = categoryExplicitRegex.find(ln)?.groupValues?.getOrNull(1)
            val time = extractTime(ln)

            val merchant = guessMerchant(
                line = ln,
                removeRegexes = listOf(
                    timeRegexFull, timeRegexShort, timeOnlyRegex,
                    moneyRegex, cancelRegex, cardRegex, installmentRegex, cumulativeRegex, categoryExplicitRegex
                )
            ).ifBlank { "미상" }

            val category = categoryExplicit ?: merchantCategoryMap.entries
                .firstOrNull { (k, _) -> merchant.contains(k, ignoreCase = true) }
                ?.value

            out.add(
                CardEvent(
                    id = UUID.randomUUID().toString(),
                    time = time,
                    merchant = merchant,
                    amount = amount,
                    sourceApp = "SMS",
                    raw = ln,
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    installmentMonths = installmentMonths,
                    category = category,
                    cumulativeAmount = cumulativeAmount,
                    holderMasked = holderMasked
                )
            )
        }

        return out
    }

    // ----- 내부 유틸 -----

    private fun extractTime(s: String): String {
        return when {
            timeRegexFull.containsMatchIn(s) -> normalizeDateTime(timeRegexFull.find(s)!!.value)
            timeRegexShort.containsMatchIn(s) -> normalizeShortDateTime(timeRegexShort.find(s)!!.value)
            timeOnlyRegex.containsMatchIn(s) -> normalizeTimeOnly(timeOnlyRegex.find(s)!!.value)
            else -> nowStr()
        }
    }

    private fun nowStr(): String = LocalDateTime.now().format(fmt)

    private fun normalizeDateTime(s: String): String {
        // "2025/10/22 14:33" -> "2025-10-22 14:33:00"
        val t = s.replace("/", "-").replace(".", "-")
        return if (t.length == 16) "${t}:00" else t
    }

    private fun normalizeShortDateTime(s: String): String {
        // "10/13 15:48" → 올해 "2025-10-13 15:48:00"
        val now = LocalDateTime.now()
        val replaced = s.replace("/", "-").replace(".", "-")
        val parts = replaced.split(" ")
        val date = parts.getOrNull(0)?.split("-") ?: emptyList()
        val time = parts.getOrNull(1) ?: "00:00:00"
        val mm = date.getOrNull(0)?.padStart(2, '0') ?: "01"
        val dd = date.getOrNull(1)?.padStart(2, '0') ?: "01"
        val t = if (time.length == 5) "$time:00" else time
        return "${now.year}-${mm}-${dd} $t"
    }

    private fun normalizeTimeOnly(s: String): String {
        // "14:33" → 오늘 날짜 + "14:33:00"
        val now = LocalDateTime.now()
        val t = if (s.length == 5) "$s:00" else s
        return "${now.year}-${now.monthValue.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')} $t"
    }

    private fun guessMerchant(line: String, removeRegexes: List<Regex>): String {
        var text = line
        removeRegexes.forEach { re -> text = text.replace(re, " ") }

        // 카드사/노이즈 토큰 제거
        (cardVendors + noiseTokens).forEach { token ->
            text = text.replace(token, " ", ignoreCase = true)
        }

        // 특수문자/여분 공백 정리
        text = text.replace(Regex("""[^\p{L}\p{N}\s\-\&\(\)\[\]\.\,]"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // 너무 짧거나 의미 없으면 빈 문자열
        return if (text.length < 1) "" else text
    }
}
