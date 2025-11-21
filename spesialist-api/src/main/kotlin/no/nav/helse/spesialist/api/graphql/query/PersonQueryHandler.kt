package no.nav.helse.spesialist.api.graphql.query

import graphql.GraphQLContext
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
import no.nav.helse.spesialist.api.graphql.query.Inputvalidering.UgyldigInput
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private sealed interface Inputvalidering {
    class Ok(
        val fødselsnummer: String,
    ) : Inputvalidering

    sealed class UgyldigInput(
        val graphqlError: GraphQLError,
    ) : Inputvalidering {
        class UkjentFødselsnummer(
            val fødselsnummer: String,
            graphqlError: GraphQLError,
        ) : UgyldigInput(graphqlError)

        class UkjentAktørId(
            val aktørId: String,
            graphqlError: GraphQLError,
        ) : UgyldigInput(graphqlError)

        class UkjentPersonPseudoId(
            val personPseudoId: String,
            graphqlError: GraphQLError,
        ) : UgyldigInput(graphqlError)

        class UgyldigPersonPseudoId(
            graphqlError: GraphQLError,
        ) : UgyldigInput(graphqlError)

        class ParametreMangler(
            graphqlError: GraphQLError,
        ) : UgyldigInput(graphqlError)

        class UgyldigAktørId(
            graphqlError: GraphQLError,
        ) : UgyldigInput(graphqlError)

        class HarFlereFødselsnumre(
            val aktørId: String,
            graphqlError: GraphQLError,
        ) : UgyldigInput(graphqlError)
    }
}

