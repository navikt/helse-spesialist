package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveDao.NyesteVedtaksperiodeTotrinn
import no.nav.helse.oppgave.OppgaveDao.NyesteVedtaksperiodeTotrinn.Companion.nyestePeriode
import no.nav.helse.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.overstyring.OverstyringType.Dager
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import org.slf4j.LoggerFactory

internal class PersisterTotrinnsvurderingTidslinjeCommand(
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val overstyrteDager: List<OverstyringDagDto>,
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
        if (manuell != null && automatisk != null) return nyestePeriode(manuell, automatisk).vedtaksperiodeId
        else if (manuell != null) return manuell.vedtaksperiodeId
        else if (automatisk != null) return automatisk.vedtaksperiodeId
        return tilGodkjenning?.vedtaksperiodeId
    }

    override fun execute(context: CommandContext): Boolean {
        if (overstyrteDager.isEmpty()) {
            sikkerLogg.info("OverstyrteDager er tom for fnr $fødselsnummer og orgnr $organisasjonsnummer i PersisterTotrinnsvurderingTidslinjeCommand")
            return true
        }
        val forsteDag = overstyrteDager.first().dato

        val nyesteManuelleVedtaksperiode =
            oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(
                fødselsnummer,
                organisasjonsnummer,
                forsteDag,
                Ferdigstilt
            )
        val nyesteAutomatiskeVedtaksperiode =
            automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(fødselsnummer, organisasjonsnummer)
        val nyesteVedtaksperiodeTilGodkjenning =
            oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(
                fødselsnummer,
                organisasjonsnummer,
                forsteDag,
                AvventerSaksbehandler
            )

        val vedtaksperiodeId = finnVedtaksperiodeId(
            nyesteManuelleVedtaksperiode,
            nyesteAutomatiskeVedtaksperiode,
            nyesteVedtaksperiodeTilGodkjenning
        )

        if (vedtaksperiodeId != null) {
            sikkerLogg.info("Fant vedtaksperiodeId $vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og første overstyrte dag $forsteDag")
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, Dager)
        } else {
            sikkerLogg.info("Fant ikke vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og første overstyrte dag $forsteDag ")
        }

        return true
    }
}
