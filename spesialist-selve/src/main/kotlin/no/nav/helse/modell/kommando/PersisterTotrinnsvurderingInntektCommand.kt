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
        val nyesteManueltBehandletUtbetalteEllerAktiveVedtaksperiode =
            oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeIdForSkjæringstidspunkt(
                fødselsnummer,
                organisasjonsnummer,
                skjæringstidspunkt
            )
        val automatisertVedtaksperiode =
            automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(fødselsnummer, organisasjonsnummer)

        val vedtaksperiodeId =
            if ((nyesteManueltBehandletUtbetalteEllerAktiveVedtaksperiode?.fom ?: LocalDate.MIN).isAfter(
                    automatisertVedtaksperiode?.fom ?: LocalDate.MIN
                )
            ) {
                nyesteManueltBehandletUtbetalteEllerAktiveVedtaksperiode?.vedtaksperiodeId
            } else automatisertVedtaksperiode?.vedtaksperiodeId


        if(vedtaksperiodeId != null) {
            sikkerLogg.info("Fant vedtaksperiodeId $vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og skjæringstidspunkt $skjæringstidspunkt")
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, OverstyringType.Inntekt)
        } else {
            sikkerLogg.info("Fant ikke vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og skjæringstidspunkt $skjæringstidspunkt")
        }

        return true
    }
}
