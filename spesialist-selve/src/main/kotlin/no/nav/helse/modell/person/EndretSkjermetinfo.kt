package no.nav.helse.modell.person

import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand

internal class EndretSkjermetinfo(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = emptyList()

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json
}
