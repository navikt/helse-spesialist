package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.mediator.meldinger.Kommandohendelse

internal class TestKommandohendelse(
    override val id: UUID,
    private val vedtaksperiodeId: UUID?,
    private val fnr: String,
    private val json: String = "{}"
) : Kommandohendelse {

    override fun execute(context: CommandContext): Boolean {
        TODO("Not yet implemented")
    }

    override fun fødselsnummer(): String {
        return fnr
    }

    override fun vedtaksperiodeId(): UUID? {
        return vedtaksperiodeId
    }

    override fun toJson() = json
}
