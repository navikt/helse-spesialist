package no.nav.helse.modell.kommando

import java.time.LocalDate
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import org.slf4j.LoggerFactory

internal class PersisterTotrinnsvurderingInntektCommand(
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val oppgaveDao: OppgaveDao,
    private val overstyrtVedtaksperiodeDao: OverstyrtVedtaksperiodeDao,
) : Command {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean {
        val vedtaksperiodeId = oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeIdForSkjæringstidspunkt(fødselsnummer, organisasjonsnummer, skjæringstidspunkt)

        if(vedtaksperiodeId != null) {
            sikkerLogg.info("Fant vedtaksperiodeId $vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og skjæringstidspunkt $skjæringstidspunkt")
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId)
        } else {
            sikkerLogg.info("Fant ikke vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og skjæringstidspunkt $skjæringstidspunkt")
        }

        return true
    }
}
