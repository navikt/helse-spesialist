package no.nav.helse.modell.vedtaksperiode.vedtak

import java.util.UUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodehendelse

internal class VedtakFattet(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String
) : Vedtaksperiodehendelse {

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
    override fun toJson(): String = json
}