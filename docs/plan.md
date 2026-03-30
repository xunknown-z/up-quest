# UpQuest 구현 계획

요구사항: `docs/requirement.md`

---

## 진행 상태 범례
- `[ ]` 미완료
- `[x]` 완료
- `[-]` 진행 중

---

## Phase 1. 환경 설정

### 1-a. libs.versions.toml — 버전 추가

`gradle/libs.versions.toml`에 아래 버전 항목 추가.

```toml
[versions]
# 기존 유지 ...
kotlin = "2.3.10"
hilt = "2.59.2"
hiltNavigationCompose = "1.3.0"
navigationCompose = "2.9.7"
room = "2.8.4"
datastorePreferences = "1.2.0"
coil = "2.7.0"
cameraX = "1.5.3"
materialIconsExtended = "1.7.8"
lifecycleViewmodelCompose = "2.10.0"
coroutinesTest = "1.10.2"
mockk = "1.14.9"
junitJupiter = "5.11.4"
ksp = "2.3.4"

[libraries]
# 기존 유지 ...
# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
# DataStore
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
# Coil
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
# CameraX
androidx-camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "cameraX" }
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "cameraX" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "cameraX" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "cameraX" }
# Material Icons Extended
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended", version.ref = "materialIconsExtended" }
# ViewModel Compose
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleViewmodelCompose" }
# 테스트
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junitJupiter" }

[plugins]
# 기존 유지 ...
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 1-b. build.gradle.kts (root) — 플러그인 추가

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.ksp) apply false
}
```

### 1-c. app/build.gradle.kts — 플러그인 및 의존성 추가

