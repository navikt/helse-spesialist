package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.mediator.meldinger.VedtaksperiodeHendelse

internal class TestHendelse(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fnr: String,
    private val json: String = "{}"
) : Kommandohendelse, VedtaksperiodeHendelse {
    override fun execute(context: CommandContext): Boolean {
        TODO("Not yet implemented")
    }

    override fun fødselsnummer(): String {
        return fnr
    }

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun toJson() = json
}
