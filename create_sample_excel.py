import pandas as pd
import os
from datetime import datetime

# 샘플 데이터 생성
def create_sample_data():
    # 거래내역 데이터
    transactions_data = {
        '날짜': ['2024-01-15', '2024-01-15', '2024-01-14', '2024-01-14', '2024-01-13', 
                '2024-01-13', '2024-01-12', '2024-01-12', '2024-01-11', '2024-01-11'],
        '시간': ['14:30', '12:15', '19:45', '16:20', '20:30', '11:00', '18:15', '14:00', '21:00', '13:30'],
        '카드사': ['신한카드'] * 10,
        '카드번호': ['1234'] * 10,
        '거래유형': ['승인'] * 10,
        '사용자': ['본인'] * 10,
        '금액': [12700, 45000, 25000, 15000, 80000, 12000, 35000, 22000, 18000, 9500],
        '할부': ['일시불'] * 10,
        '가맹점': ['스타벅스 강남점', '이마트 과천점', '맥도날드 홍대점', 'GS25 강남역점', 
                 '롯데마트 잠실점', '투썸플레이스 신촌점', '교보문고 강남점', '올리브영 명동점', 
                 '배달의민족', 'CU 신촌점'],
        '누적금액': [12700, 57700, 82700, 97700, 177700, 189700, 224700, 246700, 264700, 274200],
        '카테고리': ['카페', '마트', '식당', '편의점', '마트', '카페', '서점', '화장품', '배달', '편의점'],
        '메모': [''] * 10
    }
    
    return pd.DataFrame(transactions_data)

def create_monthly_summary():
    # 월별 요약 데이터
    summary_data = {
        '항목': ['년도', '월', '총 사용액', '거래 건수', '평균 거래액'],
        '값': [2024, 1, 274200, 10, 27420.00]
    }
    
    # 상위 가맹점 데이터
    merchants_data = {
        '가맹점명': ['롯데마트 잠실점', '이마트 과천점', '교보문고 강남점', '맥도날드 홍대점', '올리브영 명동점'],
        '사용액': [80000, 45000, 35000, 25000, 22000],
        '거래건수': [1, 1, 1, 1, 1],
        '비율(%)': [29.18, 16.42, 12.77, 9.12, 8.03]
    }
    
    return pd.DataFrame(summary_data), pd.DataFrame(merchants_data)

def create_excel_files():
    # 거래내역 엑셀 파일 생성
    transactions_df = create_sample_data()
    
    with pd.ExcelWriter('sample_card_transactions.xlsx', engine='openpyxl') as writer:
        transactions_df.to_excel(writer, sheet_name='거래내역', index=False)
        
        # 워크시트 스타일링
        worksheet = writer.sheets['거래내역']
        for column in worksheet.columns:
            max_length = 0
            column_letter = column[0].column_letter
            for cell in column:
                try:
                    if len(str(cell.value)) > max_length:
                        max_length = len(str(cell.value))
                except:
                    pass
            adjusted_width = min(max_length + 2, 20)
            worksheet.column_dimensions[column_letter].width = adjusted_width
    
    # 월별 요약 엑셀 파일 생성
    summary_df, merchants_df = create_monthly_summary()
    
    with pd.ExcelWriter('sample_monthly_summary.xlsx', engine='openpyxl') as writer:
        summary_df.to_excel(writer, sheet_name='월별요약', index=False)
        merchants_df.to_excel(writer, sheet_name='상위가맹점', index=False)
        
        # 워크시트 스타일링
        for sheet_name in ['월별요약', '상위가맹점']:
            worksheet = writer.sheets[sheet_name]
            for column in worksheet.columns:
                max_length = 0
                column_letter = column[0].column_letter
                for cell in column:
                    try:
                        if len(str(cell.value)) > max_length:
                            max_length = len(str(cell.value))
                    except:
                        pass
                adjusted_width = min(max_length + 2, 20)
                worksheet.column_dimensions[column_letter].width = adjusted_width
    
    # 통합 리포트 엑셀 파일 생성
    with pd.ExcelWriter('sample_comprehensive_report.xlsx', engine='openpyxl') as writer:
        # 기본 요약
        summary_df.to_excel(writer, sheet_name='월별요약', index=False)
        
        # 상위 가맹점
        merchants_df.to_excel(writer, sheet_name='상위가맹점', index=False)
        
        # 거래내역
        transactions_df.to_excel(writer, sheet_name='거래내역', index=False)
        
        # 결제 예상액
        forecast_data = {
            '항목': ['현재 월 사용액', '예상 월 총액', '남은 일수', '일평균 소비액', '예상 추가 소비액', '신뢰도'],
            '값': [274200, 350000, 16, 27420.00, 75800, '85.00%']
        }
        forecast_df = pd.DataFrame(forecast_data)
        forecast_df.to_excel(writer, sheet_name='결제예상액', index=False)
        
        # 워크시트 스타일링
        for sheet_name in ['월별요약', '상위가맹점', '거래내역', '결제예상액']:
            worksheet = writer.sheets[sheet_name]
            for column in worksheet.columns:
                max_length = 0
                column_letter = column[0].column_letter
                for cell in column:
                    try:
                        if len(str(cell.value)) > max_length:
                            max_length = len(str(cell.value))
                    except:
                        pass
                adjusted_width = min(max_length + 2, 20)
                worksheet.column_dimensions[column_letter].width = adjusted_width

if __name__ == "__main__":
    try:
        create_excel_files()
        print("엑셀 파일이 성공적으로 생성되었습니다!")
        print("- sample_card_transactions.xlsx")
        print("- sample_monthly_summary.xlsx") 
        print("- sample_comprehensive_report.xlsx")
    except ImportError:
        print("pandas와 openpyxl 라이브러리가 필요합니다.")
        print("다음 명령어로 설치하세요: pip install pandas openpyxl")
    except Exception as e:
        print(f"오류 발생: {e}")
