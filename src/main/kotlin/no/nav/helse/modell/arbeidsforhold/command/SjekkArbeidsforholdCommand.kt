package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.time.LocalDate
import java.util.*

internal class SjekkArbeidsforholdCommand(
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periodetype: Periodetype,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsforholdId: String?,
    arbeidsforholdDao: ArbeidsforholdDao,
    warningDao: WarningDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        return true
    }
}
