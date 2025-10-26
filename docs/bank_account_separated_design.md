# 은행 거래와 잔고 분리 설계

## 📋 개요

은행 거래(`bank_transactions`)와 거래 태이블(`bank_account_balances`)을 분리하여 관리합니다.

## 🎯 목적

- **거래 내역**과 **계좌 잔고**를 명확히 구분
- 거래 내역 기반으로 잔고 자동 계산
- 계좌별 거래 내역 및 잔고 추적 가능

## 📊 테이블 구조

### 1. `bank_transactions` (거래 내역)

```kotlin
@Entity(tableName = "bank_transactions")
data class BankTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 계좌 정보
    val bankName: String,           // 은행명 (예: 신한은행)
    val accountNumber: String,       // 계좌번호 (예: 110-123456-78900)
    val accountName: String? = null, // 계좌명 (옵션)
    
    // 거래 정보
    val transactionType: String,    // 거래구분 (예: 입금, 출금, 이체입, 이체출)
    val amount: Long,              // 거래금액
    val balance: Long,             // 거래 후 잔액
    val description: String,       // 거래내용 (예: 급여, 신한카드)
    val counterParty: String? = null, // 상대방 정보
    val category: String? = null,  // 카테고리 (예: 급여, 지출, 이체)
    
    // 거래일시
    val transactionDate: LocalDateTime,
    
    // 원본 정보
    val originalText: String? = null, // 원본 SMS 텍스트
    val memo: String? = null,       // 메모
    
    // 메타 정보
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isConfirmed: Boolean = true // 확정 여부
)
```

### 2. `bank_account_balances` (계좌 잔고)

```kotlin
@Entity(tableName = "bank_account_balances")
data class BankAccountBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 계좌 정보
    val bankName: String,           // 은행명
    val accountNumber: String,       // 계좌번호
    val accountName: String? = null, // 계좌명
    val accountType: String? = null, // 계좌 유형 (예: 입출금, 예금, 적금)
    
    // 잔고 정보
    val currentBalance: Long,       // 현재 잔고
    val availableBalance: Long? = null, // 가용 잔고
    val pendingBalance: Long? = null,   // 대기 중 잔고
    
    // 최신 거래 정보
    val lastTransactionDate: LocalDateTime?, // 마지막 거래일시
    val lastTransactionId: Long? = null,      // 마지막 거래 ID (외래키)
    
    // 메타 정보
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true,    // 활성 여부
    val memo: String? = null        // 메모
)
```

### 3. 인덱스 추가

```sql
-- 거래 내역 인덱스
CREATE INDEX idx_bank_transactions_bank_account ON bank_transactions(bankName, accountNumber);
CREATE INDEX idx_bank_transactions_date ON bank_transactions(transactionDate DESC);
CREATE INDEX idx_bank_transactions_type ON bank_transactions(transactionType);

-- 계좌 잔고 인덱스
CREATE INDEX idx_bank_account_balances_bank_account ON bank_account_balances(bankName, accountNumber);
CREATE INDEX idx_bank_account_balances_active ON bank_account_balances(isActive);
```

## 🔄 데이터 흐름

### 거래 추가 시

```
1. bank_transactions에 거래 추가
   ↓
2. 거래 후 잔액(balance)을 계좌별로 최신화
   ↓
3. bank_account_balances의 currentBalance 업데이트
```

### 잔고 조회 시

```
1. bank_account_balances에서 현재 잔고 조회
2. 필요시 bank_transactions에서 최신 거래 확인
3. 잔고 불일치 시 최신 거래 기준으로 재계산
```

## 📝 DAO 설계

### `BankTransactionDao` (기존 수정)

```kotlin
@Dao
interface BankTransactionDao {
    // 조회
    @Query("SELECT * FROM bank_transactions ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<BankTransactionEntity>>
    
    @Query("SELECT * FROM bank_transactions WHERE bankName = :bankName AND accountNumber = :accountNumber ORDER BY transactionDate DESC")
    fun getTransactionsByAccount(bankName: String, accountNumber: String): Flow<List<BankTransactionEntity>>
    
    @Query("SELECT * FROM bank_transactions WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<BankTransactionEntity>>
    
    // 최신 거래 조회
    @Query("SELECT * FROM bank_transactions WHERE bankName = :bankName AND accountNumber = :accountNumber ORDER BY transactionDate DESC LIMIT 1")
    suspend fun getLatestTransaction(bankName: String, accountNumber: String): BankTransactionEntity?
    
    // 잔고 조회 (거래 내역 기반)
    @Query("SELECT balance FROM bank_transactions WHERE bankName = :bankName AND accountNumber = :accountNumber ORDER BY transactionDate DESC LIMIT 1")
    suspend fun getLatestBalance(bankName: String, accountNumber: String): Long?
    
    // 집계
    @Query("SELECT SUM(amount) FROM bank_transactions WHERE bankName = :bankName AND accountNumber = :accountNumber AND transactionType = '입금'")
    suspend fun getTotalDeposit(bankName: String, accountNumber: String): Long?
    
    @Query("SELECT SUM(amount) FROM bank_transactions WHERE bankName = :bankName AND accountNumber = :accountNumber AND transactionType = '출금'")
    suspend fun getTotalWithdrawal(bankName: String, accountNumber: String): Long?
    
    // CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BankTransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<BankTransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: BankTransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: BankTransactionEntity)
    
    @Query("DELETE FROM bank_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
    
    // 중복 체크
    @Query("SELECT COUNT(*) FROM bank_transactions WHERE bankName = :bankName AND accountNumber = :accountNumber AND transactionDate = :date AND amount = :amount")
    suspend fun checkDuplicate(bankName: String, accountNumber: String, date: LocalDateTime, amount: Long): Int
}
```

