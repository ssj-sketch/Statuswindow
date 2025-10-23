# StatusWindow 엑셀 내보내기 샘플 파일

## 📁 파일 목록

### CSV 샘플 파일
- `sample_card_transactions.csv` - 거래내역 샘플
- `sample_monthly_summary.csv` - 월별 요약 샘플  
- `sample_comprehensive_report.csv` - 통합 리포트 샘플

### 문서 파일
- `엑셀_내보내기_가이드.md` - 상세 사용 가이드
- `엑셀_샘플_다운로드.html` - 웹 브라우저에서 볼 수 있는 샘플 뷰어

### 스크립트 파일
- `create_sample_excel.py` - Python으로 엑셀 파일 생성하는 스크립트

## 🚀 빠른 시작

1. **CSV 파일 다운로드**: 원하는 샘플 CSV 파일을 다운로드
2. **엑셀에서 열기**: Microsoft Excel, Google Sheets, LibreOffice Calc에서 열기
3. **데이터 분석**: 필터, 차트, 피벗 테이블 등 활용

## 📊 데이터 구조

### 거래내역 (card_transactions.csv)
```
날짜,시간,카드사,카드번호,거래유형,사용자,금액,할부,가맹점,누적금액,카테고리,메모
```

### 월별 요약 (monthly_summary.csv)
```
월별 카드 사용 요약
상위 가맹점
카드별 요약
일별 요약
카테고리별 요약
```

### 통합 리포트 (comprehensive_report.csv)
```
월별 카드 사용 요약
결제 예상액
상위 가맹점
카드별 요약
상세 거래 내역
```

## 💡 활용 팁

- **예산 관리**: 월별 요약을 통해 소비 패턴 파악
- **소비 분석**: 카테고리별, 가맹점별 소비 현황 분석
- **예측**: 결제 예상액을 통한 월말 예산 계획
- **시각화**: 엑셀 차트로 소비 패턴 시각화

## 🔧 앱 사용법

1. StatusWindow 앱 실행
2. 메인 화면 → "엑셀로 내보내기" 버튼 클릭
3. 원하는 옵션 선택:
   - 거래내역만 내보내기
   - 월별 요약만 내보내기
   - 통합 리포트 내보내기
4. Downloads/StatusWindow/ 폴더에서 파일 확인

## 📱 지원 앱

- **Android**: StatusWindow 앱
- **파일 형식**: CSV (UTF-8 인코딩)
- **호환성**: Microsoft Excel, Google Sheets, LibreOffice Calc

## ⚠️ 주의사항

- CSV 파일은 UTF-8 인코딩으로 저장됩니다
- 한글 데이터가 포함된 경우 엑셀에서 열 때 인코딩을 확인하세요
- 파일명에 타임스탬프가 포함되어 중복 저장을 방지합니다
