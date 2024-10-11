package no.nav.helse.spesialist.api.graphql.query

import graphql.GraphQLContext
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.auditLogTeller
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.graphql.forbiddenError
import no.nav.helse.spesialist.api.graphql.notFoundError
import no.nav.helse.spesialist.api.graphql.personNotReadyError
import no.nav.helse.spesialist.api.graphql.query.Inputvalidering.UgyldigInput
import no.nav.helse.spesialist.api.graphql.schema.Person
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private sealed interface Inputvalidering {
    class Ok(val fødselsnummer: String) : Inputvalidering

    sealed class UgyldigInput(val graphqlError: GraphQLError) : Inputvalidering {
        class UkjentFødselsnummer(val fødselsnummer: String, graphqlError: GraphQLError) : UgyldigInput(graphqlError)

        class UkjentAktørId(val aktørId: String, graphqlError: GraphQLError) : UgyldigInput(graphqlError)

        class ParametreMangler(graphqlError: GraphQLError) : UgyldigInput(graphqlError)

        class UgyldigAktørId(graphqlError: GraphQLError) : UgyldigInput(graphqlError)

        class HarFlereFødselsnumre(val aktørId: String, graphqlError: GraphQLError) : UgyldigInput(graphqlError)
    }
}

interface PersonoppslagService {
    suspend fun hentPerson(
        fødselsnummer: String,
        tilganger: SaksbehandlerTilganger,
    ): FetchPersonResult

    fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean

    fun fødselsnumreKnyttetTil(aktørId: String): Set<String>
}

sealed interface FetchPersonResult {
    class Ok(val person: Person) : FetchPersonResult

    sealed interface Feil : FetchPersonResult {
        class IkkeKlarTilVisning(val aktørId: String, val personKlargjøres: Boolean) : Feil

        data object ManglerTilgang : Feil

        data object IkkeFunnet : Feil

        data object UgyldigSnapshot : Feil
    }
}

class PersonQuery(
    private val personoppslagService: PersonoppslagService,
) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    private val auditLog = LoggerFactory.getLogger("auditLogger")

    private companion object {
        private const val GYLDIG_AKTØRID_LENDGE = 13
    }

    suspend fun person(
        fnr: String? = null,
        aktorId: String? = null,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Person?> {
        val fødselsnummer =
            when (val validering = validerInput(fnr, aktorId)) {
                is Inputvalidering.Ok -> validering.fødselsnummer
                is UgyldigInput -> {
                    validering.auditlogg(env)
                    return validering.graphqlError.tilGraphqlResult()
                }
            }
        sikkerLogg.info("Personoppslag på fnr=$fødselsnummer")

        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>(TILGANGER)

        return when (val result = personoppslagService.hentPerson(fødselsnummer, tilganger)) {
            is FetchPersonResult.Feil -> {
                result.auditlogg(env, fødselsnummer)
                result.tilGraphqlError(fødselsnummer)
            }
            is FetchPersonResult.Ok -> {
                auditLog(env.graphQlContext, fødselsnummer, true, null)
                DataFetcherResult.newResult<Person?>().data(result.person).build()
            }
        }
    }

    private fun FetchPersonResult.Feil.auditlogg(
        env: DataFetchingEnvironment,
        fødselsnummer: String,
    ) {
        when (this) {
            is FetchPersonResult.Feil.IkkeFunnet -> auditLog(env.graphQlContext, fødselsnummer, true, notFoundError(fødselsnummer).message)
            is FetchPersonResult.Feil.IkkeKlarTilVisning -> auditLog(env.graphQlContext, fødselsnummer, false, null)
            is FetchPersonResult.Feil.ManglerTilgang -> auditLog(env.graphQlContext, fødselsnummer, false, null)
            is FetchPersonResult.Feil.UgyldigSnapshot ->
                auditLog(
                    env.graphQlContext,
                    fødselsnummer,
                    null,
                    getSnapshotValidationError().message,
                )
        }
    }

    private fun FetchPersonResult.Feil.tilGraphqlError(fødselsnummer: String): DataFetcherResult<Person?> {
        val graphqlError =
            when (this) {
                is FetchPersonResult.Feil.IkkeFunnet -> notFoundError(fødselsnummer)
                is FetchPersonResult.Feil.IkkeKlarTilVisning -> personNotReadyError(fødselsnummer, aktørId, personKlargjøres)
                is FetchPersonResult.Feil.ManglerTilgang -> forbiddenError(fødselsnummer)
                is FetchPersonResult.Feil.UgyldigSnapshot -> getSnapshotValidationError()
            }

        return graphqlError.tilGraphqlResult()
    }

    private fun Set<String>.harFlereFødselsnumre() = this.size > 1

    private fun Set<String>.harIngenFødselsnumre() = this.isEmpty()

    private fun ugyldigAktørId(aktørId: String) = aktørId.length != GYLDIG_AKTØRID_LENDGE

    private fun validerInput(
        fødselsnummer: String?,
        aktørId: String?,
    ): Inputvalidering {
        if (fødselsnummer != null) {
            if (personoppslagService.finnesPersonMedFødselsnummer(fødselsnummer)) return Inputvalidering.Ok(fødselsnummer)
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
        if (fødselsnumre.harFlereFødselsnumre()) return UgyldigInput.HarFlereFødselsnumre(aktørId, getFlereFødselsnumreError(fødselsnumre))

        return Inputvalidering.Ok(fødselsnumre.single())
    }

    private fun GraphQLError.tilGraphqlResult(): DataFetcherResult<Person?> = DataFetcherResult.newResult<Person?>().error(this).build()

    private fun loggNotFoundForAktørId(
        aktorId: String,
        env: DataFetchingEnvironment,
    ) {
        sikkerLogg.info("Svarer not found for parametere aktorId=$aktorId.")
        auditLog(env.graphQlContext, aktorId, null, notFoundError(aktorId).message)
    }

    private fun loggNotFoundForFødselsnummer(
        fnr: String,
        env: DataFetchingEnvironment,
    ) {
        sikkerLogg.info("Svarer not found for parametere fnr=$fnr.")
        auditLog(env.graphQlContext, fnr, null, notFoundError(fnr).message)
    }

    private fun getFlereFødselsnumreError(fødselsnumre: Set<String>): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message("Mer enn ett fødselsnummer for personen")
            .extensions(
                mapOf(
                    "code" to 500,
                    "feilkode" to "HarFlereFodselsnumre",
                    "fodselsnumre" to fødselsnumre,
                ),
            ).build()

    private fun getSnapshotValidationError(): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message(
                "Lagret snapshot stemmer ikke overens med forventet format. Dette kommer som regel av at noen har gjort endringer på formatet men glemt å bumpe versjonsnummeret.",
            ).extensions(mapOf("code" to 501, "field" to "person"))
            .build()

    private fun getBadRequestError(melding: String): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message(melding)
            .extensions(mapOf("code" to 400))
            .build()

    private fun auditLog(
        graphQLContext: GraphQLContext,
        personId: String,
        harTilgang: Boolean?,
        fantIkkePersonErrorMsg: String?,
    ) {
        val saksbehandlerIdent = graphQLContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).ident
        auditLogTeller.inc()

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
            is UgyldigInput.HarFlereFødselsnumre -> auditLog(env.graphQlContext, aktørId, null, graphqlError.message)
            is UgyldigInput.ParametreMangler -> {}
            is UgyldigInput.UgyldigAktørId -> {}
        }
    }
}
