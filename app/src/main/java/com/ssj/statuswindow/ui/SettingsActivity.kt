package com.ssj.statuswindow.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ssj.statuswindow.databinding.ActivitySettingsBinding
import com.ssj.statuswindow.util.SettingsPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsPreferences: SettingsPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsPreferences = SettingsPreferences.getInstance(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "설정"

        setupSlider()
        setupClickListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupSlider() {
        val currentMinutes = settingsPreferences.getDuplicateDetectionMinutes()
        binding.sliderDuplicateMinutes.value = currentMinutes.toFloat()
        updateMinutesText(currentMinutes)

        binding.sliderDuplicateMinutes.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val minutes = value.toInt()
                updateMinutesText(minutes)
                settingsPreferences.setDuplicateDetectionMinutes(minutes)
            }
        }
    }

    private fun setupClickListeners() {
        // 설정은 슬라이더 변경 시 자동으로 저장되므로 별도 버튼 불필요
    }

    private fun updateMinutesText(minutes: Int) {
        binding.tvDuplicateMinutes.text = "${minutes}분"
    }
}
