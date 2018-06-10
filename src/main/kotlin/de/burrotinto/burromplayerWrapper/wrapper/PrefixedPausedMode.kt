package de.burrotinto.burromplayerWrapper.wrapper

/**
 * Created by Florian Klinger on 17.12.17, 08:05.
 */
enum class PrefixedPausedMode(val string: String) {
    PAUSING("pausing"), PAUSING_KEEP("pausing_keep"), PAUSING_TOGGLE("pausing_toggle"),
    PAUSING_KEEP_FORCE("pausing_keep_force")
}