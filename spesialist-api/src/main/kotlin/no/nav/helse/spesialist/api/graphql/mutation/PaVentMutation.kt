package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException.newErrorException
import graphql.execution.DataFetcherResult
import graphql.execution.DataFetcherResult.newResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER_OID
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.graphql.schema.PaVent
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.FjernPåVent
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LeggPåVent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class PaVentMutation(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val notatMediator: NotatMediator,
    private val periodehistorikkDao: PeriodehistorikkDao,
) : Mutation {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    @Suppress("unused")
    suspend fun leggPaVent(
        oppgaveId: String,
        notatTekst: String,
        notatType: NotatType,
        frist: LocalDate,
        tildeling: Boolean,
        begrunnelse: String?,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<PaVent?> {
        val saksbehandler = env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(ContextValues.SAKSBEHANDLER.key).value
        return withContext(Dispatchers.IO) {
            val saksbehandlerOid = UUID.fromString(env.graphQlContext.get(SAKSBEHANDLER_OID.key))

            try {
                notatMediator.lagreForOppgaveId(oppgaveId.toLong(), notatTekst, saksbehandlerOid, notatType)
                saksbehandlerhåndterer.håndter(
                    LeggPåVent(oppgaveId.toLong(), saksbehandler.oid, frist, tildeling, begrunnelse),
                    saksbehandler,
                )
                newResult<PaVent?>().data(
                    PaVent(
                        frist = frist,
                        begrunnelse = begrunnelse,
                        oid = saksbehandler.oid,
                    ),
                ).build()
            } catch (e: OppgaveIkkeTildelt) {
                newResult<PaVent?>().error(ikkeTildeltError(e)).build()
            } catch (e: OppgaveTildeltNoenAndre) {
                newResult<PaVent?>().error(tildeltNoenAndreError(e)).build()
            } catch (e: RuntimeException) {
                newResult<PaVent?>().error(getUpdateError(oppgaveId)).build()
            }
        }
    }

    @Suppress("unused")
    suspend fun fjernPaVent(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> {
        val saksbehandler = env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(ContextValues.SAKSBEHANDLER.key).value
        return withContext(Dispatchers.IO) {
            try {
                periodehistorikkDao.lagre(PeriodehistorikkType.FJERN_FRA_PA_VENT, saksbehandler.oid, oppgaveId.toLong(), null)
                saksbehandlerhåndterer.håndter(FjernPåVent(oppgaveId.toLong()), saksbehandler)
                newResult<Boolean?>().data(true).build()
            } catch (e: OppgaveIkkeTildelt) {
                newResult<Boolean>().data(false).build()
            } catch (e: OppgaveTildeltNoenAndre) {
                newResult<Boolean>().data(false).build()
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

    private fun tildeltNoenAndreError(error: OppgaveTildeltNoenAndre): GraphQLError {
        return newErrorException()
            .message("Oppgave tildelt noen andre")
            .extensions(mapOf("code" to error.httpkode, "tildeling" to error.tildeling))
            .build()
    }

    private fun ikkeTildeltError(error: OppgaveIkkeTildelt): GraphQLError {
        return newErrorException()
            .message("Oppgave ikke tildelt")
            .extensions(mapOf("code" to error.httpkode))
            .build()
    }
}
