package me.soknight.easydesk.core.logging

import ch.qos.logback.core.rolling.TriggeringPolicyBase
import java.io.File
import kotlin.concurrent.Volatile

internal class RollOncePerSessionTriggeringPolicy<E> : TriggeringPolicyBase<E>() {

    @Volatile
    private var doRolling = true;

    override fun isTriggeringEvent(activeFile: File, event: E): Boolean {
        if (!doRolling) return false

        this.doRolling = false
        return activeFile.length() > 0L
    }

}