# UpQuest

사진을 촬영해야만 알람을 해제할 수 있는 Android 기상 알람 앱.

## 소개

단순히 버튼을 눌러 알람을 끄고 다시 잠드는 것을 방지합니다.
**Normal 모드**는 스와이프로 해제하고, **Photo Verification 모드**는 미리 등록해 둔 사물(세면대, 정수기 등)을 직접 촬영해야만 알람이 종료됩니다.

## 주요 기능

| 기능 | 설명 |
|------|------|
| 알람 목록 | 등록된 알람 확인 및 활성화/비활성화 |
| 알람 설정 | 시간, 요일 반복, 해제 방식(Normal / Photo Verification) 설정 |
| 알람 울림 | 알람음 + 진동 재생, 해제 방식에 따른 UI 표시 |
| 사진 등록 | Photo Verification 모드에서 기준 사진 촬영 및 저장 |
| 테마 설정 | 라이트 / 다크 / 시스템 모드 선택 (DataStore 저장) |
| 다국어 지원 | 한국어 / 영어 (기기 언어 자동 적용) |

## 기술 스택

| 역할 | 라이브러리 |
|------|-----------|
| 언어 | Kotlin 2.3.10 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| 아키텍처 | MVVM + MVI + Clean Architecture |
| DI | Hilt |
| 내비게이션 | Navigation Compose (Type-safe) |
| 로컬 DB | Room |
| 설정 저장 | DataStore Preferences |
| 이미지 로딩 | Coil |
| 카메라 | CameraX |
| 알람 | AlarmManager + PendingIntent |
| 이미지 분석 | ML Kit Image Labeling (prod) |
| 테스트 | JUnit5 + MockK |

## 아키텍처

**Clean Architecture** 기반 3-레이어 구조와 단방향 데이터 흐름(UDF)을 준수합니다.

```
Presentation  →  Domain  →  Data
(ViewModel)      (UseCase)   (Repository)
```

모든 화면은 **Root / Screen**으로 분리합니다.

- **Root**: ViewModel 주입, 내비게이션 담당
- **Screen**: ViewModel 미의존, 순수 UI (Preview·단위 테스트 가능)

## Flavor

| Flavor | 용도 |
|--------|------|
| `dev` | Mock Repository 기반 UI 개발·검증 |
| `prod` | 실제 Repository 및 ML Kit 연동 |

## 빌드

```bash
# dev 디버그 빌드
./gradlew assembleDevDebug

# prod 릴리즈 빌드
./gradlew assembleProdRelease

# 단위 테스트
./gradlew testDevDebugUnitTest

# Lint
./gradlew lintDevDebug
```

## 요구사항

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **AGP**: 9.0.1
