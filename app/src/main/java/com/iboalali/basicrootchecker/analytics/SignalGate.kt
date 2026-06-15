package com.iboalali.basicrootchecker.analytics

/**
 * Buffers telemetry actions until the opt-out preference (read asynchronously at startup) is
 * known, then either flushes or discards them.
 *
 * - **PENDING** (initial): [submit]ted actions are queued, bounded by [maxQueue].
 * - **ENABLED** ([resolve]`(true)`): the queue is flushed in FIFO order and later actions run
 *   immediately.
 * - **DISABLED** ([resolve]`(false)`): the queue is discarded and later actions are dropped.
 *
 * Actions run outside the lock, so the action passed to [submit] (or a flushed one) may itself
 * touch telemetry without re-entering the gate. Safe to call from any thread.
 */
internal class SignalGate(private val maxQueue: Int = 100) {

    private enum class State { PENDING, ENABLED, DISABLED }

    private val lock = Any()
    private var state = State.PENDING
    private val queue = ArrayDeque<() -> Unit>()

    /** Resolve the opt-out preference: flush buffered actions if [enabled], otherwise drop them. */
    fun resolve(enabled: Boolean) {
        val flush: List<() -> Unit>
        synchronized(lock) {
            if (enabled) {
                state = State.ENABLED
                flush = queue.toList()
                queue.clear()
            } else {
                state = State.DISABLED
                queue.clear()
                flush = emptyList()
            }
        }
        flush.forEach { it() }
    }

    /** Run [action] now if enabled, buffer it while pending (up to [maxQueue]), or drop it if disabled. */
    fun submit(action: () -> Unit) {
        val runNow: Boolean
        synchronized(lock) {
            when (state) {
                State.PENDING -> {
                    if (queue.size < maxQueue) queue.add(action)
                    runNow = false
                }
                State.ENABLED -> runNow = true
                State.DISABLED -> runNow = false
            }
        }
        if (runNow) action()
    }
}
