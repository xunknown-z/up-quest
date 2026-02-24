# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

UpQuest는 사진 촬영으로 알람을 해제하는 Android 기상 알람 앱입니다.
요구사항 전문: `docs/requirement.md` / 구현 계획: `docs/plan.md`

- **패키지명**: `com.goldennova.upquest`
- **Min SDK**: 24 / **Target SDK**: 36
- **언어**: Kotlin 2.0.21
- **UI**: Jetpack Compose (BOM 2024.09.00)
- **AGP**: 9.0.1

---

## 빌드 및 실행 명령어

```bash
# 전체 빌드 (dev flavor)
./gradlew assembleDevDebug

# 전체 빌드 (prod flavor)
./gradlew assembleProdRelease

# 단위 테스트 실행 (전체)
./gradlew testDevDebugUnitTest

# 단위 테스트 실행 (단일 클래스)
./gradlew testDevDebugUnitTest --tests "com.goldennova.upquest.presentation.alarmlist.AlarmListViewModelTest"

# 계측 테스트 실행
./gradlew connectedDevDebugAndroidTest

# Lint 검사
./gradlew lintDevDebug

# 클린 빌드
./gradlew clean assembleDevDebug
```

---

## 아키텍처

**MVVM + MVI + Clean Architecture** 기반, 단방향 데이터 흐름(UDF) 준수.

### 레이어 구조

```
Presentation  →  Domain  →  Data
(ViewModel)      (UseCase)   (Repository)
```

- **Presentation**: ViewModel, UiState(StateFlow), Event(sealed class), SideEffect(SharedFlow), Root/Screen 컴포저블
- **Domain**: UseCase, Repository 인터페이스, 도메인 모델
- **Data**: RepositoryImpl, DataSource, Room Entity, Mapper

### Root/Screen 분리 패턴 (필수)

모든 화면은 반드시 Root와 Screen으로 분리한다.

```kotlin
// Root: ViewModel 주입, 내비게이션 로직 담당
@Composable
fun AlarmListRoot(viewModel: AlarmListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AlarmListScreen(uiState = uiState, onEvent = viewModel::onEvent)
}

// Screen: ViewModel 미의존, 순수 UI만 담당 → 단위 테스트 / Preview 가능
@Composable
fun AlarmListScreen(uiState: AlarmListUiState, onEvent: (AlarmListEvent) -> Unit) { ... }
```

### MVI 상태 패턴

```kotlin
// UiState: 불변 데이터 클래스
data class AlarmListUiState(val alarms: List<Alarm> = emptyList(), val isLoading: Boolean = false)

// Event: 사용자 액션
sealed interface AlarmListEvent {
    data class ToggleAlarm(val id: Long) : AlarmListEvent
}

// SideEffect: 일회성 이벤트 (토스트, 내비게이션)
sealed interface AlarmListSideEffect {
    data class NavigateToDetail(val id: Long) : AlarmListSideEffect
}
```

---

## 패키지 구조

```
app/src/main/java/com/goldennova/upquest/
├── presentation/
│   ├── alarmlist/
│   │   ├── AlarmListRoot.kt
│   │   ├── AlarmListScreen.kt
│   │   └── AlarmListViewModel.kt
│   ├── alarmdetail/
│   ├── alarmalert/
│   ├── photosetup/
│   └── components/          # 공용 컴포저블
├── domain/
│   ├── model/
│   ├── repository/          # 인터페이스만
│   └── usecase/
└── data/
    ├── repository/          # RepositoryImpl
    ├── datasource/
    └── local/               # Room Entity, DAO, Database
```

---

## Flavor 전략

| Flavor | 용도 | Repository 주입 |
|--------|------|-----------------|
| `dev`  | Mock 데이터 기반 UI 개발/검증 | `FakeAlarmRepository` |
| `prod` | 실제 기능 구현 | `AlarmRepositoryImpl` |

Hilt 모듈을 flavor별 소스셋에 분리하여 Repository를 교체 주입한다.

```
app/src/dev/java/.../di/RepositoryModule.kt   → FakeAlarmRepository 바인딩
app/src/prod/java/.../di/RepositoryModule.kt  → AlarmRepositoryImpl 바인딩
```

---

## 주요 기술 스택

| 역할 | 라이브러리 |
|------|-----------|
| DI | Hilt |
| 내비게이션 | Navigation Compose (Type-safe) |
| 로컬 DB | Room |
| 설정 저장 (테마/언어) | DataStore Preferences |
| 이미지 로딩 | Coil |
| 카메라 | CameraX |
| 알람 | AlarmManager + PendingIntent |
| 테스트 | JUnit5 + MockK (Unit), ComposeTestRule (UI) |

---

## 다국어 / 테마

- **다국어**: `res/values/strings.xml` (영어 폴백) + `res/values-ko/strings.xml` (한국어). UI 문자열 하드코딩 금지.
- **테마**: 라이트 / 다크 / 시스템 모드 지원. 사용자 선택값은 DataStore에 저장. Dynamic Color (Android 12+) 적용.

---

## 코드 규칙

- 주석은 한국어로 작성.
- 각 화면은 Root/Screen으로 반드시 분리.
- 공용 컴포저블은 `presentation/components` 패키지에 관리.
- UI 문자열은 반드시 `strings.xml` 리소스 참조.
