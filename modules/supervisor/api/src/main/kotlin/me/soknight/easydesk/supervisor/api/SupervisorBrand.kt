package me.soknight.easydesk.supervisor.api

/**
 * Identifies a supervisor surface type.
 *
 * Each supported management interface (Telegram supergroup, future CLI admin, etc.)
 * provides its own [SupervisorBrand] singleton alongside its [SupervisorProvider] implementation.
 * New surfaces are added without modifying this interface.
 *
 * @see SupervisorProvider
 */
interface SupervisorBrand {

    /** Unique machine-readable identifier stored in binding tables (e.g., `"telegram"`). */
    val identifier: String

    /** Human-readable surface name shown in the UI. Defaults to [identifier]. */
    val humanName: String
        get() = identifier

}
