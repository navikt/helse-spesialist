package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

fun interface Tilgangsgruppehenter {
    fun hentTilgangsgrupper(saksbehandlerOid: SaksbehandlerOid): Either<Pair<Set<Tilgangsgruppe>, Set<Brukerrolle>>, Feil>

    sealed interface Feil {
        data object SaksbehandlerFinnesIkke : Feil
    }
}
