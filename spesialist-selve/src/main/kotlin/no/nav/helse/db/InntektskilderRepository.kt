package no.nav.helse.db

import no.nav.helse.modell.InntektskildeDto

internal interface InntektskilderRepository {
    fun lagre(inntektskilder: List<InntektskildeDto>)
}
