package com.goldennova.upquest.data.alarm

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [RingtoneAlarmSoundPlayer] 단위 테스트.
 *
 * [RingtoneManager]의 정적 메서드를 MockK로 대체하여
 * play/stop 위임 동작을 검증한다.
 */
class RingtoneAlarmSoundPlayerTest {

    private lateinit var context: Context
    private lateinit var mockRingtone: Ringtone
    private lateinit var defaultUri: Uri
    private lateinit var player: RingtoneAlarmSoundPlayer

    @BeforeEach
    fun setUp() {
        context = mockk()
        mockRingtone = mockk()
        defaultUri = mockk()

        mockkStatic(RingtoneManager::class)
        every { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) } returns defaultUri
        every { RingtoneManager.getRingtone(any(), any()) } returns mockRingtone
        justRun { mockRingtone.play() }
        justRun { mockRingtone.stop() }

        player = RingtoneAlarmSoundPlayer(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // region play — URI 지정

    @Test
    fun `play에 URI를 전달하면 해당 URI로 getRingtone이 호출된다`() {
        val uri: Uri = mockk()

        player.play(uri)

        verify(exactly = 1) { RingtoneManager.getRingtone(context, uri) }
    }

    @Test
    fun `play에 URI를 전달하면 ringtone play가 호출된다`() {
        val uri: Uri = mockk()

        player.play(uri)

        verify(exactly = 1) { mockRingtone.play() }
    }

    // endregion

    // region play — URI null (기본 알람음)

    @Test
    fun `play에 null을 전달하면 getDefaultUri로 기본 알람음 URI를 조회한다`() {
        player.play(null)

        verify(exactly = 1) { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) }
    }

    @Test
    fun `play에 null을 전달하면 기본 알람음 URI로 getRingtone이 호출된다`() {
        player.play(null)

        verify(exactly = 1) { RingtoneManager.getRingtone(context, defaultUri) }
    }

    @Test
    fun `play에 null을 전달하면 ringtone play가 호출된다`() {
        player.play(null)

        verify(exactly = 1) { mockRingtone.play() }
    }

    // endregion

    // region play — 이미 재생 중인 경우

    @Test
    fun `이미 재생 중일 때 play를 호출하면 기존 ringtone stop이 먼저 호출된다`() {
        val uri: Uri = mockk()
        player.play(uri) // 첫 번째 재생 시작

        player.play(uri) // 두 번째 재생 → 기존 stop 후 새로 시작

        // 첫 번째 ringtone stop은 두 번째 play 시점에 호출됨
        verify(atLeast = 1) { mockRingtone.stop() }
    }

    // endregion

    // region stop

    @Test
    fun `재생 중일 때 stop을 호출하면 ringtone stop이 호출된다`() {
        player.play(null)

        player.stop()

        verify(exactly = 1) { mockRingtone.stop() }
    }

    @Test
    fun `재생 중이 아닐 때 stop을 호출해도 예외가 발생하지 않는다`() {
        // play 없이 바로 stop 호출
        player.stop()

        // ringtone이 없으므로 stop이 호출되지 않음
        verify(exactly = 0) { mockRingtone.stop() }
    }

    // endregion
}
