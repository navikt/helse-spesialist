package no.nav.helse.mediator.meldinger

import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import java.util.*

internal class OverstyringArbeidsforhold(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = emptyList()
    override fun fødselsnummer(): String = fødselsnummer
    override fun toJson(): String = json
}
