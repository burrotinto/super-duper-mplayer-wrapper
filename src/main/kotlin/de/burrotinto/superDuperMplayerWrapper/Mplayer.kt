package de.burrotinto.superDuperMplayerWrapper

import de.burrotinto.superDuperMplayerWrapper.wrapper.PrefixedPausedMode
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by Florian Klinger on 23.12.17, 21:04.
 */
class Mplayer(vararg options: String,
              private val WAITING_TIME_MS: Long = 1000000L,
              private val ttl: Int = 100) {


    private val lock = ReentrantLock()

    private val mplayer: Process = ProcessBuilder(listOf("mplayer", "-slave", "-idle") + options).start()

    private val reader: BufferedReader
    private val writer: BufferedWriter

    private val conditions = LinkedList<Data>()

    private val status = StatusImp(this)


    init {
        reader = mplayer.inputStream.bufferedReader(Charset.defaultCharset())
        writer = mplayer.outputStream.bufferedWriter(Charset.defaultCharset())

        Thread(Runnable {
            reader.forEachLine { read(it) }
        }).start()
    }


    private fun read(string: String) {
        lock.lock()
        if (string.startsWith("A:") || string == "ANS_ERROR=PROPERTY_UNAVAILABLE") {
            status(string)
        } else {
            val d = conditions.firstOrNull { string.startsWith(it.searchString) }

            if (d != null) {
                d.returnString = string.split("=")[1]
                d.condition.signalAll()
                conditions.remove(d)
            } else {
                notUnderstand(string)
            }
            conditions.removeIf({ data -> data.ttl-- == 0 })
        }

        lock.unlock()
    }

    private fun status(string: String) {
        status.timePos = string.split("V:")[0].split(":")[1].toDouble()
    }

    fun getStatus(): Status = status

    private fun notUnderstand(string: String) {
        println("Not Understand: $string")
    }

    fun execute(cmd: String, prefix: PrefixedPausedMode, suffix: String = "") {
        execute(cmd, prefix.string, suffix)
    }


    fun execute(cmd: String, prefix: String = "", suffix: String = "") {
        writer.write("$prefix $cmd $suffix")
        writer.newLine()
        writer.flush()
    }

    fun executeWithReturnString(cmd: String, searchString: String, pausedMode: PrefixedPausedMode = PrefixedPausedMode.PAUSING_KEEP): String {
        val data = Data(ttl, lock.newCondition(), searchString)
        lock.lock()
        conditions.add(data)
        execute(cmd, pausedMode)
        data.condition.await(WAITING_TIME_MS, TimeUnit.MILLISECONDS)
        lock.unlock()
        return data.returnString.orEmpty()
    }

    fun getProperty(property: String) = executeWithReturnString("get_property $property", "ANS_$property")

    fun setProperty(property: String, value: String) = execute("set_property $property $value")

    private data class Data(var ttl: Int, val condition: Condition, val searchString: String, var returnString: String? =
    null)


    /**
     * Load the given file/URL, stopping playback of the current file/URL.
     * If <append> is nonzero playback continues and the file/URL is
     * appended to the current playlist instead.
     */
    fun loadFile(file: String, append: Int = 0) = execute("loadfile $file $append")

    fun quit(value: Int = 0) = execute("quit", suffix = value.toString())

    fun isPaused(): Boolean = executeWithReturnString("get_property pause", "ANS_pause").trim() == "yes"

    fun stop() = execute("stop")

    fun pause() = execute("pause", PrefixedPausedMode.PAUSING_KEEP_FORCE)

    /**
     * length of file in seconds
     */
    val length: Double?
        get() = getProperty("length").toDoubleOrNull()

    /**
     * change volume
     */
    var volume: Double
        get() = getProperty("volume").toDouble()
        set(value) = setProperty("volume", "$value")

    var file: String
        get() = getProperty("path")
        set(value) {
            loadFile(value)
        }

    /**
     *  seek <value> [type]

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

    fun rectangle(witdth: Int) {
        execute("change_rectangle 4 $witdth")
    }
}