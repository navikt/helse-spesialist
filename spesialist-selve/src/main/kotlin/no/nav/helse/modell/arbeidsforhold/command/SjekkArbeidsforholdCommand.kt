package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.warningteller
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger("sjekk-arbeidsforhold")
    override fun execute(context: CommandContext): Boolean {
        if(arbeidsforholdId.isNullOrBlank() || periodetype != Periodetype.FØRSTEGANGSBEHANDLING){
            return true
        }
        val aktiveArbeidsforhold = arbeidsforholdDao.findArbeidsforhold(fødselsnummer, orgnummer)
            .filter { it.startdato <= skjæringstidspunkt }
            .filter { it.sluttdato == null || it.sluttdato > skjæringstidspunkt }
        if (aktiveArbeidsforhold.size > 1){
            val melding = "ArbeidsforholdsID er fylt ut i inntektsmeldingen. Kontroller om brukeren har flere arbeidsforhold i samme virksomhet. Flere arbeidsforhold støttes ikke av systemet foreløpig."
            log.info("Legger til warning for arbeidsforholdId på vedtaksperiode $vedtaksperiodeId")
            warningDao.leggTilWarning(vedtaksperiodeId, Warning.warning(melding, WarningKilde.Spesialist))
            warningteller.labels("WARN", melding).inc()
        }
        return true
    }
}
