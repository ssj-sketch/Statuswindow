# 온디바이스 자산·은퇴관리 앱 — Cursor + CI Starter

[![Android CI](https://github.com/USERNAME/REPO/actions/workflows/android_ci.yml/badge.svg)](https://github.com/USERNAME/REPO/actions)

> AI 기반 온디바이스 재정·은퇴관리 앱  
> Cursor + GitHub Actions + Kotlin + Compose

# 온디바이스 자산·은퇴관리 앱 — Cursor 스타터

본 리포지토리는 **Cursor AI 페어프로그래밍**을 중심으로 개발하는 안드로이드 앱의
프롬프트/워크플로우/템플릿을 제공합니다. 앱은 스마트폰 **알림**을 온디바이스에서 분석해
**수입·지출·자산**을 집계하고 **은퇴 시뮬레이션**을 수행합니다.

- 개발 방식: **Spec-First + TDD + Docs-as-Code + Cursor**
- 참고 앱: Notisave (알림 수집 UX) — https://play.google.com/store/apps/details?id=com.tenqube.notisave&hl=ko

## 📁 구조
```
/docs/
  ├─ prompts/
  │   ├─ parser_generator.md
  │   ├─ ui_component_prompt.md
  │   ├─ room_schema_prompt.md
  │   └─ retire_sim_prompt.md
  ├─ workflows/
  │   └─ dev_workflow.md
  └─ templates/
      ├─ PR_TEMPLATE.md
      └─ ISSUE_TEMPLATE.md
```

## 🚀 시작하기
1. **스펙 작성**: `/docs/specs/` (없으면 생성) 에 기능 스펙 Markdown 작성
2. **Cursor 실행**: 스펙을 입력으로 하여 `/docs/prompts/*.md` 프롬프트 사용
3. **코드 생성**: Kotlin(Compose), Room(Encrypted), WorkManager, TFLite(옵션)
4. **테스트**: junit5 + mockk — 실패 → 구현 → 통과 → 리팩터
5. **커밋/PR**: Conventional Commits, `/docs/templates/PR_TEMPLATE.md` 사용

## 🔐 권한/보안 가이드
- 필수 권한: `BIND_NOTIFICATION_LISTENER_SERVICE` (알림 접근)
- 로컬 암호화: Android Keystore + Encrypted Room
- 네트워크 없이 동작(비행기 모드). 온라인은 **옵트인** 업데이트만(주택공시가격/주식/환율).

## 🧪 테스트 전략
- 파서 정규식/NER 유닛 테스트
- 은퇴 시뮬레이션 경계값·랜덤성(Seeded) 테스트
- 현지화(통화/날짜/숫자 포맷) 스냅샷 테스트

## 🛠 빌드 환경 (예시)
- Android Gradle Plugin 최신, Kotlin, Compose Material 3
- Min SDK 26+, Target 최신
- 정적 분석: ktlint, detekt

## 🤖 Cursor 팁
- 프롬프트에 **입력/출력 스키마**와 **예시**를 명확히 제공
- 생성 코드에 테스트 스텁을 반드시 포함하도록 지시
- 민감 키/데이터는 절대 포함하지 않도록 지시

_Generated: 2025-10-24_
