package no.nav.helse.db

import no.nav.helse.modell.InntektskildeDto
import no.nav.helse.modell.KomplettInntektskildeDto

internal interface InntektskilderRepository {
    fun lagreInntektskilder(inntektskilder: List<KomplettInntektskildeDto>)

    fun finnInntektskilder(
        fødselsnummer: String,
        andreOrganisasjonsnumre: List<String>,
    ): List<InntektskildeDto>
}
