package no.nav.helse.db

import no.nav.helse.modell.Inntektskilde
import no.nav.helse.modell.InntektskildeDto

internal interface InntektskilderRepository {
    fun lagreInntektskilder(inntektskilder: List<InntektskildeDto>)

    fun inntektskildeEksisterer(orgnummer: String): Boolean

    fun finnInntektskilder(
        fødselsnummer: String,
        andreOrganisasjonsnumre: List<String>,
    ): List<InntektskildeDto>

    fun finnInntektskilderSomManglerNavnForAktiveOppgaver(): List<Inntektskilde> = emptyList()
}
