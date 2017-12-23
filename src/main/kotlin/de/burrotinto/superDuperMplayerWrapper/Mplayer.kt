package de.burrotinto.superDuperMplayerWrapper.wrapper

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by Florian Klinger on 16.12.17, 15:51.
 */
open class Mplayer(vararg options: String) {
    companion object {
        val WAITING_TIME = 1000L
    }

    private val LOCK = ReentrantLock()


    private val mplayer: Process = ProcessBuilder(listOf("mplayer", "-slave", "-idle", "-quiet") +
            options).start()
    private val reader: BufferedReader
    private val writer: BufferedWriter

    private val conditions = mutableListOf<Data>()


    init {
        reader = mplayer.inputStream.bufferedReader(Charset.defaultCharset())
        writer = mplayer.outputStream.bufferedWriter(Charset.defaultCharset())

        Thread(Runnable {
            reader.forEachLine { read(it) }
        }).start()
    }

    private fun read(string: String) {
        LOCK.lock()

        val list = mutableListOf<Data>()
        conditions.filter { string.startsWith(it.searchString) }.forEach {
            it.returnString = string.split("=")[1]
            it.condition.signalAll()
            list.add(it)
        }
        if (list.isNotEmpty()) {
            conditions.removeAll(list)
        } else {
            notUnderstand(string)
        }
        LOCK.unlock()
    }

    private fun execute(cmd: String, prefix: PrefixedPausedMode, suffix: String = "") {
        execute(cmd, prefix.string)
    }


    fun execute(cmd: String, prefix: String = "", suffix: String = "") {
        writer.write("$prefix $cmd $suffix")
        writer.newLine()
        writer.flush()
    }

    fun executeWithReturnString(cmd: String, searchString: String): String {
        val data = Data(LOCK.newCondition(), searchString)
        LOCK.lock()
        conditions.add(data)
        execute(cmd)
        data.condition.await(WAITING_TIME, TimeUnit.MILLISECONDS)
        LOCK.unlock()
        return data.returnString.orEmpty()
    }

    protected fun notUnderstand(string: String) {
        println(string)
    }

    fun loadfile(file: String, append: Int = 0) = execute("loadfile $file $append")

    fun stop() = execute("stop")

    fun pause() = execute("pause")

    fun quit(value: Int = 0) = execute("quit", suffix = value.toString())

    fun isPaused(): Boolean = executeWithReturnString("get_property pause", "ANS_pause").trim() == "yes"

    /**y
    seek <value> [type]

    Seek to some place in the movie.
    0 is a relative seek of +/- <value> seconds (default).
    1 is a seek to <value> % in the movie.
    2 is a seek to an absolute position of <value> seconds.
     */
    fun seek(type: Int, value: String) = execute("seek $value $type")

    fun osd(level: Int = -1) = execute("osd $level")

    //    Add <value> to the current playback speed.
    fun speed_incr(value: Double) {
        execute("speed_incr $value")
    }


    //    Multiply the current speed by <value>.
    fun speed_mult(value: Double) = execute("speed_mult $value")

    /**
     * Adjust/set how many times the movie should be looped. -1 means no loop,
    and 0 forever.
     */
    var loop: Int
        get() = getProperty("loop").toInt()
        set(value) = setProperty("loop", value.toString())

    val length: Double?
        get() = getProperty("length").toDoubleOrNull()

    val bitrate: Int
        get() = getProperty("video_bitrate").toInt()

    var speed: Double?
        get() = getProperty("speed").toDoubleOrNull()
        set(value) = setProperty("speed", value.toString())

    var fullscreen: Boolean
        get() = getProperty("fullscreen") == "yes"
        set(value) = setProperty("fullscreen", if (value) "1" else "0")

    var timePos: Double?
        get() = getProperty("time_pos").toDoubleOrNull()
        set(value) = setProperty("time_pos", value.toString())

    var border: Boolean
        get() = getProperty("border") == "1"
        set(value) = setProperty("border", if (value) "1" else "0")

    var volume: Double
        get() = getProperty("volume").toDouble()
        set(value) = setProperty("volume", "$value")

    val file: File?
        get() {
            return try {
                File(getProperty("path"))
            } catch (e: Exception) {
                null
            }
        }

    fun getProperty(property: String) = executeWithReturnString("get_property $property", "ANS_$property")

    fun setProperty(property: String, value: String) = execute("set_property $property $value")

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("FILE: ").append(file?.toString()).append("\n")
        sb.append("LENGTH: $length\n")
        sb.append("POS: ").append(timePos).append("\n")
        sb.append("SPEED: ").append(speed).append("\n")
        sb.append("BITRATE: $bitrate\n")
        sb.append("LOOP: $loop")
        return sb.toString()
    }

    class Data(val condition: Condition, val searchString: String, var returnString: String? = null)
}