플러그인:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
}
```

`kotlin` 블록으로 JVM toolchain 설정 (AGP 9.0+에서 `kotlinOptions` 제거됨):
```kotlin
kotlin {
    jvmToolchain(11)
}
```
> `jvmToolchain(11)` 사용 시 `compileOptions`의 `sourceCompatibility` / `targetCompatibility`도 함께 대체되므로 기존 `compileOptions` 블록은 제거해도 무방.

의존성:
```kotlin
dependencies {
    // 기존 유지 ...

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Material Icons Extended
    implementation(libs.androidx.compose.material.icons.extended)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // 테스트
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

`android` 블록에 JUnit5 테스트 설정:
```kotlin
testOptions {
    unitTests.all {
        it.useJUnitPlatform()
    }
}
```

### 1-d. app/build.gradle.kts — Flavor 설정 추가

```kotlin
android {
    // ...
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("prod") {
            dimension = "environment"
        }
    }
}
```

### 1-e. UpQuestApplication.kt 생성

`app/src/main/java/com/goldennova/upquest/UpQuestApplication.kt`
- `@HiltAndroidApp` 어노테이션 적용
- `Application` 클래스 상속

`AndroidManifest.xml`의 `<application>` 태그에 `android:name=".UpQuestApplication"` 추가.

### 1-f. MainActivity.kt — @AndroidEntryPoint 추가

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

### 1-g. strings.xml 기본 구조 구성

`res/values/strings.xml` (영어 폴백):
- `app_name`, `alarm_list_title`, `add_alarm`, `dismiss_normal`, `dismiss_photo` 등 기본 키 정의.

`res/values-ko/strings.xml` (한국어):
- 동일 키에 한국어 번역 추가.

---

## Phase 2. 도메인 레이어 구축

### 2-a. Alarm 도메인 모델 정의

`domain/model/Alarm.kt`
```kotlin
data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek>,
    val label: String,
    val isEnabled: Boolean,
    val dismissMode: DismissMode,
)
```

### 2-b. DismissMode sealed class 정의

`domain/model/DismissMode.kt`
```kotlin
sealed class DismissMode {
    data object Normal : DismissMode()
    data class PhotoVerification(val referencePhotoPath: String?) : DismissMode()
}
```

### 2-c. AlarmRepository 인터페이스 정의

`domain/repository/AlarmRepository.kt`
```kotlin
interface AlarmRepository {
    fun getAlarms(): Flow<List<Alarm>>
    suspend fun getAlarmById(id: Long): Alarm?
    suspend fun insertAlarm(alarm: Alarm): Long
    suspend fun updateAlarm(alarm: Alarm)
    suspend fun deleteAlarm(id: Long)
    suspend fun toggleAlarm(id: Long, isEnabled: Boolean)
}
```

### 2-d. UseCase 5종 구현

각 UseCase는 `domain/usecase/` 패키지에 단일 책임으로 분리.

- `GetAlarmsUseCase.kt` — `AlarmRepository.getAlarms()` 래핑 (Flow 반환)
- `GetAlarmByIdUseCase.kt` — 단건 조회
- `SaveAlarmUseCase.kt` — insert/update 분기 처리 (`id == 0`이면 insert)
- `DeleteAlarmUseCase.kt` — 삭제
- `ToggleAlarmUseCase.kt` — 활성화/비활성화

### 2-e. UseCase 단위 테스트

`test/.../domain/usecase/GetAlarmsUseCaseTest.kt` 등 UseCase별 테스트 파일.
- `AlarmRepository`를 MockK로 mock 처리.
- 각 UseCase의 정상 동작 및 예외 케이스 검증.
- `UnconfinedTestDispatcher` 사용.

---

## Phase 3. 데이터 레이어 — Mock (dev flavor)

### 3-a. FakeAlarmDataSource 구현

`data/datasource/FakeAlarmDataSource.kt`
- 하드코딩된 알람 리스트 (최소 3개) 를 `MutableStateFlow`로 관리.
- CRUD 메서드 구현 (메모리 내 처리).

### 3-b. FakeAlarmDataSource 단위 테스트

`test/.../data/datasource/FakeAlarmDataSourceTest.kt`
- 초기 데이터 로드, insert / update / delete / toggle 시나리오 검증.
- Flow 방출 값 변화 검증 (`turbine` 또는 `toList()`).

### 3-c. FakeAlarmRepository 구현

`data/repository/FakeAlarmRepository.kt`
- `AlarmRepository` 인터페이스 구현.
- `FakeAlarmDataSource`를 내부적으로 사용.

### 3-d. FakeAlarmRepository 단위 테스트

`test/.../data/repository/FakeAlarmRepositoryTest.kt`
- `FakeAlarmDataSource`를 직접 주입하여 Repository CRUD 동작 검증.

### 3-e. Hilt 모듈 — dev 소스셋

`app/src/dev/java/com/goldennova/upquest/di/RepositoryModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindAlarmRepository(impl: FakeAlarmRepository): AlarmRepository
}
```

---

## Phase 4. 디자인 시스템 구축

### 4-a. ThemeMode 정의

`presentation/theme/ThemeMode.kt`
```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }
```

### 4-b. Color.kt — 브랜드 컬러 팔레트 정의

- Primary: 오렌지 계열 (예: `#FF6B35`)
- Secondary: 블루 계열 (예: `#1E88E5`)
- 라이트/다크 색상 쌍 정의.

### 4-c. Theme.kt — 라이트/다크/시스템 + Dynamic Color 지원

- `UpQuestTheme(themeMode: ThemeMode, ...)` 파라미터 추가.
- `when (themeMode)` 분기로 `darkTheme` 값 결정.
- Android 12+ Dynamic Color 지원 유지.

### 4-d. ThemePreferencesDataSource 구현

`data/datasource/ThemePreferencesDataSource.kt`
- `DataStore<Preferences>`를 주입받아 `ThemeMode` 읽기/쓰기.
- `themeMode: Flow<ThemeMode>` 프로퍼티 노출.

### 4-e. ThemePreferencesDataSource 단위 테스트

`test/.../data/datasource/ThemePreferencesDataSourceTest.kt`
- `DataStore` 인메모리 구현체(`PreferenceDataStoreFactory`)로 대체하여 읽기/쓰기 검증.
- 기본값(`SYSTEM`) 반환 검증.

### 4-f. ThemeRepository 인터페이스 및 구현

`domain/repository/ThemeRepository.kt` — `getThemeMode(): Flow<ThemeMode>`, `setThemeMode(mode: ThemeMode)`
`data/repository/ThemeRepositoryImpl.kt` — `ThemePreferencesDataSource` 사용.

### 4-g. ThemeRepositoryImpl 단위 테스트

`test/.../data/repository/ThemeRepositoryImplTest.kt`
- `ThemePreferencesDataSource`를 MockK로 mock 처리.
- `getThemeMode()` Flow 방출 및 `setThemeMode()` 위임 동작 검증.

### 4-h. Hilt 모듈 — DataStore 및 ThemeRepository 바인딩

`di/DataStoreModule.kt` — `DataStore<Preferences>` 싱글톤 프로바이더.
`di/ThemeModule.kt` — `ThemeRepository` 바인딩 (공통 소스셋).

### 4-i. MainViewModel 생성

- `ThemeRepository.getThemeMode()`를 StateFlow로 수집.
- `setContent { }` 내부에서 `themeMode` 값을 `UpQuestTheme`에 전달.

### 4-j. MainViewModel 단위 테스트

`test/.../MainViewModelTest.kt`
- `ThemeRepository`를 MockK로 mock 처리.
- 초기 테마 모드 수집 및 상태 반영 검증.

---

## Phase 5. 내비게이션 설정

### 5-a. Route 정의 (Type-safe Navigation)

`presentation/navigation/Route.kt`
```kotlin
@Serializable object AlarmList
@Serializable data class AlarmDetail(val alarmId: Long = -1L)  // -1 = 신규 생성
@Serializable object AlarmAlert
@Serializable data class PhotoSetup(val alarmId: Long)
@Serializable object Settings
```

### 5-b. AppNavHost 컴포저블 생성

`presentation/navigation/AppNavHost.kt`
- `NavHost`에 각 Route 등록.
- 각 화면의 `Root` 컴포저블 호출.

### 5-c. MainActivity에 AppNavHost 연결

---

## Phase 6. 알람 리스트 화면

### 6-a. UiState / Event / SideEffect 정의

`presentation/alarmlist/AlarmListContract.kt`
```kotlin
data class AlarmListUiState(
    val alarms: List<Alarm> = emptyList(),
    val isLoading: Boolean = false,
)
sealed interface AlarmListEvent {
    data class ToggleAlarm(val id: Long, val enabled: Boolean) : AlarmListEvent
    data class DeleteAlarm(val id: Long) : AlarmListEvent
    data object AddAlarm : AlarmListEvent
    data class EditAlarm(val id: Long) : AlarmListEvent
}
sealed interface AlarmListSideEffect {
    data class NavigateToDetail(val alarmId: Long) : AlarmListSideEffect
    data object NavigateToNewAlarm : AlarmListSideEffect
}
```

### 6-b. AlarmListViewModel 구현

- `GetAlarmsUseCase`, `ToggleAlarmUseCase`, `DeleteAlarmUseCase` 주입.
- `uiState: StateFlow<AlarmListUiState>` 노출.
- `sideEffect: SharedFlow<AlarmListSideEffect>` 노출.
- `onEvent(event: AlarmListEvent)` 이벤트 처리.

### 6-c. AlarmListViewModel 단위 테스트

`test/.../alarmlist/AlarmListViewModelTest.kt`
- `GetAlarmsUseCase`, `ToggleAlarmUseCase`, `DeleteAlarmUseCase`를 MockK로 mock 처리.
- 알람 목록 로드, 토글, 삭제 이벤트 처리 후 UiState 변화 검증.
- `SideEffect` 방출 검증 (NavigateToDetail, NavigateToNewAlarm).
- `UnconfinedTestDispatcher` 사용.

### 6-d. AlarmCard 공용 컴포저블 작성

`presentation/components/AlarmCard.kt`
- 알람 시간, 라벨, 요일, 해제 모드 아이콘, 활성화 토글 Switch 표시.
- `alarm: Alarm`, `onToggle`, `onEdit`, `onDelete` 람다 파라미터.

### 6-e. AlarmListScreen 컴포저블 작성

`presentation/alarmlist/AlarmListScreen.kt`
- `LazyColumn`으로 `AlarmCard` 목록 출력.
- 빈 상태(Empty State) UI 처리.
- `Scaffold` + `TopAppBar` + `FloatingActionButton` 구성.

### 6-f. AlarmListScreen UI 테스트

`androidTest/.../alarmlist/AlarmListScreenTest.kt`
- Mock UiState(알람 있음 / 없음) 주입 후 카드 렌더링 검증.
- FAB 클릭, 토글 클릭 이벤트 람다 호출 검증.

### 6-g. AlarmListRoot 컴포저블 작성

`presentation/alarmlist/AlarmListRoot.kt`
- `hiltViewModel()`로 ViewModel 주입.
- `sideEffect` 수집 후 내비게이션 처리.

---

## Phase 7. 알람 설정 화면 (Create / Edit)

### 7-a. UiState / Event / SideEffect 정의

`presentation/alarmdetail/AlarmDetailContract.kt`
- `hour`, `minute`, `repeatDays`, `label`, `dismissMode`, `isLoading` 등 포함.
- Event: `ChangeHour`, `ChangeMinute`, `ToggleDay`, `ChangeDismissMode`, `Save`, `Delete`.
- SideEffect: `NavigateBack`, `ShowError`.

### 7-b. AlarmDetailViewModel 구현

- `GetAlarmByIdUseCase`, `SaveAlarmUseCase`, `DeleteAlarmUseCase` 주입.
- `alarmId == -1L`이면 신규 생성 모드.

### 7-c. AlarmDetailViewModel 단위 테스트

`test/.../alarmdetail/AlarmDetailViewModelTest.kt`
- 신규 생성 / 기존 수정 분기 시나리오 검증.
- 각 Event 처리 후 UiState 변화 검증.
- Save 성공 시 `NavigateBack` SideEffect 방출 검증.

### 7-d. AlarmDetailScreen 컴포저블 작성

- 시간 선택: `TimePicker` (M3).
- 요일 반복: 요일 토글 버튼 행.
- 해제 모드 선택: `RadioButton` 그룹 (일반 / 사진 인증).
- 사진 인증 선택 시 "사진 등록" 버튼 노출.

### 7-e. AlarmDetailScreen UI 테스트

`androidTest/.../alarmdetail/AlarmDetailScreenTest.kt`
- 신규/수정 UiState 주입 후 각 입력 필드 렌더링 검증.
- 해제 모드 전환 시 사진 등록 버튼 노출/숨김 검증.

### 7-f. AlarmDetailRoot 컴포저블 작성

---

## Phase 8. 사진 등록 화면 (Photo Setup)

### 8-a. UiState / Event / SideEffect 정의

`presentation/photosetup/PhotoSetupContract.kt`
- `capturedImagePath: String?`, `isCameraReady: Boolean`, `isPhotoTaken: Boolean`.
- Event: `TakePhoto`, `RetakePhoto`, `Confirm`.
- SideEffect: `NavigateBackWithPath(path: String)`.

### 8-b. PhotoSetupViewModel 구현

- 촬영 완료 시 내부 저장소에 이미지 경로 저장.
- `SaveAlarmUseCase`로 `referencePhotoPath` 업데이트.

### 8-c. PhotoSetupViewModel 단위 테스트

`test/.../photosetup/PhotoSetupViewModelTest.kt`
- `TakePhoto` 이벤트 처리 후 `capturedImagePath` 상태 변화 검증.
- `Confirm` 이벤트 처리 후 `NavigateBackWithPath` SideEffect 방출 검증.
- `RetakePhoto` 이벤트 처리 후 `capturedImagePath` 초기화 검증.

### 8-d. CameraPreview 공용 컴포저블 작성

`presentation/components/CameraPreview.kt`
- `CameraX`의 `PreviewView`를 `AndroidView`로 래핑.
- `ImageCapture` UseCase 연동.

### 8-e. PhotoSetupScreen 컴포저블 작성

- `CameraPreview` + 촬영 버튼 + 재촬영 버튼 + 확인 버튼.
- 촬영 후 미리보기 이미지 표시 (`Coil`).

### 8-f. PhotoSetupScreen UI 테스트

`androidTest/.../photosetup/PhotoSetupScreenTest.kt`
- 촬영 전/후 UiState 주입 후 버튼 노출 상태 검증.

### 8-g. PhotoSetupRoot 컴포저블 작성

### 8-h. AndroidManifest.xml — 카메라 권한 추가

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

### 8-i. PhotoSetup 화면 카메라 권한 처리 통합

`PhotoSetupRoot`에서 `rememberPermissionState(CAMERA)`로 권한 상태 관리.

- **화면 진입 시**: 권한 상태 확인.
  - `Granted` → `CameraPreview` 바로 표시.
  - `Denied` → `PermissionRationaleDialog` 표시 후 권한 재요청.
  - `PermanentlyDenied` → `PermissionSettingsDialog` 표시.
- **앱 설정에서 돌아왔을 때 (`LaunchedEffect` + `lifecycleState == RESUMED`)**: 권한 상태 재확인하여 UI 자동 갱신.
- **권한 미허용 상태 유지 시**: 카메라 프리뷰 대신 권한 안내 UI 표시, 사진 촬영 버튼 비활성화.

### 8-j. PhotoSetupViewModel 권한 상태 반영

- `UiState`에 `isCameraPermissionGranted: Boolean` 필드 추가.
- 권한 미허용 상태에서 `TakePhoto` 이벤트 수신 시 무시(guard) 처리.

### 8-k. PhotoSetupViewModel 단위 테스트 업데이트

- 권한 미허용 상태에서 `TakePhoto` 이벤트 무시 검증.

---

## Phase 9. 알람 울림 화면 (Alarm Alert)

### 9-a. UiState / Event / SideEffect 정의

`presentation/alarmalert/AlarmAlertContract.kt`
- `alarm: Alarm?`, `isDismissed: Boolean`, `isPhotoVerified: Boolean`.
- Event: `DismissNormal`, `TakeVerificationPhoto`, `PhotoVerified`.
- SideEffect: `DismissAlarm`, `ShowError`.

### 9-b. PhotoVerificationUseCase 정의 및 dev 구현체

`domain/usecase/PhotoVerificationUseCase.kt`
- `suspend fun verify(capturedPath: String, referencePath: String): Boolean`

`app/src/dev/java/.../usecase/PhotoVerificationUseCaseImpl.kt`
- 항상 `true` 반환 (Mock).

### 9-c. PhotoVerificationUseCase 단위 테스트

`test/.../domain/usecase/PhotoVerificationUseCaseTest.kt`
- dev 구현체: 어떤 경로 입력에도 `true` 반환 검증.

### 9-d. AlarmAlertViewModel 구현

- `GetAlarmByIdUseCase`, `PhotoVerificationUseCase` 주입.
- 해제 모드에 따라 Normal / Photo 분기 처리.

### 9-e. AlarmAlertViewModel 단위 테스트

`test/.../alarmalert/AlarmAlertViewModelTest.kt`
- Normal 모드 `DismissNormal` 이벤트 → `DismissAlarm` SideEffect 검증.
- Photo 모드 `PhotoVerified` 이벤트 → `PhotoVerificationUseCase` 호출 및 결과 반영 검증.

### 9-f. AlarmAlertScreen 컴포저블 작성

- `DismissMode.Normal`: 스와이프 또는 큰 버튼으로 종료.
- `DismissMode.PhotoVerification`: `CameraPreview` + 촬영 버튼 노출.
- Shake 애니메이션: `InfiniteTransition`으로 카드 흔들림 구현.

### 9-g. AlarmAlertScreen UI 테스트

`androidTest/.../alarmalert/AlarmAlertScreenTest.kt`
- Normal / Photo 모드 UiState 주입 후 각 버튼 노출 검증.

### 9-h. AlarmAlertRoot 컴포저블 작성

### 9-i. AlarmAlertActivity 생성

- 알람 울림 화면은 잠금 화면 위에 표시되어야 하므로 별도 `Activity` 사용.
- `FLAG_SHOW_WHEN_LOCKED`, `FLAG_TURN_SCREEN_ON` 플래그 설정.
- `AndroidManifest.xml`에 등록.

---

## Phase 10. 알람 스케줄러 (Background)

### 10-a. AlarmScheduler 인터페이스 정의

`domain/alarm/AlarmScheduler.kt`
```kotlin
interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
}
```

### 10-b. AlarmManagerScheduler 구현

`data/alarm/AlarmManagerScheduler.kt`
- `AlarmManager.setExactAndAllowWhileIdle()` 사용.
- `PendingIntent`로 `AlarmBroadcastReceiver` 연결.

### 10-c. AlarmManagerScheduler 단위 테스트

`test/.../data/alarm/AlarmManagerSchedulerTest.kt`
- `AlarmManager`를 MockK로 mock 처리.
- `schedule()` 호출 시 `setExactAndAllowWhileIdle()` 파라미터(triggerTime 등) 검증.
- `cancel()` 호출 시 `cancel(PendingIntent)` 호출 검증.

### 10-d. AlarmBroadcastReceiver 구현

`data/alarm/AlarmBroadcastReceiver.kt`
- 수신 시 `AlarmAlertActivity` Intent 실행.
- `AndroidManifest.xml`에 등록.

### 10-e. BootReceiver 구현

`data/alarm/BootReceiver.kt`
- 기기 재부팅 후 활성화된 알람 재등록.
- `AndroidManifest.xml`에 `RECEIVE_BOOT_COMPLETED` 권한 및 수신기 등록.

### 10-f. AndroidManifest.xml — 알람 관련 권한 추가

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
```

### 10-f-1. 알람 관련 권한 처리 통합

알람 관련 권한은 두 종류로 나뉘어 처리 방식이 다름.

**① POST_NOTIFICATIONS (Android 13+, API 33+) — 일반 런타임 권한**
- `AlarmDetailRoot`에서 알람 저장 시 권한 상태 확인.
  - `Denied` → `PermissionRationaleDialog` 표시 후 권한 재요청.
  - `PermanentlyDenied` → `PermissionSettingsDialog` 표시하여 앱 설정으로 유도.
  - 미허용 상태로 저장된 알람은 알람음 없이 동작 가능하도록 안내.
- 앱 설정에서 돌아왔을 때(`RESUMED`) 권한 상태 재확인.

**② SCHEDULE_EXACT_ALARM (Android 12+, API 31+) — 특수 권한 (시스템 설정 필요)**
- 일반 `requestPermission()`으로 요청 불가. `AlarmManager.canScheduleExactAlarms()`로 상태 확인.
- 미허용 시: `PermissionSettingsDialog` 표시 후 `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` Intent로 시스템 설정 화면 이동.
- 앱 설정에서 돌아왔을 때(`RESUMED`) `canScheduleExactAlarms()` 재확인.
- 미허용 상태에서는 정확한 알람 대신 `setAndAllowWhileIdle()`로 폴백(fallback) 처리.

### 10-g. SaveAlarmUseCase에 AlarmScheduler 연동

- 알람 저장/업데이트 시 `AlarmScheduler.schedule()` 호출.
- 알람 삭제/비활성화 시 `AlarmScheduler.cancel()` 호출.

### 10-h. SaveAlarmUseCase 단위 테스트 업데이트

`test/.../domain/usecase/SaveAlarmUseCaseTest.kt`
- `AlarmScheduler`를 MockK로 mock 처리.
- 저장 시 `schedule()` 호출, 비활성화 시 `cancel()` 호출 검증.

---

## Phase 11. 설정 화면 (Settings)

### 11-a. UiState / Event 정의

`presentation/settings/SettingsContract.kt`
- `currentThemeMode: ThemeMode`.
- Event: `ChangeThemeMode(mode: ThemeMode)`.

### 11-b. SettingsViewModel 구현

- `ThemeRepository` 주입.
- `getThemeMode()` Flow 수집 → `uiState` 업데이트.
- `onEvent(ChangeThemeMode)` → `setThemeMode()` 호출.

### 11-c. SettingsViewModel 단위 테스트

`test/.../settings/SettingsViewModelTest.kt`
- `ThemeRepository`를 MockK로 mock 처리.
- 초기 테마 모드 UiState 반영 검증.
- `ChangeThemeMode` 이벤트 처리 후 `setThemeMode()` 호출 검증.

### 11-d. SettingsScreen 컴포저블 작성

- 테마 모드 선택 (라이트 / 다크 / 시스템) `RadioButton` 또는 `SegmentedButton` (M3).

### 11-e. SettingsScreen UI 테스트

`androidTest/.../settings/SettingsScreenTest.kt`
- 각 테마 모드 UiState 주입 후 선택 상태 표시 검증.
- 항목 클릭 시 Event 람다 호출 검증.

### 11-f. SettingsRoot 컴포저블 작성

---

## Phase 12. dev flavor 완성 검증

### 12-a. 빌드 확인

```bash
./gradlew assembleDevDebug
```

### 12-b. 전체 단위 테스트 통과

```bash
./gradlew testDevDebugUnitTest
```

### 12-c. 전체 UI 테스트 통과

```bash
./gradlew connectedDevDebugAndroidTest
```

### 12-d. Lint 검사 통과

```bash
./gradlew lintDevDebug
```

---

## Phase 13. Room DB 구축 (prod flavor)

### 13-a. AlarmEntity 정의

`data/local/entity/AlarmEntity.kt`
- `@Entity(tableName = "alarms")`
- `id`, `hour`, `minute`, `repeatDays` (String 직렬화), `label`, `isEnabled`, `dismissMode` (String), `referencePhotoPath` 컬럼.

### 13-b. AlarmDao 정의

`data/local/dao/AlarmDao.kt`
- `getAll(): Flow<List<AlarmEntity>>`
- `getById(id: Long): AlarmEntity?`
- `insert(entity: AlarmEntity): Long`
- `update(entity: AlarmEntity)`
- `deleteById(id: Long)`

### 13-c. AppDatabase 정의

`data/local/AppDatabase.kt`
- `@Database(entities = [AlarmEntity::class], version = 1)`
- `alarmDao()` 추상 메서드.

### 13-d. AlarmDao 단위 테스트

`androidTest/.../data/local/dao/AlarmDaoTest.kt`
- `Room.inMemoryDatabaseBuilder()`로 인메모리 DB 생성.
- insert / getAll / getById / update / deleteById 시나리오 검증.

### 13-e. AlarmEntityMapper 구현

`data/local/mapper/AlarmEntityMapper.kt`
- `AlarmEntity → Alarm` (도메인 모델)
- `Alarm → AlarmEntity`

### 13-f. AlarmEntityMapper 단위 테스트

`test/.../data/local/mapper/AlarmEntityMapperTest.kt`
- Normal / PhotoVerification 각 DismissMode에 대해 양방향 변환 정확성 검증.
- `repeatDays` 직렬화/역직렬화 검증.

### 13-g. RoomAlarmDataSource 구현

`data/datasource/RoomAlarmDataSource.kt`
- `AlarmDao` 주입, CRUD 메서드 위임.

### 13-h. RoomAlarmDataSource 단위 테스트

`test/.../data/datasource/RoomAlarmDataSourceTest.kt`
- `AlarmDao`를 MockK로 mock 처리.
- 각 메서드가 DAO에 올바르게 위임되는지 검증.

### 13-i. AlarmRepositoryImpl 구현

`data/repository/AlarmRepositoryImpl.kt`
- `AlarmRepository` 인터페이스 구현.
- `RoomAlarmDataSource`와 `AlarmEntityMapper` 사용.

### 13-j. AlarmRepositoryImpl 단위 테스트

`test/.../data/repository/AlarmRepositoryImplTest.kt`
- `RoomAlarmDataSource`와 `AlarmEntityMapper`를 MockK로 mock 처리.
- Flow 변환 및 CRUD 위임 동작 검증.

### 13-k. Hilt 모듈 — prod 소스셋

`app/src/prod/java/com/goldennova/upquest/di/RepositoryModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindAlarmRepository(impl: AlarmRepositoryImpl): AlarmRepository
}
```

### 13-l. DatabaseModule 작성 (공통 소스셋)

`di/DatabaseModule.kt`
- `AppDatabase` 싱글톤 프로바이더.
- `AlarmDao` 프로바이더.

---

## Phase 14. 사진 비교 알고리즘 (prod flavor)

### 14-a. libs.versions.toml — ML Kit 버전 추가

```toml
mlkitImageLabeling = "17.0.9"
mlkit-image-labeling = { group = "com.google.mlkit", name = "image-labeling", version.ref = "mlkitImageLabeling" }
```

### 14-b. PhotoVerificationUseCaseImpl 구현 (prod)

`app/src/prod/java/.../usecase/PhotoVerificationUseCaseImpl.kt`
- ML Kit `ImageLabeling` 또는 픽셀 히스토그램 비교 로직 구현.
- 유사도 임계값(threshold) 이상 시 `true` 반환.

### 14-c. PhotoVerificationUseCaseImpl 단위 테스트

`test/.../usecase/PhotoVerificationUseCaseImplTest.kt`
- 동일 이미지 경로 입력 시 `true` 반환 검증.
- 임계값 미만 유사도 입력 시 `false` 반환 검증.

### 14-d. prod Hilt 모듈에 PhotoVerificationUseCase 바인딩 추가

---

## Phase 15. 실제 알람음 / 진동 서비스 (prod flavor)

### 15-a. AlarmSoundPlayer 인터페이스 정의

`domain/alarm/AlarmSoundPlayer.kt`
- `fun play(uri: Uri?)` / `fun stop()`

### 15-b. RingtoneAlarmSoundPlayer 구현

`data/alarm/RingtoneAlarmSoundPlayer.kt`
- `RingtoneManager` 기반 기본 알람음 재생.
- prod Hilt 모듈에 바인딩.

### 15-c. RingtoneAlarmSoundPlayer 단위 테스트

`test/.../data/alarm/RingtoneAlarmSoundPlayerTest.kt`
- `RingtoneManager`를 MockK로 mock 처리.
- `play()` 호출 시 `getRingtone()` 및 `play()` 호출 검증.
- `stop()` 호출 시 재생 중인 Ringtone `stop()` 호출 검증.

### 15-d. AlarmBroadcastReceiver에 AlarmSoundPlayer 연동

- `AlarmSoundPlayer.play()` 호출.
- `AlarmAlertActivity` 종료 시 `AlarmSoundPlayer.stop()` 호출.

---

## Phase 16. prod flavor 완성 검증

### 16-a. 빌드 확인

```bash
./gradlew assembleProdRelease
```

### 16-b. 전체 단위 테스트 통과

```bash
./gradlew testProdReleaseUnitTest
```

### 16-c. 릴리즈 Lint 검사 통과

```bash
./gradlew lintProdRelease
```

## 추가 필요한 기능들

---

## Phase 17. 알림(Notification) 연동

> **최우선 과제**: Android 10+ 백그라운드 Activity 실행 제한으로 인해 알람 발생 시 `AlarmAlertActivity`가 직접 실행되지 않음.
> `setFullScreenIntent()` 포함 고우선순위 Notification으로 대체해야 앱이 닫혀 있어도 알람 화면이 표시됨.

### 17-a. AndroidManifest.xml — USE_FULL_SCREEN_INTENT 권한 추가

```xml
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
```

- Android 14(API 34)+에서는 `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` Intent로 사용자 허용 유도 필요.

### 17-b. NotificationHelper 구현

`data/alarm/NotificationHelper.kt`
- `@Singleton` + `@Inject constructor(@ApplicationContext context: Context)`.
- `createChannel()`: `NotificationChannel` (id: `"alarm_channel"`, importance: `IMPORTANCE_HIGH`, enableVibration: false) 생성.
- `showAlarmNotification(alarmId: Long, label: String)`:
  - `AlarmAlertActivity`로 이동하는 `PendingIntent` 생성 (`FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`).
  - `setFullScreenIntent(pendingIntent, true)` 설정 — 잠금/백그라운드 상태에서도 `AlarmAlertActivity` 실행.
  - `setOngoing(true)` — 스와이프 삭제 방지.
  - `setCategory(Notification.CATEGORY_ALARM)` 설정.
  - `NotificationManagerCompat.notify(alarmId.toInt(), notification)` 발송.
- `cancelAlarmNotification(alarmId: Long)`: `NotificationManagerCompat.cancel(alarmId.toInt())` 호출.
- `canUseFullScreenIntent(): Boolean`: Android 14(API 34)+에서 `NotificationManager.canUseFullScreenIntent()` 확인. 미만 버전은 `true` 반환.

### 17-b-1. AlarmDetailRoot — USE_FULL_SCREEN_INTENT 권한 허용 유도 (Android 14+)

`presentation/alarmdetail/AlarmDetailRoot.kt`
- 알람 저장(`Save` SideEffect 처리) 직전 `NotificationHelper.canUseFullScreenIntent()` 확인.
  - `false`이면 `PermissionSettingsDialog` 표시 후 `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` Intent로 시스템 설정 이동.
- `lifecycleState == RESUMED` 복귀 시 권한 재확인하여 이미 허용됐으면 다이얼로그 자동 닫힘.

### 17-c. UpQuestApplication — NotificationChannel 초기화

`UpQuestApplication.kt`
- `NotificationHelper` Hilt 필드 주입.
- `onCreate()` 에서 `notificationHelper.createChannel()` 호출.

### 17-d. AlarmBroadcastReceiver — startActivity 제거 및 Notification 발송으로 교체

`data/alarm/AlarmBroadcastReceiver.kt`
- `NotificationHelper` 필드 주입.
- `onReceive()` 에서 기존 `context.startActivity()` 직접 호출 **제거**.
- `notificationHelper.showAlarmNotification(alarmId, label)` 호출로 대체.
  - Intent extra에서 `alarmId`와 함께 `label`도 전달받도록 `AlarmManagerScheduler` PendingIntent extra 추가.
- `alarmSoundPlayer.play(null)` 호출은 유지.

### 17-e. AlarmAlertActivity — Notification 취소

`presentation/alarmalert/AlarmAlertActivity.kt`
- `NotificationHelper` 필드 주입.
- `onDestroy()` 에서 `notificationHelper.cancelAlarmNotification(alarmId)` 호출.
- `alarmId`는 `intent.getLongExtra(EXTRA_ALARM_ID, -1L)` 로 읽음.

### 17-f. AlarmManagerScheduler — label extra 추가

`data/alarm/AlarmManagerScheduler.kt`
- `PendingIntent` 생성 시 `alarmId` 외에 `alarm.label` 도 extra로 포함.
  - key: `AlarmAlertActivity.EXTRA_ALARM_ID`, `"extra_alarm_label"`.

### 17-g. NotificationHelper 단위 테스트

`test/.../data/alarm/NotificationHelperTest.kt`
- `NotificationManagerCompat`을 MockK로 mock 처리.
- `showAlarmNotification()` 호출 시 `notify()` 호출 및 alarmId 파라미터 검증.
- `cancelAlarmNotification()` 호출 시 `cancel(alarmId.toInt())` 호출 검증.

---

## Phase 18. 치명적 버그 수정

### 18-a. 반복 알람 재등록 — AlarmAlertViewModel 수정

**문제**: 반복 요일이 설정된 알람이 1회 울린 뒤 다음 회차를 재등록하는 로직이 없어, 반복 알람이 한 번 울리고 영구적으로 꺼짐.

`presentation/alarmalert/AlarmAlertViewModel.kt`
- 생성자에 `AlarmScheduler` 주입 추가.
- `dismissNormal()` 및 `onPhotoVerified()` 성공 경로에서 알람 해제 직전 반복 재등록 처리.
  - `alarm.repeatDays.isNotEmpty()` 이면 `AlarmScheduler.schedule(alarm)` 재호출.
  - 반복 없는 알람(`repeatDays.isEmpty()`)은 비활성화(`AlarmRepository.toggleAlarm(id, false)`) 후 종료.

### 18-b. 반복 알람 재등록 — AlarmAlertViewModel 단위 테스트 업데이트

`test/.../alarmalert/AlarmAlertViewModelTest.kt`
- `AlarmScheduler`를 MockK로 mock 처리.
- 반복 알람 해제 시 `AlarmScheduler.schedule()` 재호출 검증.
- 비반복 알람 해제 시 `AlarmRepository.toggleAlarm(false)` 호출 및 `schedule()` 미호출 검증.

### 18-c. referencePath null 가드 — AlarmAlertViewModel 수정

**문제**: 사진 등록 없이 PhotoVerification 모드로 저장된 알람이 울리면 `referencePath = null`인 채로 `verify()` 가 호출되어 런타임 크래시 위험.

`presentation/alarmalert/AlarmAlertViewModel.kt`
- `onPhotoVerified()` 내부에서 `mode.referencePhotoPath`가 null이면 `verify()` 호출 없이 `ShowError` SideEffect를 방출하고 조기 반환.
- 오류 메시지 strings.xml에 `alarm_alert_no_reference_photo` 키로 추가.

`PhotoVerificationUseCase` 인터페이스 시그니처 재검토:
- `suspend fun verify(capturedPath: String, referencePath: String): Boolean` — `referencePath`를 `String`(non-null)으로 유지.
- 호출부에서 null 가드를 완전히 처리하여 UseCase 레이어에 null이 도달하지 않도록 보장.

### 18-d. referencePath null 가드 — 단위 테스트 추가

`test/.../alarmalert/AlarmAlertViewModelTest.kt`
- `referencePhotoPath = null`인 PhotoVerification 알람으로 `PhotoVerified` 이벤트 전달 시 `verify()` 미호출 및 `ShowError` SideEffect 방출 검증.

### 18-e. USE_EXACT_ALARM 권한 정리

**문제**: `USE_EXACT_ALARM`은 Android 13+에서 시스템이 허가한 알람/시계 앱에만 부여됨. 일반 앱이 선언하면 Google Play 심사에서 배포 차단될 수 있음.

`AndroidManifest.xml`
- `USE_EXACT_ALARM` 권한 제거.
- `SCHEDULE_EXACT_ALARM` 단독 유지 (`maxSdkVersion` 미지정, API 31+ 대응).

`data/alarm/AlarmManagerScheduler.kt`
- `schedule()` 내부에서 `AlarmManager.canScheduleExactAlarms()` (API 31+) 체크.
  - 허용: `setExactAndAllowWhileIdle()` 사용 (기존 동작 유지).
  - 미허용: `setAndAllowWhileIdle()` 로 폴백 — 이미 `AlarmManagerSchedulerTest`에 스텁 존재하므로 로직만 추가.

`presentation/alarmdetail/AlarmDetailRoot.kt`
- 알람 저장 시 `canScheduleExactAlarms()` 확인 후 미허용이면 `PermissionSettingsDialog` 표시.
- `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` Intent로 시스템 설정 화면 이동.
- `lifecycleState == RESUMED` 복귀 시 권한 재확인 및 UiState 갱신.

### 18-f. AlarmManagerScheduler 단위 테스트 업데이트

`test/.../data/alarm/AlarmManagerSchedulerTest.kt`
- `canScheduleExactAlarms() = true` → `setExactAndAllowWhileIdle()` 호출 검증 (기존 테스트 유지).
- `canScheduleExactAlarms() = false` → `setAndAllowWhileIdle()` 폴백 호출 검증 (신규).

---

## Phase 19. 진동 기능

### 19-a. VibrationPlayer 인터페이스 정의

`domain/alarm/VibrationPlayer.kt`
```kotlin
interface VibrationPlayer {
    fun vibrate()
    fun cancel()
}
```

### 19-b. SystemVibrationPlayer 구현 (공통 소스셋)

`data/alarm/SystemVibrationPlayer.kt`
- `Vibrator` (API < 31) / `VibratorManager` (API 31+) 분기 처리.
- 패턴: 0ms 대기 → 500ms 진동 → 500ms 정지 반복 (`VibrationEffect.createWaveform()`).
- `@Singleton` + `@Inject constructor(@ApplicationContext context: Context)`.

### 19-c. Hilt 모듈 — VibrationPlayer 바인딩

`di/SchedulerModule.kt` (공통 소스셋, 기존 파일에 추가)
- `VibrationPlayer → SystemVibrationPlayer` `@Binds @Singleton` 바인딩 추가.

### 19-d. AlarmBroadcastReceiver에 VibrationPlayer 연동

`data/alarm/AlarmBroadcastReceiver.kt`
- `VibrationPlayer` 필드 주입 추가.
- `onReceive()` 에서 `alarmSoundPlayer.play()` 와 함께 `vibrationPlayer.vibrate()` 호출.

### 19-e. AlarmAlertActivity에 VibrationPlayer 연동

`presentation/alarmalert/AlarmAlertActivity.kt`
- `VibrationPlayer` 필드 주입 추가.
- `onDestroy()` 에서 `alarmSoundPlayer.stop()` 과 함께 `vibrationPlayer.cancel()` 호출.

### 19-f. SystemVibrationPlayer 단위 테스트

`test/.../data/alarm/SystemVibrationPlayerTest.kt`
- `Vibrator` / `VibratorManager`를 MockK로 mock 처리.
- `vibrate()` 호출 시 `VibrationEffect.createWaveform()` 파라미터 검증.
- `cancel()` 호출 시 `vibrator.cancel()` 호출 검증.

---

## Phase 20. 알람음 선택 기능

### 20-a. Alarm 도메인 모델에 ringtoneUri 필드 추가

`domain/model/Alarm.kt`
- `val ringtoneUri: String? = null` 필드 추가. (`null` = 시스템 기본 알람음)

### 20-b. AlarmEntity에 ringtoneUri 컬럼 추가 및 DB 마이그레이션

`data/local/entity/AlarmEntity.kt`
- `val ringtoneUri: String? = null` 컬럼 추가.

`data/local/AppDatabase.kt`
- `version = 2`로 변경.
- `MIGRATION_1_2` 정의: `ALTER TABLE alarms ADD COLUMN ringtoneUri TEXT`.
- `addMigrations(MIGRATION_1_2)` 등록.

### 20-c. AlarmEntityMapper 업데이트

`data/local/mapper/AlarmEntityMapper.kt`
- `ringtoneUri` 필드 양방향 매핑 추가.

### 20-d. AlarmEntityMapper 단위 테스트 업데이트

`test/.../data/local/mapper/AlarmEntityMapperTest.kt`
- `ringtoneUri` null / non-null 각 경우 양방향 변환 검증 추가.

### 20-e. UiState / Event 업데이트 — AlarmDetailContract

`presentation/alarmdetail/AlarmDetailUiState.kt`
- `ringtoneUri: String?` 필드 추가.

`presentation/alarmdetail/AlarmDetailEvent.kt`
- `data class ChangeRingtone(val uri: String?) : AlarmDetailEvent` 추가.

### 20-f. AlarmDetailViewModel 업데이트

`presentation/alarmdetail/AlarmDetailViewModel.kt`
- `ChangeRingtone` 이벤트 처리 — UiState `ringtoneUri` 업데이트.
- 기존 알람 로드 시 `ringtoneUri` 복원.
- `Save` 이벤트 처리 시 `ringtoneUri` 포함하여 `SaveAlarmUseCase` 호출.

### 20-g. AlarmDetailScreen — 알람음 선택 UI 추가

`presentation/alarmdetail/AlarmDetailScreen.kt`
- 알람음 선택 행(Row) 추가: 현재 선택된 알람음 이름 표시 + 변경 버튼.
- 버튼 클릭 시 Android 기본 링톤 선택 Intent(`RingtoneManager.ACTION_RINGTONE_PICKER`) 실행을 위한 `onPickRingtone: () -> Unit` 람다 파라미터 추가.
- `null`인 경우 "기본 알람음" 문자열 표시 (strings.xml에 `ringtone_default` 키 추가).

### 20-h. AlarmDetailRoot — 링톤 선택 결과 처리

`presentation/alarmdetail/AlarmDetailRoot.kt`
- `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())` 등록.
- 결과에서 `RingtoneManager.EXTRA_RINGTONE_PICKED_URI` 추출 후 `ChangeRingtone` 이벤트 발송.

### 20-i. RingtoneAlarmSoundPlayer — 선택된 URI 반영

`data/alarm/RingtoneAlarmSoundPlayer.kt`
- 현재 `play(uri = null)` 고정 호출 → 알람의 `ringtoneUri`를 `Uri.parse()`로 변환 후 전달.

`data/alarm/AlarmBroadcastReceiver.kt`
- Intent extra로 `ringtoneUri`를 함께 전달받아 `alarmSoundPlayer.play(uri)` 에 적용.
- `AlarmManagerScheduler`에서 `PendingIntent` 생성 시 `ringtoneUri` extra 포함.

### 20-j. AlarmDetailViewModel 단위 테스트 업데이트

`test/.../alarmdetail/AlarmDetailViewModelTest.kt`
- `ChangeRingtone` 이벤트 처리 후 UiState `ringtoneUri` 변화 검증.
- Save 시 `ringtoneUri`가 `SaveAlarmUseCase` 파라미터에 포함되는지 검증.

---

## Phase 21. 안정성 보강

### 21-a. 사진 파일 관리 — 알람 삭제 시 참조 사진 삭제

`domain/usecase/DeleteAlarmUseCase.kt`
- 삭제 전 `GetAlarmByIdUseCase`로 알람 조회.
- `dismissMode`가 `PhotoVerification`이고 `referencePhotoPath`가 non-null이면 `File(path).delete()` 호출.

`test/.../domain/usecase/DeleteAlarmUseCaseTest.kt`
- PhotoVerification 알람 삭제 시 참조 파일 삭제 호출 검증.
- Normal 알람 삭제 시 파일 삭제 미호출 검증.

### 21-b. 알람 울림 화면 — referencePath 미등록 안내 UI

`presentation/alarmalert/AlarmAlertScreen.kt`
- PhotoVerification 모드이지만 `referencePhotoPath == null`인 경우:
  - 카메라 프리뷰 대신 경고 메시지 표시 ("기준 사진이 등록되지 않았습니다. 일반 해제 버튼을 사용하세요.").
  - 일반 해제 버튼 노출.
- strings.xml에 `alarm_alert_no_reference_photo_message` 키 추가.

`presentation/alarmalert/AlarmAlertUiState.kt`
- `val hasReferencePhoto: Boolean` 계산 프로퍼티 또는 파생 필드 추가.

### 21-c. 도즈 모드(Doze Mode) 대응 — 알람 신뢰성 향상

`data/alarm/AlarmManagerScheduler.kt`
- 반복 알람의 경우 알람 해제 후 다음 회차 재등록 시 `setExactAndAllowWhileIdle()` 사용 확인 (기존 로직 점검).
- 배터리 최적화 예외 설정 안내: `AlarmDetailRoot`에서 알람 저장 시 `PowerManager.isIgnoringBatteryOptimizations()` 확인 후 미설정이면 `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` Intent 안내 다이얼로그 표시.

### 21-d. FakeAlarmRepository — 반복 알람 재등록 시나리오 대응

`data/repository/FakeAlarmRepository.kt` (dev 소스셋)
- `toggleAlarm()` 메서드가 반복 알람에 대해서도 정상 동작하는지 확인 및 보완.
- dev 빌드에서 AlarmAlertViewModel 재등록 로직 검증 가능하도록 FakeAlarmScheduler 제공.

`app/src/dev/java/.../di/` — FakeAlarmScheduler 바인딩 확인.

### 21-e. 전체 회귀 테스트

```bash
# dev flavor 전체 단위 테스트
./gradlew testDevDebugUnitTest

