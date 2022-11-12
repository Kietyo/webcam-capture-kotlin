package com.github.sarxos.webcam

import com.github.sarxos.webcam.Webcam
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith

/**
 * This test case is to cover [WebcamLock] class.
 *
 * @author Bartosz Firyn (sarxos)
 */
class WebcamLockTest {
    @MockK
    lateinit var webcam: Webcam

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this)
        every { webcam.name } returns "test-webcam"
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