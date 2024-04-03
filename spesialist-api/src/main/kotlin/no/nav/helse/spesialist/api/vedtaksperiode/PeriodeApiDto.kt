package no.nav.helse.spesialist.api.vedtaksperiode

import com.fasterxml.jackson.annotation.JsonProperty

enum class Mottakertype {
    ARBEIDSGIVER,
    SYKMELDT,
    BEGGE,
}

enum class Periodetype {
    FØRSTEGANGSBEHANDLING,
    FORLENGELSE,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
}

enum class Inntektskilde { EN_ARBEIDSGIVER, FLERE_ARBEIDSGIVERE }

data class EnhetDto(
    @JsonProperty("id") private val _id: String,
    val navn: String,
) {
    val id get() = if (_id.length == 3) "0$_id" else _id
}
