#!/bin/bash

# 빌드 전 로직 테스트 실행 스크립트
# 모든 핵심 로직을 테스트하여 빌드 전 문제를 사전 발견

echo "=========================================="
echo "🚀 빌드 전 로직 테스트 시작"
echo "=========================================="

# 프로젝트 루트로 이동
cd "$(dirname "$0")/.."

# Gradle 테스트 실행
echo "📋 Android Instrumentation 테스트 실행 중..."
./gradlew connectedAndroidTest --tests "com.ssj.statuswindow.LogicTestFramework" --info

# 테스트 결과 확인
if [ $? -eq 0 ]; then
    echo "=========================================="
    echo "✅ 모든 테스트 통과!"
    echo "🎉 빌드 승인 가능"
    echo "=========================================="
    exit 0
else
    echo "=========================================="
    echo "❌ 테스트 실패!"
    echo "⚠️ 빌드 전 수정 필요"
    echo "=========================================="
    exit 1
fi
