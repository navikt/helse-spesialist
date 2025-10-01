package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import kotlin.reflect.KClass
import kotlin.reflect.KType

sealed interface RestOperasjonHåndterer<URLPARAMETERS : Any> {
    val urlPath: String
    val responseBodyType: KType
    val urlParametersClass: KClass<URLPARAMETERS>
}

interface GetHåndterer<URLPARAMETERS : Any, RESPONSEBODY> : RestOperasjonHåndterer<URLPARAMETERS> {
    fun extractParametre(
        pathParameters: Parameters,
        queryParameters: Parameters,
    ): URLPARAMETERS

    fun håndter(
        urlParametre: URLPARAMETERS,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<RESPONSEBODY>
}

interface PostHåndterer<URLPARAMETERS : Any, REQUESTBODY : Any, RESPONSEBODY> : RestOperasjonHåndterer<URLPARAMETERS> {
    fun extractParametre(
        pathParameters: Parameters,
        queryParameters: Parameters,
    ): URLPARAMETERS

    fun håndter(
        urlParametre: URLPARAMETERS,
        requestBody: REQUESTBODY,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        meldingsKø: KøetMeldingPubliserer,
    ): RestResponse<RESPONSEBODY>

    val requestBodyType: KType
}
