package com.goldennova.upquest.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit5 Extension — Dispatchers.Main을 테스트 디스패처로 교체한다.
 *
 * Dispatchers.Main을 TestDispatcher로 교체하면, 이후 runTest {} 가 인자 없이 호출되더라도
 * Main의 TestCoroutineScheduler를 자동으로 공유한다.
 * (kotlinx-coroutines-test 문서 참고)
 *
 * 사용법:
 * ```
 * @RegisterExtension
 * val mainDispatcherExtension = MainDispatcherExtension()
 *
 * @Test
 * fun test() = runTest(mainDispatcherExtension.testDispatcher) { ... }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
