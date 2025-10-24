package com.ssj.statuswindow.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.ssj.statuswindow.R

/**
 * SMS 입력 다이얼로그
 */
class SmsInputDialog(
    context: Context,
    private val onParseClick: (String) -> Unit
) : Dialog(context) {

    private lateinit var etSmsInput: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnParse: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_sms_input)
        
        setupViews()
        setupClickListeners()
        
        // 기본 샘플 텍스트 설정
        etSmsInput.setText(getSampleSmsText())
    }
    
    private fun setupViews() {
        etSmsInput = findViewById(R.id.etSmsInput)
        btnCancel = findViewById(R.id.btnCancel)
        btnParse = findViewById(R.id.btnParse)
    }
    
    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnParse.setOnClickListener {
            val smsText = etSmsInput.text.toString().trim()
            if (smsText.isNotEmpty()) {
                onParseClick(smsText)
                dismiss()
            }
        }
    }
    
    private fun getSampleSmsText(): String {
        return """신한카드(1054)승인 신*진 42,820원(일시불)10/22 14:59 주식회사 이마트 누적1,903,674
신한카드(1054)승인 신*진 98,700원(2개월)10/22 15:48 카톨릭대병원 누적1,960,854원
신한카드(1054)취소 신*진 12,700원(일시불)10/22 15:48 스타벅스 누적1,860,854원
신한카드(1054)승인 신*진 12,700원(일시불)10/22 15:48 스타벅스 누적1,860,854원
신한카드(1054)승인 신*진 42,820원(일시불)10/21 14:59 주식회사 이마트 누적1,903,674
신한카드(1054)승인 신*진 98,700원(3개월)10/21 15:48 카톨릭대병원 누적1,960,854원
신한카드(1054)승인 신*진 12,700원(일시불)10/21 15:48 스타벅스 누적1,860,854원
신한 10/11 21:54 100-***-159993 입금  2,500,000 잔액  3,700,000 급여
신한 10/11 21:54 100-***-159993 출금  3,500,000 잔액  1,200,000 신한카드
신한 09/11 21:54 100-***-159993 입금  2,500,000 잔액  5,000,000 신승진
신한 08/11 21:54 100-***-159993 입금  2,500,000 잔액  2,500,000 급여""".trimIndent()
    }
}
