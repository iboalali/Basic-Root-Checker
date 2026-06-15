package com.iboalali.basicrootchecker.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalGateTest {

    @Test
    fun `actions submitted while pending are buffered, not run`() {
        val gate = SignalGate()
        var runs = 0

        gate.submit { runs++ }
        gate.submit { runs++ }

        assertEquals(0, runs)
    }

    @Test
    fun `resolving enabled flushes buffered actions in FIFO order`() {
        val gate = SignalGate()
        val order = mutableListOf<Int>()

        gate.submit { order.add(1) }
        gate.submit { order.add(2) }
        gate.submit { order.add(3) }
        gate.resolve(enabled = true)

        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun `resolving disabled discards buffered actions`() {
        val gate = SignalGate()
        var runs = 0

        gate.submit { runs++ }
        gate.submit { runs++ }
        gate.resolve(enabled = false)

        assertEquals(0, runs)
    }

    @Test
    fun `once enabled, later actions run immediately`() {
        val gate = SignalGate()
        var runs = 0
        gate.resolve(enabled = true)

        gate.submit { runs++ }
        gate.submit { runs++ }

        assertEquals(2, runs)
    }

    @Test
    fun `once disabled, later actions are dropped`() {
        val gate = SignalGate()
        var runs = 0
        gate.resolve(enabled = false)

        gate.submit { runs++ }

        assertEquals(0, runs)
    }

    @Test
    fun `the buffer is bounded by maxQueue, dropping the overflow`() {
        val gate = SignalGate(maxQueue = 2)
        var runs = 0

        gate.submit { runs++ }
        gate.submit { runs++ }
        gate.submit { runs++ } // dropped — over the cap
        gate.resolve(enabled = true)

        assertEquals(2, runs)
    }

    @Test
    fun `flushing an enabled gate happens only once`() {
        val gate = SignalGate()
        var runs = 0

        gate.submit { runs++ }
        gate.resolve(enabled = true)
        gate.resolve(enabled = true) // second resolve has nothing left to flush

        assertEquals(1, runs)
    }
}
