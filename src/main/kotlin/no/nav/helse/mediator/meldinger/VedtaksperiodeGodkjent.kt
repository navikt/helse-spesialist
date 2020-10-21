package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.vedtak.WarningDto
import java.util.*

internal data class VedtaksperiodeGodkjent(
    val vedtaksperiodeId: UUID,
    val fødselsnummer: String,
    val warnings: List<WarningDto>,
    val periodetype: String,
    val løsning: JsonNode
)
