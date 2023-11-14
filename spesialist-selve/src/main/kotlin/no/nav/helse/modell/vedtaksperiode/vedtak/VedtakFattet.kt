package no.nav.helse.modell.vedtaksperiode.vedtak

import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.Generasjon

internal class VedtakFattet(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String,
    private val gjeldendeGenerasjon: Generasjon,
    private val vedtakDao: VedtakDao
) : Hendelse {

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
    override fun toJson(): String = json

    override fun execute(context: CommandContext): Boolean {
        gjeldendeGenerasjon.håndterVedtakFattet(id)
        if (vedtakDao.erSpesialsak(vedtaksperiodeId)) vedtakDao.spesialsakFerdigbehandlet(vedtaksperiodeId)
        return true
    }
}
