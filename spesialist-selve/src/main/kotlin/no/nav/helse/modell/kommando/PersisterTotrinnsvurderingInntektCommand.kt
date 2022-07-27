package no.nav.helse.modell.kommando

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveDao.NyesteVedtaksperiodeTotrinn
import no.nav.helse.oppgave.OppgaveDao.NyesteVedtaksperiodeTotrinn.Companion.nyestePeriode
import no.nav.helse.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.overstyring.OverstyringType.Inntekt
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

    private fun finnVedtaksperiodeId(
        manuell: NyesteVedtaksperiodeTotrinn?,
        automatisk: NyesteVedtaksperiodeTotrinn?,
        tilGodkjenning: NyesteVedtaksperiodeTotrinn?
    ): UUID? {
        if (manuell != null && automatisk != null) return nyestePeriode(
            manuell,
            automatisk
        ).vedtaksperiodeId
        else if (manuell != null) return manuell.vedtaksperiodeId
        else if (automatisk != null) return automatisk.vedtaksperiodeId
        return tilGodkjenning?.vedtaksperiodeId
    }
    override fun execute(context: CommandContext): Boolean {
        val nyesteManuelleVedtaksperiode =
            oppgaveDao.finnNyesteVedtaksperiodeIdMedStatusForSkjæringstidspunkt(
                fødselsnummer,
                organisasjonsnummer,
                skjæringstidspunkt,
                Ferdigstilt
            )
        val nyesteAutomatiskeVedtaksperiode =
            automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(fødselsnummer, organisasjonsnummer)
        val nyesteVedtaksperiodeTilGodkjenning =
            oppgaveDao.finnNyesteVedtaksperiodeIdMedStatusForSkjæringstidspunkt(
                fødselsnummer,
                organisasjonsnummer,
                skjæringstidspunkt,
                AvventerSaksbehandler
            )

        val vedtaksperiodeId = finnVedtaksperiodeId(
            nyesteManuelleVedtaksperiode,
            nyesteAutomatiskeVedtaksperiode,
            nyesteVedtaksperiodeTilGodkjenning
        )

        if(vedtaksperiodeId != null) {
            sikkerLogg.info("Totrinns inntekt: Fant vedtaksperiodeId $vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og skjæringstidspunkt $skjæringstidspunkt")
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, Inntekt)
        } else {
            sikkerLogg.info("Totrinns inntekt: Fant ikke vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og skjæringstidspunkt $skjæringstidspunkt")
        }

        return true
    }
}
