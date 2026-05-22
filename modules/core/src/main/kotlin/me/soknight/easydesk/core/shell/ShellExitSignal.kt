package me.soknight.easydesk.core.shell

/**
 * Thrown by a [Command] to signal that the shell loop should terminate.
 *
 * This is not an error — it is a control flow mechanism.
 *
 * @see EasyDeskShell
 */
internal class ShellExitSignal : Exception(null, null, true, false)
