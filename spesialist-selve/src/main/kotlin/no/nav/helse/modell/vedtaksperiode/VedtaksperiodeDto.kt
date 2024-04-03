package no.nav.helse.modell.vedtaksperiode

import java.util.UUID

enum class Periodetype {
    FØRSTEGANGSBEHANDLING,
    FORLENGELSE,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
}

enum class Inntektskilde {
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
}

data class VedtaksperiodeDto(
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val forkastet: Boolean,
    val generasjoner: List<GenerasjonDto>,
)
