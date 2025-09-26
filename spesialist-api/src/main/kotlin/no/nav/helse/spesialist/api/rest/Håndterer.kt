package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import kotlin.reflect.KType

interface GetHåndterer<URLPARAMETRE, RESPONSEBODY> {
    val urlPath: String

    fun extractParametre(parameters: Parameters): URLPARAMETRE

    fun håndter(
        urlParametre: URLPARAMETRE,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<RESPONSEBODY>

    fun getResponseBodyType(): KType
}

interface PostHåndterer<URLPARAMETRE, REQUESTBODY, RESPONSEBODY> {
    fun håndter(
        urlParametre: URLPARAMETRE,
        requestBody: REQUESTBODY,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        meldingsKø: KøetMeldingPubliserer,
    ): RestResponse<RESPONSEBODY>
}
