package no.nav.helse.modell.command.nyny

import no.nav.helse.mediator.kafka.meldinger.Hendelse
import java.util.*

internal class TestHendelse(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fnr: String
) :
    Hendelse {
    override fun execute(context: CommandContext): Boolean {
        TODO("Not yet implemented")
    }

    override fun fødselsnummer(): String {
        return fnr
    }

    override fun vedtaksperiodeId(): UUID {
        return vedtaksperiodeId
    }

    override fun toJson(): String {
        TODO("Not yet implemented")
    }
}
