package no.nav.helse.db

import no.nav.helse.modell.InntektskildeDto

internal interface InntektskilderRepository {
    fun lagreInntektskilder(inntektskilder: List<InntektskildeDto>)

    fun finnInntektskildeMedOrgnummer(orgnummer: String): Long?

    fun finnInntektskilder(
        fødselsnummer: String,
        andreOrganisasjonsnumre: List<String>,
    ): List<InntektskildeDto>
}