interface PersonoppslagService {
    suspend fun hentPerson(
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
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    private val auditLog = LoggerFactory.getLogger("auditLogger")

    private companion object {
        private const val GYLDIG_AKTØRID_LENDGE = 13
    }

    override suspend fun person(
        fnr: String?,
        aktorId: String?,
        personPseudoId: String?,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPerson?> {
        val fødselsnummer =
            when (val validering = validerInput(fnr, aktorId, personPseudoId)) {
                is Inputvalidering.Ok -> validering.fødselsnummer
                is UgyldigInput -> {
                    validering.auditlogg(env)
                    return byggFeilrespons(validering.graphqlError)
                }
            }
        sikkerLogg.info("Personoppslag på fnr=$fødselsnummer")

        val saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER)
        val tilgangsgrupper = env.graphQlContext.get<Set<Tilgangsgruppe>>(ContextValues.TILGANGSGRUPPER)

        return when (val result = personoppslagService.hentPerson(fødselsnummer, saksbehandler, tilgangsgrupper)) {
            is FetchPersonResult.Feil -> {
                result.auditlogg(env, fødselsnummer)
                result.tilGraphqlError(fødselsnummer)
            }

            is FetchPersonResult.Ok -> {
                auditLog(env.graphQlContext, fødselsnummer, true, null)
                byggRespons(result.person)
            }
        }
    }

    private fun FetchPersonResult.Feil.auditlogg(
        env: DataFetchingEnvironment,
        fødselsnummer: String,
    ) {
        when (this) {
            is FetchPersonResult.Feil.IkkeFunnet ->
                auditLog(
                    env.graphQlContext,
                    fødselsnummer,
                    true,
                    notFoundError(fødselsnummer).message,
                )

            is FetchPersonResult.Feil.IkkeKlarTilVisning -> auditLog(env.graphQlContext, fødselsnummer, false, null)
            is FetchPersonResult.Feil.ManglerTilgang -> auditLog(env.graphQlContext, fødselsnummer, false, null)
            is FetchPersonResult.Feil.KlarteIkkeHente ->
                auditLog(
                    env.graphQlContext,
                    fødselsnummer,
                    null,
                    getSnapshotFetchError().message,
                )
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

    private fun Set<String>.harFlereFødselsnumre() = this.size > 1

    private fun Set<String>.harIngenFødselsnumre() = this.isEmpty()

    private fun ugyldigAktørId(aktørId: String) = aktørId.length != GYLDIG_AKTØRID_LENDGE

    private fun validerInput(
        fødselsnummer: String?,
        aktørId: String?,
        personPseudoId: String?,
    ): Inputvalidering {
        if (personPseudoId != null) {
            val gyldigPersonPseudoId =
                runCatching { PersonPseudoId.fraString(personPseudoId) }
                    .getOrElse { return UgyldigInput.UgyldigPersonPseudoId(notFoundError(personPseudoId)) }

            val identitetsnummer =
                personoppslagService.fødselsnummerKnyttetTil(gyldigPersonPseudoId)
                    ?: return UgyldigInput.UkjentPersonPseudoId(personPseudoId, notFoundError(personPseudoId))

            return Inputvalidering.Ok(identitetsnummer.value)
        }
        if (fødselsnummer != null) {
            if (personoppslagService.finnesPersonMedFødselsnummer(fødselsnummer)) {
                return Inputvalidering.Ok(fødselsnummer)
            }
            return UgyldigInput.UkjentFødselsnummer(fødselsnummer, notFoundError(fødselsnummer))
        }
        if (aktørId == null) return UgyldigInput.ParametreMangler(getBadRequestError("Requesten mangler både fødselsnummer og aktorId"))
        if (ugyldigAktørId(aktørId)) {
            return UgyldigInput.UgyldigAktørId(
                getBadRequestError("Feil lengde på parameter aktorId: ${aktørId.length}"),
            )
        }

        val fødselsnumre = personoppslagService.fødselsnumreKnyttetTil(aktørId)

        if (fødselsnumre.harIngenFødselsnumre()) return UgyldigInput.UkjentAktørId(aktørId, notFoundError(aktørId))
        if (fødselsnumre.harFlereFødselsnumre()) {
            return UgyldigInput.HarFlereFødselsnumre(
                aktørId,
                getFlereFødselsnumreError(fødselsnumre),
            )
        }

        return Inputvalidering.Ok(fødselsnumre.single())
    }

    private fun loggNotFoundForAktørId(
        aktorId: String,
        env: DataFetchingEnvironment,
    ) {
        sikkerLogg.info("Svarer not found for parametere aktorId=$aktorId.")
        auditLog(env.graphQlContext, aktorId, null, notFoundError(aktorId).message)
    }

    private fun loggNotFoundForPersonPseudoId(
        personPseudoId: String,
        env: DataFetchingEnvironment,
    ) {
        sikkerLogg.info("Svarer not found for parametere personPseudoId=$personPseudoId.")
        auditLog(env.graphQlContext, personPseudoId, null, notFoundError(personPseudoId).message)
    }

    private fun loggNotFoundForFødselsnummer(
        fnr: String,
        env: DataFetchingEnvironment,
    ) {
        sikkerLogg.info("Svarer not found for parametere fnr=$fnr.")
        auditLog(env.graphQlContext, fnr, null, notFoundError(fnr).message)
    }

    private fun getFlereFødselsnumreError(fødselsnumre: Set<String>): GraphQLError =
        graphqlErrorException(
            500,
            "Mer enn ett fødselsnummer for personen",
            "feilkode" to "HarFlereFodselsnumre",
            "fodselsnumre" to fødselsnumre,
        )

    private fun getSnapshotFetchError(): GraphQLError = graphqlErrorException(501, "Feil ved henting av snapshot for person", "field" to "person")

    private fun getBadRequestError(melding: String): GraphQLError = graphqlErrorException(400, melding)

    private fun auditLog(
        graphQLContext: GraphQLContext,
        personId: String,
        harTilgang: Boolean?,
        fantIkkePersonErrorMsg: String?,
    ) {
        val saksbehandlerIdent = graphQLContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER).ident
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
        sikkerLogg.debug(
            "audit-logget, operationName: PersonQuery, harTilgang: $harTilgang, fantIkkePersonErrorMsg: $fantIkkePersonErrorMsg",
        )
    }

    private fun UgyldigInput.auditlogg(env: DataFetchingEnvironment) {
        when (this) {
            is UgyldigInput.UkjentFødselsnummer -> loggNotFoundForFødselsnummer(this.fødselsnummer, env)
            is UgyldigInput.UkjentAktørId -> loggNotFoundForAktørId(aktørId, env)
            is UgyldigInput.UkjentPersonPseudoId -> loggNotFoundForPersonPseudoId(personPseudoId, env)
            is UgyldigInput.HarFlereFødselsnumre -> auditLog(env.graphQlContext, aktørId, null, graphqlError.message)
            is UgyldigInput.ParametreMangler -> {}
            is UgyldigInput.UgyldigAktørId -> {}
            is UgyldigInput.UgyldigPersonPseudoId -> {}
        }
    }
}
