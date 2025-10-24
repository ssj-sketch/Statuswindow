package com.ssj.statuswindow.model

/**
 * amount: 원 단위의 정수 금액(부호 포함)
 *  - 승인(일반 결제): 양수
 *  - 취소/승인취소: 음수
 *
 * 부가 정보(옵션):
 *  - cardBrand: "신한", "국민", "삼성" 등
 *  - cardLast4: "1054" 등 (괄호 안 3~4자리)
 *  - installmentMonths: 0 = 일시불, N개월 = N
 *  - category: "간식", "카페" 등 (문장에 표기돼 있으면 우선)
 *  - cumulativeAmount: 누적 사용 금액(Long)
 *  - holderMasked: "신*진" 등 (있다면)
 */
data class CardEvent(
    val id: String,                 // unique key (중복 제거 기준의 일부)
    val time: String,               // "yyyy-MM-dd HH:mm:ss"
    val merchant: String,           // 가맹점/설명
    val amount: Long,               // 정수 금액(부호 포함)
    val sourceApp: String,          // 수집 출처 (알림 패키지명 또는 "SMS")
    val raw: String = "",           // 원문(디버깅/추적용)

    // --- optional metadata ---
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val installmentMonths: Int? = null,
    val category: String? = null,
    val cumulativeAmount: Long? = null,
    val holderMasked: String? = null
)
