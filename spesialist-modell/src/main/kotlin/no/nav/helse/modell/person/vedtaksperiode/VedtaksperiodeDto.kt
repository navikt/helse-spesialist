package no.nav.helse.modell.person.vedtaksperiode

import java.util.UUID

data class VedtaksperiodeDto(
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val forkastet: Boolean,
    val generasjoner: List<GenerasjonDto>,
)
