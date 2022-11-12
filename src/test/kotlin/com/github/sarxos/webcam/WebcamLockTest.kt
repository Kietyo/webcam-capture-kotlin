package com.github.sarxos.webcam

import com.github.sarxos.webcam.Webcam
import org.assertj.core.api.Assertions
import org.easymock.EasyMock
import org.easymock.EasyMockRunner
import org.easymock.EasyMockSupport
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test case is to cover [WebcamLock] class.
 *
 * @author Bartosz Firyn (sarxos)
 */
@RunWith(EasyMockRunner::class)
class WebcamLockTest : EasyMockSupport() {
    lateinit var webcam: Webcam
    @Before
    fun before() {
        webcam = createNiceMock(Webcam::class.java)
        EasyMock
            .expect(webcam.name)
            .andReturn("test-webcam")
            .anyTimes()
        replayAll()
    }

    @Test
    fun test_lock() {
        val lock = WebcamLock(webcam)
        lock.lock()
        Assertions
            .assertThat(lock.isLocked())
            .isTrue
        lock.unlock()
        Assertions
            .assertThat(lock.isLocked())
            .isFalse
    }

    @Test
    fun test_lock2() {
        val first = WebcamLock(webcam)
        val second = WebcamLock(webcam)
        first.lock()
        Assertions
            .assertThat(second.isLocked())
            .isTrue
    }
}