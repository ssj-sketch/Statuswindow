package com.ssj.statuswindow.test

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ssj.statuswindow.ui.components.*

/**
 * 간단한 컴포넌트 검증 시스템
 * 빌드 전에 컴포넌트들의 기본 동작을 검증
 */
class SimpleComponentValidator {
    
    companion object {
        private const val TAG = "ComponentValidator"
        
        /**
         * 모든 컴포넌트 검증 실행
         */
        fun validateAllComponents(context: Context): Boolean {
            var allValid = true
            
            try {
                // AppToolbar 검증
                if (!validateAppToolbar(context)) {
                    allValid = false
                }
                
                // SummaryCard 검증
                if (!validateSummaryCard(context)) {
                    allValid = false
                }
                
                // ProgressBarCard 검증
                if (!validateProgressBarCard(context)) {
                    allValid = false
                }
                
                // SectionHeader 검증
                if (!validateSectionHeader(context)) {
                    allValid = false
                }
                
                Log.i(TAG, "컴포넌트 검증 완료: ${if (allValid) "성공" else "실패"}")
                
            } catch (e: Exception) {
                Log.e(TAG, "컴포넌트 검증 중 오류 발생: ${e.message}", e)
                allValid = false
            }
            
            return allValid
        }
        
        /**
         * AppToolbar 검증
         */
        private fun validateAppToolbar(context: Context): Boolean {
            return try {
                val toolbar = AppToolbar(context)
                toolbar.setTitle("테스트")
                Log.d(TAG, "✅ AppToolbar 검증 성공")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ AppToolbar 검증 실패: ${e.message}", e)
                false
            }
        }
        
        /**
         * SummaryCard 검증
         */
        private fun validateSummaryCard(context: Context): Boolean {
            return try {
                val card = SummaryCard(context)
                card.setTitle("테스트")
                card.setPrimaryValue("100원")
                card.setSubtitle("테스트 설명")
                Log.d(TAG, "✅ SummaryCard 검증 성공")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ SummaryCard 검증 실패: ${e.message}", e)
                false
            }
        }
        
        /**
         * ProgressBarCard 검증
         */
        private fun validateProgressBarCard(context: Context): Boolean {
            return try {
                val card = ProgressBarCard(context)
                card.setup(
                    title = "테스트 진행률",
                    current = 50L,
                    target = 100L,
                    progressType = ProgressBarCard.ProgressType.SPENDING
                )
                Log.d(TAG, "✅ ProgressBarCard 검증 성공")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ ProgressBarCard 검증 실패: ${e.message}", e)
                false
            }
        }
        
        /**
         * SectionHeader 검증
         */
        private fun validateSectionHeader(context: Context): Boolean {
            return try {
                val header = SectionHeader(context)
                header.setTitle("테스트 섹션")
                header.setIcon(android.R.drawable.ic_menu_info_details)
                header.setActionButton("액션") { }
                Log.d(TAG, "✅ SectionHeader 검증 성공")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ SectionHeader 검증 실패: ${e.message}", e)
                false
            }
        }
        
        /**
         * 검증 결과를 Toast로 표시
         */
        fun showValidationResult(context: Context, isValid: Boolean) {
            val message = if (isValid) {
                "✅ 모든 컴포넌트 검증 성공!"
            } else {
                "❌ 컴포넌트 검증 실패 - 로그를 확인하세요"
            }
            
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

