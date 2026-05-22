package me.soknight.easydesk.core.shell

/**
 * Represents a single executable shell command.
 *
 * @see EasyDeskShell
 */
interface Command {

    /** Unique command name used for invocation and tab-completion. */
    val name: String

    /** Short one-line description shown in `help` output. */
    val description: String

    /**
     * Executes this command with the given [args].
     */
    fun execute(args: List<String>)

}
