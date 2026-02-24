# UpQuest 앱
사용자들의 아침을 깨워주는 알림 앱.

## 1. 앱 목적 및 대상 사용자

- 앱 목적: 사용자가 단순히 알람을 끄고 다시 자는 것을 방지하기 위해, '일반 버튼' 방식과 더불어 '미리 설정된 사진과 동일한 대상을 촬영'해야만 알람이 종료되는 기능을 제공하여 확실한 기상을 도움.
- 대상 사용자: 아침에 일어나기 힘들어하는 사용자(헤비 슬리퍼), 특정 루틴(영양제 먹기, 화장실 가기 등)과 함께 잠을 깨고 싶은 사용자.
- 왜 필요한가?: 기존의 단순한 알람은 무의식중에 끄기 쉽지만, 특정 사물을 식별하는 인터랙션을 강제함으로써 뇌를 활성화함. 초기 단계에서는 이미지 인식 로직의 복잡성을 배제하고 **Mock Data(이미 일치하는 것으로 간주하거나 로컬 비교)**를 통해 UI/UX를 빠르게 검증하기 위함.

---
## 2. 주요 기능 및 페이지 구성
### 필수 화면 (Screen):

- 알람 리스트 (Home): 설정된 알람 목록을 확인하고 활성화/비활성화할 수 있는 메인 화면.
- 알람 설정 (Edit/Create): 시간, 요일 반복, 알람음, **해제 방법(일반/사진)** 을 설정하는 화면.
- 알람 울림 (Alarm Alert): 알람이 울릴 때 나타나는 화면. 해제 방식에 따라 '버튼' 또는 '카메라 촬영 버튼' 노출.
- 사진 등록/확인 (Photo Setup): 사진 인증 모드 사용 시, 기준이 될 사진을 미리 찍어서 저장하는 화면.

### 핵심 기능:

- 해제 모드 1 (Normal): 전통적인 스와이프 또는 버튼 클릭 종료.
- 해제 모드 2 (Photo Verification):
  - 알람 설정 시 특정 사물(예: 세면대, 정수기) 사진 등록.
  - 알람 발생 시 카메라 프리뷰 실행 -> 촬영 -> 기준 사진과 비교(유사도 체크) -> 일치 시 종료.
- 백그라운드 실행: 앱이 닫혀 있거나 도즈 모드(Doze Mode) 상태에서도 정확한 시간에 알람 발생 (AlarmManager & PendingIntent 활용).
- Mock Data 관리:
  - AlarmRepository 인터페이스를 생성하여 초기에는 MockAlarmRepository에서 고정된 JSON 형태의 데이터를 반환하도록 구현.
  - 사진 비교 로직 또한 초기에는 '촬영 완료 시 무조건 성공'하는 Mock 로직으로 구현.

---
## 3. 콘텐츠 및 디자인 요구사항

- 디자인 시스템: Material Design 3 (M3) 기반의 최신 트렌드 적용.
  - Color: 잠을 깨우는 산뜻한 Vivid 컬러(오렌지, 블루).
  - Dynamic Color (Android 12+): 사용자 배경화면 기반의 동적 테마 색상 적용.
  - Typography: 가독성이 높은 Sans-serif 계열 (Pretendard 등).
- 테마 모드:
  - 라이트 모드 / 다크 모드 / 시스템 모드(기기 설정 자동 적용) 세 가지 지원.
  - 앱 내 설정에서 사용자가 직접 선택 가능하며, 기본값은 시스템 모드.
  - 선택값은 DataStore에 저장하여 앱 재실행 시에도 유지.
- 컴포넌트:
  - TopAppBar: 현재 페이지 제목 및 설정 아이콘.
  - AlarmCard: 리스트에서 개별 알람 정보를 보여주는 카드.
  - FloatingActionButton (FAB): 새 알람 추가 버튼.
- 애니메이션:
  - 알람이 울릴 때 카드가 흔들리는(Shake) 효과.
  - 화면 전환 시 Shared Element Transition (Compose 1.7+) 및 AnimatedContent를 활용한 부드러운 전환.
- 아이콘: androidx.compose.material:material-icons-extended 활용.

---
## 4. 아키텍처

### 4-1. 전체 구조
MVVM + MVI + Clean Architecture를 기반으로 단방향 데이터 흐름(UDF)을 준수하여 구현.

### 4-2. 레이어 정의

- **Presentation Layer**: ViewModel, UI State (MVI), Root/Screen 컴포저블
- **Domain Layer**: UseCase, Repository Interface, Domain Model
- **Data Layer**: RepositoryImpl, DataSource (Local/Mock), Room Entity, Mapper

### 4-3. 화면 구성 패턴 (Route/Screen 분리)

각 화면은 Root와 Screen으로 분리하여 ViewModel 의존성을 격리하고 단위 테스트 가능성을 확보.

- **Root (예: AlarmListRoot)**: ViewModel을 주입받아 상태와 이벤트를 Screen에 전달. 내비게이션 로직 포함.
- **Screen (예: AlarmListScreen)**: ViewModel 미의존. UI State와 이벤트 람다만 파라미터로 받음. Compose Preview 및 단위 테스트 가능.

