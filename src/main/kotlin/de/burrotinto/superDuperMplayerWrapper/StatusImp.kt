package de.burrotinto.superDuperMplayerWrapper

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by Florian Klinger on 31.12.17, 06:35.
 */
class StatusImp(private val mplayer: Mplayer) : Status {
    override fun getLength(): Double = mplayer.length ?: 0.0

    private val LOCK = ReentrantLock()

    private val timeCondition = LOCK.newCondition()
    private val timePosConditions = HashMap<Double, Condition>()
    var timePos: Double = -1.0
        set(value) {
            LOCK.lock()

            if (value != field) {
                timeCondition.signalAll()
            }
            timePosConditions[value]?.signalAll()

            field = value

            LOCK.unlock()
        }

    override
    fun waitOnTime(time: Double) {
        val t = (time * 10).toInt().toDouble() / 10
        timePosConditions.putIfAbsent(t, LOCK.newCondition())
        LOCK.lock()
        timePosConditions[t]!!.await()
        LOCK.unlock()
    }

    override fun waitOnNewTime() {
        LOCK.lock()
        timeCondition.await()
        LOCK.unlock()
    }

    override fun getTimepos(): Double = timePos

    override fun getassoziatedPlayer(): Mplayer = mplayer
}
