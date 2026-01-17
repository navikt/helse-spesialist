package no.nav.helse.spesialist.api.graphql.query

import graphql.GraphQLError
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.auditLogTeller
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.forbiddenError
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.notFoundError
import no.nav.helse.spesialist.api.graphql.personNotReadyError
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.slf4j.LoggerFactory

interface PersonoppslagService {
    fun hentPerson(
        fødselsnummer: String,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): FetchPersonResult

    fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean

    fun fødselsnumreKnyttetTil(aktørId: String): Set<String>

    fun fødselsnummerKnyttetTil(personPseudoId: PersonPseudoId): Identitetsnummer?
}

sealed interface FetchPersonResult {
    class Ok(
        val person: ApiPerson,
    ) : FetchPersonResult

    sealed interface Feil : FetchPersonResult {
        class IkkeKlarTilVisning(
            val aktørId: String,
        ) : Feil

        data object ManglerTilgang : Feil

        data object IkkeFunnet : Feil

        data object KlarteIkkeHente : Feil
    }
}

class PersonQueryHandler(
    private val personoppslagService: PersonoppslagService,
) : PersonQuerySchema {
    private val auditLog = LoggerFactory.getLogger("auditLogger")

    override suspend fun person(
        personPseudoId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPerson?> =
        håndterRequest(
            personPseudoId =
                runCatching { PersonPseudoId.fraString(personPseudoId) }
                    .getOrElse { return byggFeilrespons(notFoundError(personPseudoId)) },
            saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER),
            tilgangsgrupper = env.graphQlContext.get<Set<Tilgangsgruppe>>(ContextValues.TILGANGSGRUPPER),
        )

    private fun håndterRequest(
        personPseudoId: PersonPseudoId,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): DataFetcherResult<ApiPerson?> {
        val fødselsnummer =
            (
                personoppslagService.fødselsnummerKnyttetTil(personPseudoId)
                    ?: run {
                        loggNotFoundForPersonPseudoId(personPseudoId.value.toString(), saksbehandler)
                        return byggFeilrespons(notFoundError(personPseudoId.value.toString()))
                    }
            ).value

        loggInfo(
            "Personoppslag på person",
            "fødselsnummer: $fødselsnummer",
        )

        return when (val result = personoppslagService.hentPerson(fødselsnummer, saksbehandler, tilgangsgrupper)) {
            is FetchPersonResult.Feil -> {
                result.auditlogg(saksbehandler, fødselsnummer)
                result.tilGraphqlError(fødselsnummer)
            }

            is FetchPersonResult.Ok -> {
                auditLog(saksbehandler, fødselsnummer, true, null)
                byggRespons(result.person)
            }
        }
    }

    private fun FetchPersonResult.Feil.auditlogg(
        saksbehandler: Saksbehandler,
        fødselsnummer: String,
    ) {
        when (this) {
            is FetchPersonResult.Feil.IkkeFunnet -> {
                auditLog(saksbehandler, `fødselsnummer`, true, notFoundError(`fødselsnummer`).message)
            }

            is FetchPersonResult.Feil.IkkeKlarTilVisning -> {
                auditLog(saksbehandler, `fødselsnummer`, false, null)
            }

            is FetchPersonResult.Feil.ManglerTilgang -> {
                auditLog(saksbehandler, `fødselsnummer`, false, null)
            }

            is FetchPersonResult.Feil.KlarteIkkeHente -> {
                auditLog(saksbehandler, `fødselsnummer`, null, getSnapshotFetchError().message)
            }
        }
    }

    private fun FetchPersonResult.Feil.tilGraphqlError(fødselsnummer: String): DataFetcherResult<ApiPerson?> {
        val graphqlError =
            when (this) {
                is FetchPersonResult.Feil.IkkeFunnet -> notFoundError(fødselsnummer)
                is FetchPersonResult.Feil.IkkeKlarTilVisning -> personNotReadyError(fødselsnummer, aktørId)
                is FetchPersonResult.Feil.ManglerTilgang -> forbiddenError(fødselsnummer)
                is FetchPersonResult.Feil.KlarteIkkeHente -> getSnapshotFetchError()
            }

        return byggFeilrespons(graphqlError)
    }

    private fun loggNotFoundForPersonPseudoId(
        personPseudoId: String,
        saksbehandler: Saksbehandler,
    ) {
        loggInfo("Fant ikke person basert på personPseudoId: $personPseudoId")
        auditLog(saksbehandler, personPseudoId, null, notFoundError(personPseudoId).message)
    }

    private fun getSnapshotFetchError(): GraphQLError = graphqlErrorException(501, "Feil ved henting av snapshot for person", "field" to "person")

    private fun auditLog(
        saksbehandler: Saksbehandler,
        personId: String,
        harTilgang: Boolean?,
        fantIkkePersonErrorMsg: String?,
    ) {
        val saksbehandlerIdent = saksbehandler.ident.value
        auditLogTeller.increment()
        if (harTilgang == false) {
            auditLog.warn(
                "end=${System.currentTimeMillis()} suid=$saksbehandlerIdent duid=$personId operation=PersonQuery flexString1=Deny",
            )
        } else if (fantIkkePersonErrorMsg != null) {
            auditLog.warn(
                "end=${System.currentTimeMillis()} suid=$saksbehandlerIdent duid=$personId operation=PersonQuery msg=$fantIkkePersonErrorMsg",
            )
        } else {
            auditLog.info("end=${System.currentTimeMillis()} suid=$saksbehandlerIdent duid=$personId operation=PersonQuery")
        }
        sikkerlogg.debug(
            "audit-logget, operationName: PersonQuery, harTilgang: $harTilgang, fantIkkePersonErrorMsg: $fantIkkePersonErrorMsg",
        )
    }
}
