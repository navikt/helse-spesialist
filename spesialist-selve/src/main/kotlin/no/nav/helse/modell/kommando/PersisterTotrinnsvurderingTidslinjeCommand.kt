package no.nav.helse.modell.kommando

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
) : Command {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean {
        if (overstyrteDager.isEmpty()) {
            sikkerLogg.info("OverstyrteDager er tom for fnr $fødselsnummer og orgnr $organisasjonsnummer i PersisterTotrinnsvurderingTidslinjeCommand")
            return true
        }
        val forsteDag = overstyrteDager.first().dato

        val vedtaksperiodeId = oppgaveDao.finnNyesteUtbetalteEllerAktiveVedtaksperiodeId(fødselsnummer, organisasjonsnummer)

        if(vedtaksperiodeId != null) {
            sikkerLogg.info("Fant vedtaksperiodeId $vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og første overstyrte dag $forsteDag")
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(vedtaksperiodeId, OverstyringType.Dager)
        } else {
            sikkerLogg.info("Fant ikke vedtaksperiodeId for fnr $fødselsnummer, orgnr $organisasjonsnummer og første overstyrte dag $forsteDag ")
        }

        return true
    }
}
