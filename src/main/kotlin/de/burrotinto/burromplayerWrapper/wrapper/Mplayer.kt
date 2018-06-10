package de.burrotinto.burromplayerWrapper.wrapper

import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by Florian Klinger on 16.12.17, 15:51.
 */
class Mplayer(vararg options: String) {
    companion object {
        val WAITING_TIME = 500L
    }

    private val LOCK = ReentrantLock()
    private val TOTAL_TIME_CONDITION = LOCK.newCondition()
    private val CURRENT_TIME_CONDITION = LOCK.newCondition()
    private val PAUSED_CONDITION = LOCK.newCondition()
    private val FILENAME_CONDITION = LOCK.newCondition()


    private val mplayer: Process = ProcessBuilder(listOf("mplayer", "-slave", "-idle", "-quiet") +
            options).start()
    private val reader: BufferedReader
    private val writer: BufferedWriter

    private var total = Optional.empty<Double>()
    private var current = Optional.empty<Double>()
    private var isPaused = Optional.empty<Boolean>()
    private var currentFileName = Optional.empty<String>()


    init {
        reader = mplayer.inputStream.bufferedReader(Charset.defaultCharset())
        writer = mplayer.outputStream.bufferedWriter(Charset.defaultCharset())

        Thread(Runnable {
            reader.forEachLine { read(it) }
        }).start()
    }

    private fun read(string: String) {
        LOCK.lock()
        when {
            string.startsWith("ANS_LENGTH") -> {
                total = Optional.ofNullable(string.split("=")[1].toDouble())
                TOTAL_TIME_CONDITION.signalAll()
            }
            string.startsWith("ANS_TIME_POSITION") -> {
                current = Optional.of(string.split("=")[1].toDouble())
                CURRENT_TIME_CONDITION.signalAll()
            }
            string.startsWith("ANS_pause") -> {
                isPaused = Optional.of(string.split("=")[1] == "yes")
                PAUSED_CONDITION.signalAll()
            }
            string.startsWith("ANS_FILENAME") -> {
                currentFileName = Optional.of(string.split("=")[1])
                FILENAME_CONDITION.signalAll()
            }
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

    fun loadfile(file: String, append: Int = 0) = execute("loadfile $file $append")

    fun stop() = execute("stop")

    fun pause() = execute("pause")

    fun get_time_length(): Optional<Double> {
        LOCK.lock()
//        total = Optional.empty()
        execute("get_time_length", PrefixedPausedMode.PAUSING_KEEP_FORCE)
        TOTAL_TIME_CONDITION.await(WAITING_TIME, TimeUnit.MILLISECONDS)
        LOCK.unlock()
        return total
    }

    fun get_time_pos(): Optional<Double> {
        LOCK.lock()
//        current = Optional.empty()
        execute("get_time_pos", PrefixedPausedMode.PAUSING_KEEP_FORCE)
        CURRENT_TIME_CONDITION.await(WAITING_TIME, TimeUnit.MILLISECONDS)
        LOCK.unlock()
        return current
    }

    fun loop(value: Int) = execute("loop $value", PrefixedPausedMode.PAUSING_KEEP_FORCE)

    fun isPaused(): Optional<Boolean> {
        LOCK.lock()
        execute("get_property pause", PrefixedPausedMode.PAUSING_KEEP_FORCE)
        val time = PAUSED_CONDITION.await(WAITING_TIME, TimeUnit.MILLISECONDS)
        LOCK.unlock()
        return isPaused
    }

    fun get_file_name(): Optional<String> {
        LOCK.lock()
        currentFileName = Optional.empty()
        execute("get_file_name")
        FILENAME_CONDITION.await(WAITING_TIME, TimeUnit.MILLISECONDS)
        LOCK.unlock()
        return currentFileName
    }

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
}

fun main(args: Array<String>) {
    val mplayer = Mplayer()
    mplayer.loadfile("/home/derduke/Downloads/out.mp4")

    var total = mplayer.get_time_length().orElse(0.0)
    var current = 0.0
    mplayer.get_time_pos()

    mplayer.get_time_length()
    mplayer.loop(1)
    println(mplayer.get_file_name().orElse(""))

    Thread(object : Runnable {
        override fun run() {
            while (true) {
                Thread.sleep(50)
                current = mplayer.get_time_pos().orElse(0.0)
                when {
                    ((current + 0.2 >= total || (current + 1 >= (total / 2)) && (current <= (total / 2))) &&
                            !mplayer.isPaused().orElse(false)) -> {
                        mplayer.pause()
                    }
                }
            }
        }
    }).start()

    while (true) {
        var x = readLine()
        when (x) {
            "" -> mplayer.seek(2, (total - current).toString())
            else -> mplayer.speed_set(x!!.toDouble())
        }
    }

}

