# 💰 온디바이스 은퇴관리 앱 — Dashboard 화면 설계 및 와이어프레임

## 🎯 개요
은퇴 시 총자산을 최상단에 시각화하고, **소비 발생 시 숫자와 그래프가 동시에 하락하는 애니메이션**을 구현하는 Jetpack Compose 기반 UI 설계안입니다.

---

## 🧩 주요 컴포넌트
- **AnimatedWonNumber**: 소비 이벤트에 따라 스프링 애니메이션으로 숫자 감소
- **SynchronizedSparkline**: 총자산 그래프가 소비 시 함께 하락
- **RetirementAssetHeader**: 국민연금/생활비/나이 등 메타 정보 포함
- **MonthlySpendSection**: 전월 대비 소비금액 및 진척도 표시
- **MonthlyIncomeSection**: 이달 소득 및 전월 대비 증감 표시
- **DashboardScreen**: 전체 레이아웃 조합 및 소비 이벤트 트리거

---

## 🧭 와이어프레임

```
┌────────────────────────────────────────────┐
│ 💰 은퇴 시 총자산: 58,420,000원 ↓          │
│ ───────────────────────────────────────── │
│   ▓▓▓▓▓▓▓▓░░░░ (그래프 하락 애니메이션)     │
│ 국민연금 1,200,000원 / 생활비 10,000,000원 │
│ 현재나이 45세 / 가입 235개월 / 은퇴 60세   │
│───────────────────────────────────────────│
│ 이달 소비금액 1,000,000원 (전월 1,600,000) │
│ [███████████░░░░░░░░░░░░░░] 70%            │
│───────────────────────────────────────────│
│ 이달 소득금액 6,000,000원 (+1,200,000)     │
└────────────────────────────────────────────┘
```

---

## 🧱 Jetpack Compose 코드 예시

```kotlin
// DashboardScreen.kt
@file:Suppress("FunctionName", "MagicNumber")

package com.example.retire.ui

import android.icu.text.NumberFormat
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.random.Random

// 주요 데이터 클래스 및 컴포넌트 정의
// (AnimatedWonNumber, SynchronizedSparkline, RetirementAssetHeader, MonthlySpendSection 등)
// 상세 코드는 프로젝트의 DashboardScreen.kt 참고
```

---

## ⚙️ 애니메이션 로직 요약

| 트리거 | 동작 | 영향 컴포넌트 |
|--------|------|----------------|
| 소비 알림 수신 | dropProgress 0→1→0 | AnimatedWonNumber, SynchronizedSparkline |
| 총자산 업데이트 | 숫자 보간 애니메이션 | AnimatedWonNumber |
| 월별 데이터 변경 | LinearProgressIndicator 갱신 | MonthlySpendSection |

---

## 📊 연동 데이터 예시

| 항목 | 예시 값 |
|------|-----------|
| 총자산 | ₩58,420,000 |
| 국민연금(월) | ₩1,200,000 |
| 생활비(월) | ₩10,000,000 |
| 현재나이 | 45세 |
| 가입개월 | 235개월 |
| 희망은퇴나이 | 60세 |
| 이달 소비금액 | ₩1,000,000 |
| 전월 소비금액 | ₩1,600,000 |
| 소비진척도 | 70% |
| 이달 소득금액 | ₩6,000,000 |
| 전월 대비 증감 | +₩1,200,000 |

---

## 💡 Cursor 프롬프트 예시

```md
# Prompt: Create animated retirement dashboard screen

Generate a Kotlin Jetpack Compose UI that visualizes total retirement assets at the top,
and synchronizes a numerical drop with a line graph animation when spending occurs.

Include:
- Animated total asset number
- Sparkline graph that dips with spending
- Pension, cost, and retirement info rows
- Monthly spend & income cards
- Material3 theme and responsive layout
```
