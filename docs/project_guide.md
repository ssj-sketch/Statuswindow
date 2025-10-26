# StatusWindow 프로젝트 가이드

> 온디바이스 자산 관리 앱 - 프로젝트 구조 및 개발 가이드

## 📋 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [아키텍처](#아키텍처)
3. [프로젝트 구조](#프로젝트-구조)
4. [데이터베이스 설계](#데이터베이스-설계)
5. [주요 기능](#주요-기능)
6. [개발 가이드](#개발-가이드)
7. [UI 컴포넌트](#ui-컴포넌트)
8. [빌드 및 배포](#빌드-및-배포)

## 프로젝트 개요

StatusWindow는 스마트폰 알림을 분석하여 카드 거래, 은행 입출금, 수입 등을 자동으로 추적하는 온디바이스 자산 관리 앱입니다.

### 핵심 기능

- 📱 **SMS 알림 파싱**: 카드 승인, 입출금 내역을 자동으로 분석
- 💳 **카드 거래 관리**: 할부 정보, 청구금액 계산
- 🏦 **은행 거래 추적**: 입출금 내역 및 잔고 관리
- 💰 **수입 관리**: 급여 및 기타 수입 추적
- 📊 **엑셀 내보내기**: 거래 내역을 엑셀 파일로 저장
- 📝 **로그 관리**: 실시간 로그 수집 및 확인

## 아키텍처

### 레이어 구조

```
┌─────────────────────────────────────┐
│         UI Layer (Activities)       │
│  - MainActivity                     │
│  - CardDetailsActivity              │
│  - BankTransactionActivity          │
│  ...                                │
└─────────────────────────────────────┘
           ↓
┌─────────────────────────────────────┐
│       Repository Layer              │
│  - CardTransactionRepository         │
│  - BankTransactionRepository        │
│  - IncomeRepository                 │
└─────────────────────────────────────┘
           ↓
┌─────────────────────────────────────┐
│      Data Access Layer (DAO)        │
│  - CardTransactionDao                │
│  - BankTransactionDao                │
│  - IncomeTransactionDao              │
└─────────────────────────────────────┘
           ↓
┌─────────────────────────────────────┐
│      Database (Room)                │
│  - StatusWindowDatabase             │
└─────────────────────────────────────┘
```

### 주요 구성 요소

1. **Presentation Layer**: UI 구성 (Activities, Adapters)
2. **Domain Layer**: 비즈니스 로직 (Repositories, Services)
3. **Data Layer**: 데이터 저장소 (DAO, Entities)
4. **Util Layer**: 공통 유틸리티 (Parsers, Managers)

## 프로젝트 구조

```
app/src/main/java/com/ssj/statuswindow/
├── ai/                          # AI 파싱 엔진
│   ├── AiEngineFactory.kt
│   ├── KoreanAiEngine.kt
│   ├── AmericanAiEngine.kt
│   ├── income/
│   │   └── IncomeParsingAiEngine.kt
│   └── ...
│
├── database/                    # 데이터베이스
│   ├── StatusWindowDatabase.kt
│   ├── dao/                     # 데이터 액세스 객체
│   │   ├── CardTransactionDao.kt
│   │   ├── BankTransactionDao.kt
│   │   ├── LogDao.kt
│   │   └── ...
│   ├── entity/                  # 엔티티
│   │   ├── CardTransactionEntity.kt
│   │   ├── BankTransactionEntity.kt
│   │   ├── LogEntity.kt
│   │   └── ...
│   └── converter/               # 타입 컨버터
│       └── DateTimeConverter.kt
│
├── model/                       # 데이터 모델
│   ├── CardTransaction.kt
│   ├── FinancialInfo.kt
│   ├── RetirementAssetEstimate.kt
│   └── ...
│
├── notification/                # 알림 처리
│   └── StatusNotificationListener.kt
│
├── repo/                        # 저장소
│   ├── CardTransactionRepository.kt
│   ├── database/
│   │   ├── CardTransactionRepository.kt
│   │   ├── BankBalanceRepository.kt
│   │   └── ...
│   └── IncomeRepository.kt
│
├── service/                     # 서비스
│   ├── MerchantCategoryAiService.kt
│   ├── NotificationProcessingService.kt
│   ├── RetirementCalculationService.kt
│   └── ...
│
├── ui/                          # UI
│   ├── adapter/                 # 어댑터
│   │   ├── CardTransactionAdapter.kt
│   │   ├── LogAdapter.kt
│   │   └── ...
│   ├── components/              # 재사용 가능한 UI 컴포넌트
│   │   ├── AppToolbar.kt
│   │   ├── SummaryCard.kt
│   │   ├── ProgressBarCard.kt
│   │   └── ...
│   ├── MainActivity.kt
│   ├── CardDetailsActivity.kt
│   ├── BankTransactionActivity.kt
│   ├── LogViewerActivity.kt
│   └── ...
│
├── util/                        # 유틸리티
│   ├── LogManager.kt
│   ├── NavigationManager.kt
│   ├── ExcelExportManager.kt
│   ├── SmsParser.kt
│   └── ...
│
├── test/                        # 테스트
│   ├── SimpleComponentValidator.kt
│   ├── ButtonTestManager.kt
│   └── ...
│
└── viewmodel/                   # 뷰모델
    ├── MainViewModel.kt
    └── NotificationLogViewModel.kt
```

## 데이터베이스 설계

### 주요 테이블

#### 1. `card_transactions` - 카드 거래 내역

```kotlin
@Entity(tableName = "card_transactions")
data class CardTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardNumber: String,              // 카드 번호
    val merchant: String,                 // 가맹점
    val amount: Long,                     // 거래 금액
    val installmentMonths: Int = 0,      // 할부 개월 (0=일시불)
    val transactionDate: LocalDateTime,   // 거래일시
    val billingDate: LocalDateTime,       // 청구일
    val transactionType: String,         // 거래 유형
    val description: String,             // 설명
    val originalText: String             // 원본 SMS
)
```

#### 2. `bank_transaction` - 은행 거래 내역

```kotlin
@Entity(tableName = "bank_transaction")
data class BankTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String,                // 은행명
    val accountNumber: String,            // 계좌번호
    val accountType: String,              // 계좌 유형
    val transactionType: String,          // 거래구분 (입금/출금)
    val amount: Long,                     // 거래금액
    val balance: Long,                    // 잔액
    val description: String,              // 거래내용
    val transactionDate: LocalDateTime,   // 거래일시
    val memo: String,                     // 메모
    val originalText: String              // 원본 SMS
)
```

#### 3. `app_logs` - 애플리케이션 로그

```kotlin
@Entity(tableName = "app_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: String,                   // 로그 레벨 (DEBUG, INFO, WARN, ERROR)
    val tag: String,                     // 로그 태그
    val message: String,                  // 로그 메시지
    val timestamp: LocalDateTime,         // 타임스탬프
    val stackTrace: String? = null,       // 스택 트레이스
    val extra: String? = null             // 추가 정보
)
```

### 데이터베이스 버전 관리

```kotlin
@Database(
    entities = [
        CardTransactionEntity::class,
        BankTransactionEntity::class,
        IncomeTransactionEntity::class,
        BankBalanceEntity::class,
        LogEntity::class,
        ...
    ],
    version = 7,
    exportSchema = false
)
```

## 주요 기능

### 1. SMS 알림 파싱

- 카드 승인/취소 거래 파싱
- 할부 정보 추출 (일시불, 2개월, 3개월 등)
- 입출금 내역 파싱
- 수입(급여) 내역 파싱

### 2. 카드 거래 관리

- 거래 내역 조회 및 필터링
- 할부 정보 표시
- 청구금액 자동 계산
- 엑셀 내보내기

### 3. 은행 거래 관리

- 입출금 내역 추적
- 계좌별 잔고 관리
- 거래 내역 정렬 및 검색
- 엑셀 내보내기

### 4. 로그 시스템

- 실시간 로그 수집
- 로그 레벨별 필터링
- 로그 뷰어에서 확인
- 로그 엑셀 내보내기

### 5. 네비게이션

- 통합 네비게이션 드로어
- 일관된 헤더 디자인
- AppToolbar 컴포넌트 사용

## 개발 가이드

### 새 기능 추가

1. **데이터 모델 정의**
   ```kotlin
   // model/NewFeature.kt
   data class NewFeature(
       val id: Long,
       val name: String,
       ...
   )
   ```

2. **Entity 생성**
   ```kotlin
   // database/entity/NewFeatureEntity.kt
   @Entity(tableName = "new_features")
   data class NewFeatureEntity(...)
   ```

3. **DAO 작성**
   ```kotlin
   // database/dao/NewFeatureDao.kt
   @Dao
   interface NewFeatureDao {
       @Query("SELECT * FROM new_features")
       fun getAllFeatures(): Flow<List<NewFeatureEntity>>
       // ...
   }
   ```

4. **Repository 구현**
   ```kotlin
   // repo/NewFeatureRepository.kt
   class NewFeatureRepository(private val dao: NewFeatureDao) {
       fun getAllFeatures(): Flow<List<NewFeature>> { ... }
       // ...
   }
   ```

5. **UI 컴포넌트 생성**
   ```kotlin
   // ui/NewFeatureActivity.kt
   class NewFeatureActivity : AppCompatActivity() {
       // ...
   }
   ```

### 네비게이션 추가

1. **메뉴에 추가** (`app/src/main/res/menu/nav_menu.xml`)
   ```xml
   <item android:id="@+id/nav_new_feature" android:title="새 기능" />
   ```

2. **NavigationManager에 연결** (`util/NavigationManager.kt`)
   ```kotlin
   R.id.nav_new_feature -> {
       navigateToActivity(activity, NewFeatureActivity::class.java, currentActivityClass)
       true
   }
   ```

3. **액티비티 매핑**
   ```kotlin
   setActiveMenuItem()에서:
   NewFeatureActivity::class.java -> R.id.nav_new_feature
   ```

### 로깅 시스템 사용

```kotlin
// LogManager 사용
LogManager.getInstance().d("Tag", "Debug message")
LogManager.getInstance().i("Tag", "Info message")
LogManager.getInstance().w("Tag", "Warning message")
LogManager.getInstance().e("Tag", "Error message", exception)
```

### 엑셀 내보내기

```kotlin
// ExcelExportManager 사용
val exportManager = ExcelExportManager()
exportManager.exportCardTransactions(
    context = this,
    transactions = transactions,
    fileName = "카드거래내역.xlsx"
)
```

## UI 컴포넌트

### AppToolbar

공통 헤더 컴포넌트:

```kotlin
<com.ssj.statuswindow.ui.components.AppToolbar
    android:id="@+id/appToolbar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

설정:
```kotlin
appToolbar.setupWithDrawer(this, drawerLayout)
appToolbar.setTitle("화면 제목")
```

### SummaryCard

요약 정보 표시:

```kotlin
<com.ssj.statuswindow.ui.components.SummaryCard
    android:id="@+id/summaryCard"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

설정:
```kotlin
summaryCard.setTitle("총 거래")
summaryCard.setPrimaryValue("1,234,567원")
summaryCard.setSubtitle("12건")
```

### SectionHeader

섹션 헤더:

```kotlin
<com.ssj.statuswindow.ui.components.SectionHeader
    android:id="@+id/sectionHeader"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

설정:
```kotlin
sectionHeader.setTitle("상세 거래 내역")
```

## 빌드 및 배포

### 빌드 명령어

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease

# 앱 설치
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 앱 실행
adb shell am start -n com.ssj.statuswindow/.ui.MainActivity
```

### 빌드 설정

- **Gradle**: 8.9
- **Android Gradle Plugin**: 최신 버전
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 최신
- **Kotlin**: 최신 버전

### 빌드 최적화

`gradle.properties` 설정:

```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4096m
android.useAndroidX=true
android.enableJetifier=true
```

## 프로젝트 메뉴 구조

```
StatusWindow 앱
│
├─ 🏠 메인 기능
│   ├─ 🏠 홈 (MainActivity)
│   ├─ 💳 카드 사용내역 (CardDetailsActivity)
│   ├─ 📊 카드 테이블 (CardTableActivity)
│   ├─ 🏦 입출금내역 (BankTransactionActivity)
│   └─ 📈 입출금 테이블 (BankTransactionTableActivity)
│
├─ 🔧 개발자 도구
│   ├─ 📱 SMS 파싱 테스트 (SmsDataTestActivity)
│   ├─ 🔘 UI 자동 테스트 (ButtonTestActivity)
│   ├─ 📝 실시간 로그 (RealTimeLogActivity)
│   └─ 📋 로그 뷰어 (LogViewerActivity)
│
└─ ⚙️ 설정
    ├─ ⚙️ 앱 설정 (SettingsActivity)
    └─ ℹ️ 앱 정보 (Toast 메시지)
```

## 참고 자료

- [은행 계좌 분리 설계](./bank_account_separated_design.md)
- [프로젝트 README](../README.md)

---

_마지막 업데이트: 2025-01-XX_

