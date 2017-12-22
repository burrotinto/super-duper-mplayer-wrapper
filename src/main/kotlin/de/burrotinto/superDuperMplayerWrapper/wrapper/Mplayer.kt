package de.burrotinto.superDuperMplayerWrapper.wrapper

import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by Florian Klinger on 16.12.17, 15:51.
 */
class Mplayer(vararg options: String) {
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
        conditions.removeAll(list)

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

    private fun executeWithReturnString(cmd: String, searchString: String): String {
        val data = Data(LOCK.newCondition(), searchString)
        LOCK.lock()
        conditions.add(data)
        execute(cmd)
        data.condition.await(WAITING_TIME, TimeUnit.MILLISECONDS)
        LOCK.unlock()
        return data.returnString.orEmpty()
    }

    fun loadfile(file: String, append: Int = 0) = execute("loadfile $file $append")

    fun stop() = execute("stop")

    fun pause() = execute("pause")

    fun get_time_length(): Optional<Double> = Optional.ofNullable(executeWithReturnString("get_time_length", "ANS_LENGTH").toDoubleOrNull())

    fun get_time_pos(): Optional<Double> = Optional.ofNullable(executeWithReturnString("get_time_pos", "ANS_TIME_POSITION").toDoubleOrNull())

    fun loop(value: Int) = execute("loop $value", PrefixedPausedMode.PAUSING_KEEP_FORCE)

    fun isPaused(): Boolean = executeWithReturnString("get_property pause", "ANS_pause").trim() == "yes"

    fun get_file_name(): Optional<String> = Optional.ofNullable(executeWithReturnString("get_file_name", "ANS_FILENAME"))


    /**
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
    fun speed_mult(value: Double) {
        execute("speed_mult $value")
    }


    //    Set the speed to <value>.
    fun speed_set(value: Double) {
        execute("speed_set $value")
    }

    fun isFullscreen(): Boolean = executeWithReturnString("get_vo_fullscreen", "ANS_VO_FULLSCREEN").trim() == "1"



    class Data(val condition: Condition, val searchString: String, var returnString: String? = null)
}