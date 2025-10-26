@echo off
REM 빌드 전 로직 테스트 실행 스크립트 (Windows)
REM 모든 핵심 로직을 테스트하여 빌드 전 문제를 사전 발견

echo ==========================================
echo 🚀 빌드 전 로직 테스트 시작
echo ==========================================

REM 프로젝트 루트로 이동
cd /d "%~dp0\.."

REM Gradle 테스트 실행
echo 📋 Android Instrumentation 테스트 실행 중...
gradlew.bat connectedAndroidTest --tests "com.ssj.statuswindow.LogicTestFramework" --info

REM 테스트 결과 확인
if %ERRORLEVEL% EQU 0 (
    echo ==========================================
    echo ✅ 모든 테스트 통과!
    echo 🎉 빌드 승인 가능
    echo ==========================================
    exit /b 0
) else (
    echo ==========================================
    echo ❌ 테스트 실패!
    echo ⚠️ 빌드 전 수정 필요
    echo ==========================================
    exit /b 1
)
