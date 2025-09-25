package no.nav.helse.spesialist.api.rest

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

interface PostHåndterer<URLPARAMETRE, REQUESTBODY, RESPONSE> {
    fun håndter(
        urlParametre: URLPARAMETRE,
        requestBody: REQUESTBODY,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RESPONSE
}
