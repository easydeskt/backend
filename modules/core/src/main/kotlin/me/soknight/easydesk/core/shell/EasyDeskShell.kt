package me.soknight.easydesk.core.shell

import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.shell.command.CommandExit
import me.soknight.easydesk.core.shell.command.CommandHelp
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString

/**
 * Interactive REPL shell powered by JLine3.
 *
 * Register commands via [registerCommand], then call [start] to enter the read-eval loop.
 * Use [printAbove] to display messages above the fixed prompt line.
 *
 * @see Command
 */
object EasyDeskShell {

    private val prompt = AttributedString.fromAnsi("\u001b[92measydesk\u001b[0m → ")

    private val commands = linkedMapOf<String, Command>()
    private val logger = getLogger()

    @Volatile private var activeReader: LineReader? = null
    @Volatile private var activeTerminal: Terminal? = null

    init {
        registerCommand(CommandHelp())
        registerCommand(CommandExit())
    }

    /** Registers a [Command] by its [name][Command.name]. */
    fun registerCommand(command: Command) {
        commands[command.name] = command
    }

    /**
     * Prints a message above the current prompt line, keeping the prompt fixed at the bottom.
     *
     * Falls back to [println] when the shell is not running.
     */
    fun printAbove(message: String) {
        activeReader?.apply {
            // pass the raw string so embedded ANSI codes are output literally,
            // bypassing JLine's attribute-stripping on dumb terminals
            printAbove(message)
            return
        }

        println(message)
    }

    /** Starts the interactive read-eval-print loop (blocks the calling thread). */
    fun start() {
        val terminal = TerminalBuilder.builder().dumb(true).build()
        val completer = AggregateCompleter(StringsCompleter(commands.keys))
        val reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(completer)
            .build()

        this.activeTerminal = terminal
        this.activeReader = reader

        while (true) {
            val line = try {
                reader.readLine(prompt.toAnsi(terminal)).trim()
            } catch (ex: Throwable) {
                when (ex) {
                    is EndOfFileException, is UserInterruptException -> break
                    is IllegalStateException if ex.isTerminalClosed -> break
                    else -> throw ex
                }
            }

            if (line.isBlank()) continue

            val parts = line.split("\\s+".toRegex())
            val name = parts.first()
            val args = parts.drop(1)

            val command = commands[name]
            if (command == null) {
                printAbove("\u001b[31mUnknown command!\u001b[0m")
                continue
            }

            try {
                command.execute(args)
            } catch (_: ShellExitSignal) {
                break
            } catch (ex: Exception) {
                logger.error("Couldn't execute command: '{}'", name, ex)
            }
        }

        stop()
    }

    /**
     * Disables the shell prompt so that subsequent log messages
     * go through plain [println] instead of [printAbove].
     *
     * Safe to call from any thread (e.g. a shutdown hook).
     */
    fun stop() {
        this.activeReader = null

        // erase the prompt line left on screen: \r moves cursor to start, \033[K clears to end of line
        print("\r\u001b[K")
        System.out.flush()

        this.activeTerminal?.close()
        this.activeTerminal = null
    }

    internal fun registeredCommands(): Collection<Command> =
        commands.values

    private val IllegalStateException.isTerminalClosed: Boolean
        get() = message?.equals("terminal has been closed", true) ?: false

}
