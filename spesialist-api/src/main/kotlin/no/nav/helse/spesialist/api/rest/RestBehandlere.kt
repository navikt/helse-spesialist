package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

interface RestBehandler {
    fun openApi(config: RouteConfig)

    val påkrevdTilgang: Tilgang
    val påkrevdeBrukerroller: Set<Brukerrolle> get() = emptySet()

    fun operationIdBasertPåKlassenavn(): String =
        this::class
            .simpleName!!
            .removeSuffix("Behandler")
            .let { it[0].lowercase() + it.substring(1) }
}

inline fun <reified REQUEST, reified RESPONSE, reified ERROR : ApiErrorCode> RestBehandler.openApiMedRequestBody(config: RouteConfig) {
    config.request {
        body<REQUEST>()
    }
    openApiUtenRequestBody<RESPONSE, ERROR>(config)
}

inline fun <reified RESPONSE, reified ERROR : ApiErrorCode> RestBehandler.openApiUtenRequestBody(config: RouteConfig) {
    config.operationId = operationIdBasertPåKlassenavn()
    config.response {
        val hasResponseBody = RESPONSE::class != Unit::class
        code(if (hasResponseBody) HttpStatusCode.OK else HttpStatusCode.NoContent) {
            description = "Vellykket svar"
            if (hasResponseBody) {
                body<RESPONSE>()
            }
        }
        default {
            description = "Svar ved feil"
            body<ApiHttpProblemDetails<ERROR>> {
                mediaTypes = setOf(io.ktor.http.ContentType.Application.ProblemJson)
            }
        }
    }
    openApi(config)
}

interface RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR : ApiErrorCode> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR>
}

interface RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        request: REQUEST,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR>
}

interface GetBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR>

interface DeleteBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR>

interface PatchBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

interface PostBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

interface PutBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

abstract class ForPersonBehandlerUtenBody<RESOURCE, RESPONSE, ERROR : ApiErrorCode>(
    private val personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    private val personPseudoIdIkkeFunnet: ERROR,
    private val manglerTilgangTilPerson: ERROR,
) : RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR> {
    abstract fun behandle(
        resource: RESOURCE,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR>

    final override fun behandle(
        resource: RESOURCE,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(personPseudoId(resource).pseudoId),
            personPseudoIdIkkeFunnet = { personPseudoIdIkkeFunnet },
            manglerTilgangTilPerson = { manglerTilgangTilPerson },
        ) { person: Person -> behandle(resource, person, kallKontekst) }
}

abstract class GetForPersonBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode>(
    personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    personPseudoIdIkkeFunnet: ERROR,
    manglerTilgangTilPerson: ERROR,
) : ForPersonBehandlerUtenBody<RESOURCE, RESPONSE, ERROR>(
        personPseudoId = personPseudoId,
        personPseudoIdIkkeFunnet = personPseudoIdIkkeFunnet,
        manglerTilgangTilPerson = manglerTilgangTilPerson,
    ),
    GetBehandler<RESOURCE, RESPONSE, ERROR>
