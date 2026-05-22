package me.soknight.easydesk.core.shell.command

import me.soknight.easydesk.core.shell.Command
import me.soknight.easydesk.core.shell.EasyDeskShell

/**
 * Built-in command that prints all registered commands with their descriptions.
 *
 * @see EasyDeskShell
 */
internal class CommandHelp : Command {

    override val name = "help"
    override val description = "show available commands"

    override fun execute(args: List<String>) {
        val commands = EasyDeskShell.registeredCommands()
        val maxLength = commands.maxOf { it.name.length }

        commands.forEach {
            val command = "\u001b[1;93m${it.name.padEnd(maxLength)}"
            val description = "\u001b[0m${it.description}"
            EasyDeskShell.printAbove(" • $command \u001b[96m— $description")
        }
    }

}
