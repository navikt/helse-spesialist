package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.http.HttpStatusCode
import io.ktor.util.logging.error
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.feilhåndtering.FinnerIkkeLagtPåVent
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PaVentMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : PaVentMutationSchema {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun leggPaVent(
        oppgaveId: String,
        notatTekst: String?,
        frist: LocalDate,
        tildeling: Boolean,
        arsaker: List<ApiPaVentRequest.ApiPaVentArsak>?,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPaVent?> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.påVent(
                ApiPaVentRequest.ApiLeggPaVent(
                    oppgaveId.toLong(),
                    saksbehandler.oid,
                    frist,
                    tildeling,
                    notatTekst,
                    arsaker ?: emptyList(),
                ),
                saksbehandler,
            )
            newResult<ApiPaVent?>().data(ApiPaVent(frist = frist, oid = saksbehandler.oid)).build()
        } catch (e: OppgaveIkkeTildelt) {
            newResult<ApiPaVent?>().error(graphqlErrorException("Oppgave ikke tildelt", e.httpkode)).build()
        } catch (e: OppgaveTildeltNoenAndre) {
            newResult<ApiPaVent?>().error(
                graphqlErrorException("Oppgave tildelt noen andre", e.httpkode, "tildeling" to e.tildeling),
            ).build()
        } catch (e: RuntimeException) {
            sikkerlogg.error(e)
            newResult<ApiPaVent?>().error(getUpdateError(oppgaveId)).build()
        }
    }

    override fun fjernPaVent(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.påVent(ApiPaVentRequest.ApiFjernPaVent(oppgaveId.toLong()), saksbehandler)
            newResult<Boolean?>().data(true).build()
        } catch (e: OppgaveIkkeTildelt) {
            e.logger()
            newResult<Boolean>().data(false).build()
        } catch (e: OppgaveTildeltNoenAndre) {
            e.logger()
            newResult<Boolean>().data(false).build()
        }
    }

    override fun endrePaVent(
        oppgaveId: String,
        notatTekst: String?,
        frist: LocalDate,
        tildeling: Boolean,
        arsaker: List<ApiPaVentRequest.ApiPaVentArsak>,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPaVent?> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.påVent(
                ApiPaVentRequest.ApiEndrePaVent(
                    oppgaveId = oppgaveId.toLong(),
                    saksbehandlerOid = saksbehandler.oid,
                    frist = frist,
                    skalTildeles = tildeling,
                    notatTekst = notatTekst,
                    årsaker = arsaker,
                ),
                saksbehandler,
            )
            newResult<ApiPaVent?>().data(ApiPaVent(frist = frist, oid = saksbehandler.oid)).build()
        } catch (e: FinnerIkkeLagtPåVent) {
            e.logger()
            newResult<ApiPaVent>().error(getUpdateError(oppgaveId)).build()
        } catch (e: RuntimeException) {
            sikkerlogg.error(e)
            newResult<ApiPaVent?>().error(getUpdateError(oppgaveId)).build()
        }
    }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        sikkerlogg.error(message)
        return graphqlErrorException(message, HttpStatusCode.InternalServerError)
    }

    private fun graphqlErrorException(
        message: String,
        statusCode: HttpStatusCode,
        vararg additionalExtensions: Pair<String, Any>,
    ) = newErrorException()
        .message(message)
        .extensions(mapOf("code" to statusCode.value, *additionalExtensions))
        .build()
}
