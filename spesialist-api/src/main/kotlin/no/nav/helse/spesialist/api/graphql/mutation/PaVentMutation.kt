package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.http.HttpStatusCode
import io.ktor.util.logging.error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.feilhåndtering.FinnerIkkeLagtPåVent
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.PaVent
import no.nav.helse.spesialist.api.graphql.schema.PaVentRequest
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PaVentMutation(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
) : Mutation {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    @Suppress("unused")
    suspend fun leggPaVent(
        oppgaveId: String,
        notatTekst: String?,
        frist: LocalDate,
        tildeling: Boolean,
        arsaker: List<PaVentRequest.PaVentArsak>? = emptyList(),
        env: DataFetchingEnvironment,
    ): DataFetcherResult<PaVent?> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        return withContext(Dispatchers.IO) {
            try {
                saksbehandlerhåndterer.påVent(
                    PaVentRequest.LeggPaVent(
                        oppgaveId.toLong(),
                        saksbehandler.oid,
                        frist,
                        tildeling,
                        notatTekst,
                        arsaker ?: emptyList(),
                    ),
                    saksbehandler,
                )
                newResult<PaVent?>()
                    .data(
                        PaVent(
                            frist = frist,
                            oid = saksbehandler.oid,
                        ),
                    ).build()
            } catch (e: OppgaveIkkeTildelt) {
                newResult<PaVent?>().error(ikkeTildeltError(e)).build()
            } catch (e: OppgaveTildeltNoenAndre) {
                newResult<PaVent?>().error(tildeltNoenAndreError(e)).build()
            } catch (e: RuntimeException) {
                sikkerlogg.error(e)
                newResult<PaVent?>().error(getUpdateError(oppgaveId)).build()
            }
        }
    }

    @Suppress("unused")
    suspend fun fjernPaVent(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        return withContext(Dispatchers.IO) {
            try {
                saksbehandlerhåndterer.påVent(PaVentRequest.FjernPaVent(oppgaveId.toLong()), saksbehandler)
                newResult<Boolean?>().data(true).build()
            } catch (e: OppgaveIkkeTildelt) {
                e.logger()
                newResult<Boolean>().data(false).build()
            } catch (e: OppgaveTildeltNoenAndre) {
                e.logger()
                newResult<Boolean>().data(false).build()
            }
        }
    }

    @Suppress("unused")
    suspend fun endrePaVent(
        oppgaveId: String,
        notatTekst: String?,
        frist: LocalDate,
        tildeling: Boolean,
        arsaker: List<PaVentRequest.PaVentArsak>,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<PaVent?> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        return withContext(Dispatchers.IO) {
            try {
                saksbehandlerhåndterer.påVent(
                    PaVentRequest.EndrePaVent(
                        oppgaveId = oppgaveId.toLong(),
                        saksbehandlerOid = saksbehandler.oid,
                        frist = frist,
                        skalTildeles = tildeling,
                        notatTekst = notatTekst,
                        årsaker = arsaker,
                    ),
                    saksbehandler,
                )
                newResult<PaVent?>()
                    .data(
                        PaVent(
                            frist = frist,
                            oid = saksbehandler.oid,
                        ),
                    ).build()
            } catch (e: FinnerIkkeLagtPåVent) {
                e.logger()
                newResult<PaVent>().error(getUpdateError(oppgaveId)).build()
            } catch (e: RuntimeException) {
                sikkerlogg.error(e)
                newResult<PaVent?>().error(getUpdateError(oppgaveId)).build()
            }
        }
    }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        sikkerlogg.error(message)
        return newErrorException()
            .message(message)
            .extensions(mapOf("code" to HttpStatusCode.InternalServerError))
            .build()
    }

    private fun tildeltNoenAndreError(error: OppgaveTildeltNoenAndre): GraphQLError =
        newErrorException()
            .message("Oppgave tildelt noen andre")
            .extensions(mapOf("code" to error.httpkode, "tildeling" to error.tildeling))
            .build()

    private fun ikkeTildeltError(error: OppgaveIkkeTildelt): GraphQLError =
        newErrorException()
            .message("Oppgave ikke tildelt")
            .extensions(mapOf("code" to error.httpkode))
            .build()
}
