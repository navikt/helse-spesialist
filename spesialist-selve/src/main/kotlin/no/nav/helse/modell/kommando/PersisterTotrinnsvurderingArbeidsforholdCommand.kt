package no.nav.helse.modell.kommando

import java.time.LocalDate
import no.nav.helse.spesialist.api.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import no.nav.helse.spesialist.api.overstyring.OverstyrtVedtaksperiodeDao
import org.slf4j.LoggerFactory

internal class PersisterTotrinnsvurderingArbeidsforholdCommand(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val oppgaveDao: OppgaveDao,
    private val overstyrtVedtaksperiodeDao: OverstyrtVedtaksperiodeDao,
) : Command {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean {
        val vedtaksperiodeId = oppgaveDao.finnAktivVedtaksperiodeId(fødselsnummer)

        if(vedtaksperiodeId != null) {
            sikkerLogg.info("Totrinns arbeidsforhold: Fant vedtaksperiodeId $vedtaksperiodeId for fnr $fødselsnummer og skjæringstidspunkt $skjæringstidspunkt")
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, OverstyringType.Arbeidsforhold)
        } else {
            sikkerLogg.info("Totrinns arbeidsforhold: Fant ikke vedtaksperiodeId for fnr $fødselsnummer og skjæringstidspunkt $skjæringstidspunkt")
        }

        return true
    }
}
