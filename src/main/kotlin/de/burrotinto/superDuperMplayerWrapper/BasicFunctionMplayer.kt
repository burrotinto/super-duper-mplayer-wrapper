package de.burrotinto.superDuperMplayerWrapper.wrapper

import de.burrotinto.superDuperMplayerWrapper.AbstractMplayer
import java.io.File

/**
 * Created by Florian Klinger on 16.12.17, 15:51.
 */
abstract class BasicFunctionMplayer(vararg options: String) : AbstractMplayer(*options) {


    override fun notUnderstand(string: String) {
        println(string)
    }

    fun loadfile(file: String, append: Int = 0) = execute("loadfile $file $append")

    fun quit(value: Int = 0) = execute("quit", suffix = value.toString())

    fun isPaused(): Boolean = executeWithReturnString("get_property pause", "ANS_pause").trim() == "yes"

    fun stop() = execute("stop")

    fun pause() = execute("pause")

    val length: Double?
        get() = getProperty("length").toDoubleOrNull()


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


//    override fun toString(): String {
//        val sb = StringBuilder()
//        sb.append("FILE: ").append(file?.toString()).append("\n")
//        sb.append("LENGTH: $length\n")
//        sb.append("POS: ").append(timePos).append("\n")
//        sb.append("SPEED: ").append(speed).append("\n")
//        sb.append("BITRATE: $bitrate\n")
//        sb.append("LOOP: $loop")
//        return sb.toString()
//    }


}
