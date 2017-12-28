package de.burrotinto.superDuperMplayerWrapper

import de.burrotinto.superDuperMplayerWrapper.wrapper.PrefixedPausedMode
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by Florian Klinger on 23.12.17, 21:04.
 */
abstract class AbstractMplayer(vararg options: String) {
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

    abstract fun notUnderstand(string: String)

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

    fun getProperty(property: String) = executeWithReturnString("get_property $property", "ANS_$property")

    fun setProperty(property: String, value: String) = execute("set_property $property $value")

    private class Data(val condition: Condition, val searchString: String, var returnString: String? = null)
}