### `BankAccountBalanceDao` (신규)

```kotlin
@Dao
interface BankAccountBalanceDao {
    // 조회
    @Query("SELECT * FROM bank_account_balances WHERE isActive = 1 ORDER BY bankName, accountName")
    fun getAllActiveBalances(): Flow<List<BankAccountBalanceEntity>>
    
    @Query("SELECT * FROM bank_account_balances WHERE id = :id")
    suspend fun getBalanceById(id: Long): BankAccountBalanceEntity?
    
    @Query("SELECT * FROM bank_account_balances WHERE bankName = :bankName AND accountNumber = :accountNumber")
    suspend fun getBalance(bankName: String, accountNumber: String): BankAccountBalanceEntity?
    
    // 집계
    @Query("SELECT SUM(currentBalance) FROM bank_account_balances WHERE isActive = 1")
    suspend fun getTotalBalance(): Long?
    
    @Query("SELECT COUNT(*) FROM bank_account_balances WHERE isActive = 1")
    suspend fun getActiveAccountCount(): Int
    
    // CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: BankAccountBalanceEntity): Long
    
    @Update
    suspend fun updateBalance(balance: BankAccountBalanceEntity)
    
    @Query("UPDATE bank_account_balances SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteBalance(id: Long)
    
    @Query("DELETE FROM bank_account_balances WHERE id = :id")
    suspend fun deleteBalance(id: Long)
    
    @Query("DELETE FROM bank_account_balances")
    suspend fun deleteAllBalances()
}
```

## 💼 Repository 패턴

### `BankAccountBalanceRepository`

```kotlin
class BankAccountBalanceRepository(
    private val balanceDao: BankAccountBalanceDao,
    private val transactionDao: BankTransactionDao
) {
    
    /**
     * 거래 추가 시 잔고 자동 업데이트
     */
    suspend fun updateBalanceFromTransaction(transaction: BankTransactionEntity) {
        // 현재 계좌 정보 조회
        val currentBalance = balanceDao.getBalance(transaction.bankName, transaction.accountNumber)
        
        // 새 잔고로 업데이트
        val newBalance = currentBalance?.copy(
            currentBalance = transaction.balance,
            lastTransactionDate = transaction.transactionDate,
            lastTransactionId = transaction.id,
            updatedAt = LocalDateTime.now()
        ) ?: BankAccountBalanceEntity(
            bankName = transaction.bankName,
            accountNumber = transaction.accountNumber,
            accountName = transaction.accountName,
            currentBalance = transaction.balance,
            lastTransactionDate = transaction.transactionDate,
            lastTransactionId = transaction.id
        )
        
        balanceDao.insertBalance(newBalance)
    }
    
    /**
     * 거래 내역 기반으로 잔고 재계산
     */
    suspend fun recalculateBalance(bankName: String, accountNumber: String) {
        val latestTransaction = transactionDao.getLatestTransaction(bankName, accountNumber)
        val calculatedBalance = transactionDao.getLatestBalance(bankName, accountNumber)
        
        latestTransaction?.let { transaction ->
            val currentBalance = balanceDao.getBalance(bankName, accountNumber)
            val updatedBalance = currentBalance?.copy(
                currentBalance = calculatedBalance ?: 0L,
                lastTransactionDate = transaction.transactionDate,
                lastTransactionId = transaction.id,
                updatedAt = LocalDateTime.now()
            ) ?: BankAccountBalanceEntity(
                bankName = bankName,
                accountNumber = accountNumber,
                accountName = transaction.accountName,
                currentBalance = calculatedBalance ?: 0L,
                lastTransactionDate = transaction.transactionDate,
                lastTransactionId = transaction.id
            )
            
            balanceDao.insertBalance(updatedBalance)
        }
    }
}
```

## 🎨 UI 활용

### 계좌 목록 화면

```kotlin
// bank_account_balances에서 계좌 목록 조회
val accountBalances: Flow<List<BankAccountBalanceEntity>> = 
    balanceRepository.getAllActiveBalances()

// 각 계좌의 최근 거래 내역은 bank_transactions에서 조회
val recentTransactions = transactionRepository
    .getTransactionsByAccount(bankName, accountNumber)
    .take(10) // 최근 10개만
```

### 거래 내역 화면

```kotlin
// bank_transactions에서 거래 내역 조회
val transactions: Flow<List<BankTransactionEntity>> = 
    transactionRepository.getTransactionsByAccount(bankName, accountNumber)
```

### 잔고 화면

```kotlin
// bank_account_balances에서 현재 잔고 조회
val currentBalance = balanceRepository.getBalance(bankName, accountNumber)

// bank_transactions에서 거래 내역 기반 잔고 확인
val calculatedBalance = transactionRepository.getLatestBalance(bankName, accountNumber)
```

## 🔄 데이터베이스 버전

```kotlin
@Database(
    entities = [
        BankTransactionEntity::class,        // 기존 유지
        BankAccountBalanceEntity::class,     // 새로 추가
        // ... 기존 엔티티들
    ],
    version = 8, // 버전 업데이트
    exportSchema = false
)
```

## ✅ 장점

1. **관심사 분리**: 거래 내역과 잔고를 명확히 구분
2. **성능**: 잔고 조회 시 복잡한 계산 없이 바로 조회 가능
3. **일관성**: 거래 내역으로 잔고 자동 계산 가능
4. **확장성**: 계좌별 추가 정보 관리 용이

## ⚠️ 주의사항

1. **데이터 동기화**: 거래 추가 시 잔고 자동 업데이트
2. **중복 방지**: 거래 중복 추가 방지 로직 필요
3. **잔고 불일치**: 거래 내역과 잔고가 불일치 시 재계산 기능 제공

