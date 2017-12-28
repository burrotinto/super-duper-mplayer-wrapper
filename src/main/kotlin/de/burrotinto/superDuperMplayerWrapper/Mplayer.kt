package de.burrotinto.superDuperMplayerWrapper

import de.burrotinto.superDuperMplayerWrapper.wrapper.BasicFunctionMplayer

/**
 * Created by Florian Klinger on 23.12.17, 21:15.
 */
open class Mplayer(vararg options: String) : BasicFunctionMplayer(*options) {


    /**y
    seek <value> [type]

    Seek to some place in the movie.
    0 is a relative seek of +/- <value> seconds (default).
    1 is a seek to <value> % in the movie.
    2 is a seek to an absolute position of <value> seconds.
     */
    fun seek(type: Int, value: String) = execute("seek $value $type")

    //    Multiply the current speed by <value>.
    fun speed_mult(value: Double) = execute("speed_mult $value")

    /**
     * Adjust/set how many times the movie should be looped. -1 means no loop,
    and 0 forever.
     */
    var loop: Int
        get() = getProperty("loop").toInt()
        set(value) = setProperty("loop", value.toString())

    var speed: Double?
        get() = getProperty("speed").toDoubleOrNull()
        set(value) = setProperty("speed", value.toString())

    //    Add <value> to the current playback speed.
    fun speed_incr(value: Double) {
        execute("speed_incr $value")
    }

    val bitrate: Int
        get() = getProperty("video_bitrate").toInt()


    var fullscreen: Boolean
        get() = getProperty("fullscreen") == "yes"
        set(value) = setProperty("fullscreen", if (value) "1" else "0")

    var timePos: Double?
        get() = getProperty("time_pos").toDoubleOrNull()
        set(value) = setProperty("time_pos", value.toString())

    var border: Boolean
        get() = getProperty("border") == "1"
        set(value) = setProperty("border", if (value) "1" else "0")

    fun osd(level: Int = -1) = execute("osd $level")
}