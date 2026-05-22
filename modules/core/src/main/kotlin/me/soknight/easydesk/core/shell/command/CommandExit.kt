package me.soknight.easydesk.core.shell.command

import me.soknight.easydesk.core.shell.Command
import me.soknight.easydesk.core.shell.ShellExitSignal

/**
 * Built-in command that terminates the shell loop.
 *
 * @see me.soknight.easydesk.core.shell.EasyDeskShell
 */
internal class CommandExit : Command {

    override val name = "exit"
    override val description = "exit the shell"

    override fun execute(args: List<String>) =
        throw ShellExitSignal()

}
