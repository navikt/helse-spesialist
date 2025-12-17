package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext

interface Command {
    fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean

    fun resume(
        context: CommandContext,
        sessionContext: SessionContext,
    ) = true

    fun hash(): String = name

    val name: String get() = this::class.java.simpleName
}

internal fun ikkesuspenderendeCommand(
    navnForLogging: String = "<navn ikke oppgitt>",
    block: () -> Unit,
) = object : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        block()
        return true
    }

    override val name = navnForLogging
}
