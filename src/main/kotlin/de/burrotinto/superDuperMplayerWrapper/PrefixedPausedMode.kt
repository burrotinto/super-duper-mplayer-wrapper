package de.burrotinto.superDuperMplayerWrapper.wrapper

/**
 *
 * All commands can be prefixed with one of "pausing ", "pausing_keep ", or
"pausing_toggle ". "pausing " tells MPlayer to pause as soon as possible
after processing the command. "pausing_keep " tells MPlayer to do so only if
it was already in paused mode. "pausing_toggle " tells MPlayer to do so
only if it was not already in paused mode. Please note that "as soon as
possible" can be before the command is fully executed.
As a temporary hack, there is also the _experimental_ "pausing_keep_force "
prefix, with which MPlayer will not exit the pause loop at all.
Like this you can avoid the "frame stepping" effect of "pausing_keep "
but most commands will either not work at all or behave in unexpected ways.
For "set_mouse_pos" and "key_down_event", "pausing_keep_force" is the default
since other values do not make much sense for them.

 * Created by Florian Klinger on 17.12.17, 08:05.
 */
enum class PrefixedPausedMode(val string: String) {
    PAUSING("pausing"),
    PAUSING_KEEP("pausing_keep"),
    PAUSING_TOGGLE("pausing_toggle"),
    PAUSING_KEEP_FORCE("pausing_keep_force")
}