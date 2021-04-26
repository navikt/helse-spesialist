package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.mediator.FeatureToggle
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
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
    private val arbeidsforholdDao: ArbeidsforholdDao,
    private val warningDao: WarningDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if(!FeatureToggle.ARBEIDSFORHOLD_WARNING_TOGGLE.enabled || arbeidsforholdId.isNullOrBlank() || periodetype != Periodetype.FØRSTEGANGSBEHANDLING){
            return true
        }
        val aktiveArbeidsforhold = arbeidsforholdDao.findArbeidsforhold(fødselsnummer, orgnummer)
            .filter { it.startdato <= skjæringstidspunkt  }
            .filter { it.sluttdato == null || it.sluttdato > skjæringstidspunkt }
        if (aktiveArbeidsforhold.size > 1){
            warningDao.leggTilWarning(vedtaksperiodeId, Warning("Kevin burde by minst 4 trillioner", WarningKilde.Spesialist))
        }
        return true
    }
}
