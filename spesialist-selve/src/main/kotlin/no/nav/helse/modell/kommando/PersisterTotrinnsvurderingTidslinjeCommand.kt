package no.nav.helse.modell.kommando

import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.overstyring.OverstyringType
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

    override fun execute(context: CommandContext): Boolean {
        if (overstyrteDager.isEmpty()) {
            sikkerLogg.info("OverstyrteDager er tom for fnr $fødselsnummer og orgnr $organisasjonsnummer i PersisterTotrinnsvurderingTidslinjeCommand")
            return true
        }
        val forsteDag = overstyrteDager.first().dato


        val vedtaksperiodeId =
            oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeId(fødselsnummer, organisasjonsnummer, forsteDag)?.also {
                    sikkerLogg.info("Fant vedtaksperiodeId via oppgave for $fødselsnummer og orgnr $organisasjonsnummer")
                } ?: automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(fødselsnummer, organisasjonsnummer)?.also {
                sikkerLogg.info("Fant vedtaksperiodeId via automatisering for $fødselsnummer og orgnr $organisasjonsnummer")
            }


        if (vedtaksperiodeId != null) {
            sikkerLogg.info("Fant vedtaksperiodeId $vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og første overstyrte dag $forsteDag")
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, OverstyringType.Dager)
        } else {
            sikkerLogg.info("Fant ikke vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og første overstyrte dag $forsteDag ")
        }

        return true
    }
}
