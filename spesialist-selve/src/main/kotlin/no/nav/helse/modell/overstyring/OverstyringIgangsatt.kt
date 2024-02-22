package no.nav.helse.modell.overstyring

import java.util.UUID
import no.nav.helse.mediator.meldinger.Personhendelse

internal class OverstyringIgangsatt(
    override val id: UUID,
    private val fødselsnummer: String,
    val kilde: UUID,
    val berørteVedtaksperiodeIder: List<UUID>,
    private val json: String,
) : Personhendelse {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}