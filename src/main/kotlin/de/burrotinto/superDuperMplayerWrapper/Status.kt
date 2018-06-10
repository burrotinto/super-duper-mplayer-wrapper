package de.burrotinto.superDuperMplayerWrapper

/**
 * Created by Florian Klinger on 31.12.17, 06:21.
 */
interface Status {
    fun getTimepos(): Double
    fun getLength(): Double
    fun waitOnTime(time: Double)
    fun waitOnNewTime()
    fun getassoziatedPlayer(): Mplayer
}