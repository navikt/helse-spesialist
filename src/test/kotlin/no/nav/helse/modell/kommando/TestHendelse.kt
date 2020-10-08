package no.nav.helse.modell.kommando

import no.nav.helse.mediator.meldinger.Hendelse
import java.util.*

internal class TestHendelse(
    override val id: UUID,
    private val vedtaksperiodeId: UUID?,
    private val fnr: String,
    private val json: String = "{}"
) : Hendelse {

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