```
예시)
@Composable
fun AlarmListRoot(viewModel: AlarmListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AlarmListScreen(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
fun AlarmListScreen(uiState: AlarmListUiState, onEvent: (AlarmListEvent) -> Unit) {
    // 순수 UI 로직만 포함
}
```

### 4-4. MVI 상태 관리

- **UiState**: StateFlow로 관리되는 불변 UI 상태.
- **Event (Intent)**: 사용자 액션을 나타내는 sealed class.
- **SideEffect**: 토스트, 내비게이션 등 일회성 이벤트는 SharedFlow로 처리.

### 4-5. 모듈 구조

초기에는 단일 모듈(app)로 시작하되, 패키지 구조는 멀티 모듈 전환을 고려하여 레이어 기준으로 분리.

```
app/
├── presentation/
│   ├── alarmlist/
│   │   ├── AlarmListRoot.kt
│   │   ├── AlarmListScreen.kt
│   │   └── AlarmListViewModel.kt
│   ├── alarmdetail/
│   └── components/        # 공용 컴포저블
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
└── data/
    ├── repository/
    ├── datasource/
    └── local/             # Room
```

---
## 5. 기술 스택 (Tech Stack)

- Language: Kotlin
- UI: Jetpack Compose
- Navigation: Jetpack Navigation Compose (Type-safe Navigation)
- Asynchronous: Kotlin Coroutines & Flow
- DI: Hilt (Mock/Real Repository 교체 용이성 확보)
- Local DB: Room (SQLite 기반)
- 설정 저장: DataStore Preferences (테마 모드, 언어 설정 등 경량 데이터)
- 이미지 처리: Coil (이미지 로딩), CameraX (사진 촬영 및 분석)
- 상태 관리: ViewModel & StateFlow / SharedFlow

---
## 6. 테스트 전략

- **Unit Test**: ViewModel, UseCase, Repository (JUnit5 + MockK)
  - Screen/Root 분리 덕분에 ViewModel을 독립적으로 테스트 가능.
- **UI Test**: Screen 단위 (ComposeTestRule)
  - Screen 컴포저블은 ViewModel 미의존이므로 가짜 UiState를 주입하여 테스트.
- **Mock 전략**: dev flavor에서 Fake Repository 구현체를 Hilt 모듈로 교체하여 주입.

---
## 7. 구현 계획

- 한 번에 완성하지 말고, 단계별 계획을 세워서 만들 것(매우 세세하게 나누기).
  - AGP 최신 stable 버전 기반으로 진행할 것.
  - [안드로이드 공식사이트](https://developer.android.com/)를 참고해서 진행할 것.
  - flavor 빌드 환경을 dev, prod로 나눠서 진행할 것.
    - **dev**: Mock Repository 주입, 네트워크 없이 UI 개발 및 검증.
    - **prod**: Real Repository 주입, 실제 기능 구현 및 연결.
- /docs/plan.md에 계획 내용을 작성해서 저장해서 실제 구현 시 사용할 것.
  - ex)
    - Phase 1. 환경 설정
    - 1-a. libs.versions.toml 버전 등록
    - 1-b. build.gradle.kts에 의존성 추가

### 1단계: Mock 기반 프런트엔드 (dev flavor) 구현

- Alarm 도메인 모델 및 Mock Repository 구현.
- Root/Screen 분리 패턴 적용한 전체 화면 UI 구현 (Compose).
- CameraX 기본 연동 (사진 저장 없이 촬영 Flow만 구현).
- 알람 매니저를 통한 로컬 알림 트리거 기본 테스트.
- ViewModel 단위 테스트 및 Screen UI 테스트 작성.

### 2단계: 로컬 데이터 및 실 기능 연동 (prod flavor)

- Room DB를 연동하여 실제 알람 설정 저장/수정/삭제.
- 사진 비교 알고리즘(OpenCV 또는 ML Kit) 적용하여 실제 일치 여부 판별.
- 실제 알람음(Ringtone) 및 진동 서비스 연결.

---
## 8. 다국어 지원 (i18n)

- 지원 언어: 한국어 (기본), 영어 (해외 시장 대응).
- 구현 방식: Android 표준 리소스 시스템 활용.
  - `res/values/strings.xml`: 영어 (기본 폴백).
  - `res/values-ko/strings.xml`: 한국어.
- 기기 언어 설정에 따라 자동 적용되며, 미지원 언어는 영어로 폴백.
- 날짜/시간 포맷은 Locale에 따라 자동 변환 (예: 12h/24h 표기).
- 모든 UI 문자열은 하드코딩 금지, 반드시 strings.xml 리소스 참조.

---
## 9. 코드 규칙

- 코드 주석은 한국어로 작성.
- 공용 컴포저블은 최대한 재사용 가능하도록 분리하여 `presentation/components` 패키지에 관리함.
- 각 화면은 Root/Screen으로 반드시 분리하여 작성함.
- 답변은 한국어로 할 것.