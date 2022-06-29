package no.nav.helse.modell.kommando

import java.time.LocalDate
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringType
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import org.slf4j.LoggerFactory

internal class PersisterTotrinnsvurderingInntektCommand(
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val oppgaveDao: OppgaveDao,
    private val overstyrtVedtaksperiodeDao: OverstyrtVedtaksperiodeDao,
    private val automatiseringDao: AutomatiseringDao
) : Command {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean {

        val vedtaksperiodeId =
            oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeIdForSkjæringstidspunkt(fødselsnummer, organisasjonsnummer, skjæringstidspunkt)?.also {
                sikkerLogg.info("Fant vedtaksperiodeId via oppgave for $fødselsnummer og orgnr $organisasjonsnummer")
            } ?: automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(fødselsnummer, organisasjonsnummer)?.also {
                sikkerLogg.info("Fant vedtaksperiodeId via automatisering for $fødselsnummer og orgnr $organisasjonsnummer")
            }

        if(vedtaksperiodeId != null) {
            sikkerLogg.info("Fant vedtaksperiodeId $vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og skjæringstidspunkt $skjæringstidspunkt")
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, OverstyringType.Inntekt)
        } else {
            sikkerLogg.info("Fant ikke vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og skjæringstidspunkt $skjæringstidspunkt")
        }

        return true
    }
}
