package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.mediator.meldinger.VedtaksperiodemeldingOld

internal class TestMelding(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fnr: String,
    private val json: String = "{}"
) : VedtaksperiodemeldingOld {
    override fun fødselsnummer(): String = fnr

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun toJson() = json
}
