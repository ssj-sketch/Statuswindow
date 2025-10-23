package com.ssj.statuswindow.util

/**
 * SpendingHabitAnalyzer 사용 예시를 제공하는 도우미.
 *
 * 앱 내에서 미리보기/데모 데이터를 구성하거나 수동으로 Analyzer 출력을 확인할 때 활용할 수 있다.
 */
object SpendingHabitAnalyzerSamples {

    private const val shinhanSingleMessage =
        "신한카드(1054)승인 신*진 42,820원(일시불)10/20 14:59 주식회사 이마트 누적1,903,674"

    private val shinhanWithCancellationMessages = listOf(
        "신한카드(1054)승인 신*진 10,000원(일시불)10/20 10:00 스타벅스",
        "신한카드(1054)취소 신*진 3,000원(일시불)10/20 11:00 스타벅스",
        "신한카드(1054)승인 신*진 15,000원(일시불)10/21 09:00 버거킹"
    )

    /**
     * 단일 승인 알림을 기반으로 한 Shinhan 카드 사용 보고서.
     */
    val shinhanSingleReport: SpendingHabitReport by lazy {
        SpendingHabitAnalyzer.analyze(shinhanSingleMessage)
    }

    /**
     * 승인 + 취소 조합을 포함한 Shinhan 카드 사용 보고서.
     */
    val shinhanWithCancellationReport: SpendingHabitReport by lazy {
        SpendingHabitAnalyzer.analyze(shinhanWithCancellationMessages)
    }

    /**
     * 샘플 보고서 전체 모음. UI 프리뷰 등에서 순회하며 활용할 수 있다.
     */
    fun allSamples(): List<SpendingHabitReport> = listOf(
        shinhanSingleReport,
        shinhanWithCancellationReport
    )
}
