package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

fun interface Tilgangsgruppehenter {
    fun hentTilgangsgrupper(saksbehandlerOid: SaksbehandlerOid): Either<Set<Tilgangsgruppe>, Feil>

    sealed interface Feil {
        data object SaksbehandlerFinnesIkke : Feil
    }
}