# prod flavor 전체 단위 테스트
./gradlew testProdDebugUnitTest

# prod release 빌드 최종 확인
./gradlew assembleProdRelease

# prod release lint 최종 확인
./gradlew lintProdRelease
```

---

## Phase 22. 사진 비교 알고리즘 교체 — pHash(지각적 해시) 방식 (prod flavor)

> **배경**: 기존 ML Kit ImageLabeler 기반 Jaccard 유사도 방식은 동일 피사체를 찍어도 신뢰도 임계값(0.7f)을 넘기는 레이블 수가 적어 교집합이 0이 되는 경우가 빈번하여 인증 실패가 반복됨.
> pHash는 이미지의 저주파 구조(DCT)를 64비트 해시로 압축한 뒤 해밍 거리로 비교하므로 조명·각도 변화에 강인하고 외부 라이브러리 의존성이 없음.

### 22-a. ML Kit 의존성 제거

`gradle/libs.versions.toml`
- `mlkitImageLabeling` 버전 항목 제거.
- `mlkit-image-labeling` 라이브러리 항목 제거.

`app/build.gradle.kts`
- `implementation(libs.mlkit.image.labeling)` 의존성 제거.

### 22-b. pHash 유틸리티 구현

`app/src/prod/java/com/goldennova/upquest/domain/usecase/PHashCalculator.kt`

pHash 계산 절차:
1. 입력 Bitmap을 **32×32 그레이스케일**로 리사이즈.
2. 32×32 픽셀 행렬에 **2D DCT(이산 코사인 변환)** 적용.
3. DCT 결과의 **상위 좌측 8×8 저주파 계수** 64개만 추출.
4. 64개 계수의 **평균값** 계산 (DC 성분인 [0][0] 제외).
5. 각 계수가 평균 이상이면 `1`, 미만이면 `0`으로 **64비트 Long 해시** 생성.

```kotlin
// 시그니처 예시 (구현 세부사항은 별도 결정)
object PHashCalculator {
    fun calculate(bitmap: Bitmap): Long
    fun hammingDistance(a: Long, b: Long): Int = (a xor b).countOneBits()
}
```

### 22-c. PHashCalculator 단위 테스트 작성

`app/src/testProd/java/com/goldennova/upquest/domain/usecase/PHashCalculatorTest.kt`

검증 항목:
- 동일한 Bitmap 입력 시 해밍 거리 = 0.
- 좌우 반전된 이진 패턴 Bitmap 입력 시 해밍 거리 > 임계값.
- 수평 그라디언트와 반전 그라디언트 Bitmap 입력 시 해밍 거리 > 임계값.
- `hammingDistance` 헬퍼: 0 XOR 0 = 0, Long.MAX_VALUE XOR 0 = 63, 전체 비트 상이 시 64 검증.
- 참고: pHash는 "픽셀 개수가 적을수록 해밍 거리가 작다"를 보장하지 않음. 좌상단 픽셀은 DCT 기여도가 커서 1~2개 변경만으로도 AC 평균이 크게 흔들릴 수 있음.

### 22-d. PhotoVerificationUseCaseImpl 교체 (prod)

`app/src/prod/java/com/goldennova/upquest/domain/usecase/PhotoVerificationUseCaseImpl.kt`

- 기존 `ImageLabeler` 주입 제거.
- `PHashCalculator.calculate()` 로 두 이미지 각각 해시 계산.
- `PHashCalculator.hammingDistance()` 로 해밍 거리 산출.
- 해밍 거리 ≤ `HAMMING_THRESHOLD`(초기값 `10`) 이면 `true` 반환.
- Bitmap 디코딩 실패(파일 없음) 시 `false` 반환.

```kotlin
companion object {
    // 해밍 거리 허용 상한 (0 = 완전 동일, 64 = 완전 상이)
    // 10 이하 = 동일 피사체로 판단 (조명·각도 차이 허용)
    const val HAMMING_THRESHOLD = 10
}
```

### 22-e. PhotoVerificationUseCaseImpl 단위 테스트 교체

`app/src/testProd/java/com/goldennova/upquest/domain/usecase/PhotoVerificationUseCaseImplTest.kt`

- 기존 ML Kit `ImageLabeler` MockK stub 전부 제거.
- `PHashCalculator`를 MockK로 교체하여 해밍 거리 반환값 제어.

검증 항목:
- `hammingDistance ≤ HAMMING_THRESHOLD` → `verify()` = `true`.
- `hammingDistance > HAMMING_THRESHOLD` → `verify()` = `false`.
- `BitmapFactory.decodeFile()` = `null` (파일 없음) → `verify()` = `false`.
- `hammingDistance = 0` (완전 동일) → `verify()` = `true`.
- `hammingDistance = HAMMING_THRESHOLD` (경계값) → `verify()` = `true`.
- `hammingDistance = HAMMING_THRESHOLD + 1` (경계값 초과) → `verify()` = `false`.

### 22-f. Hilt 모듈 정리 (prod)

`app/src/prod/java/com/goldennova/upquest/di/UseCaseModule.kt`

- `provideImageLabeler()` `@Provides` 메서드 제거.
- `ImageLabeler`, `ImageLabeling`, `ImageLabelerOptions` import 제거.
- `CONFIDENCE_THRESHOLD` 상수 제거.
- `bindPhotoVerificationUseCase()` `@Binds` 바인딩은 유지 (인터페이스는 변경 없음).

### 22-g. 빌드 및 테스트 검증

```bash
# prod flavor 단위 테스트 (pHash 관련 신규·교체 테스트 포함)
./gradlew testProdDebugUnitTest

# prod release 빌드 — ML Kit 제거 후 컴파일 오류 없음 확인
./gradlew assembleProdRelease

# lint — 미사용 import·상수 경고 없음 확인
./gradlew lintProdRelease
```
