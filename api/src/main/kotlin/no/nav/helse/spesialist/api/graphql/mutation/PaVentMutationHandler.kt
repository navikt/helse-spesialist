package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.util.logging.error
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.FinnerIkkeLagtPåVent
import no.nav.helse.spesialist.api.graphql.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.graphql.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
import no.nav.helse.spesialist.application.logg.kanskjeMedMdc
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Saksbehandler
import java.time.LocalDate

class PaVentMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : PaVentMutationSchema {
    override fun leggPaVent(
        oppgaveId: String,
        notatTekst: String?,
        frist: LocalDate,
        tildeling: Boolean,
        arsaker: List<ApiPaVentRequest.ApiPaVentArsak>?,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPaVent?> =
        kanskjeMedMdc(saksbehandlerMediator.finnMdcParametreForOppgaveId(oppgaveId)) {
            val saksbehandler = env.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER)
            try {
                saksbehandlerMediator.påVent(
                    ApiPaVentRequest.ApiLeggPaVent(
                        oppgaveId = oppgaveId.toLong(),
                        saksbehandlerOid = saksbehandler.id.value,
                        frist = frist,
                        skalTildeles = tildeling,
                        notatTekst = notatTekst,
                        årsaker = arsaker ?: emptyList(),
                    ),
                    saksbehandler,
                )
                byggRespons(ApiPaVent(frist = frist, oid = saksbehandler.id.value))
            } catch (e: OppgaveIkkeTildelt) {
                byggFeilrespons(graphqlErrorException(e.httpkode.value, "Oppgave ikke tildelt"))
            } catch (e: OppgaveTildeltNoenAndre) {
                byggFeilrespons(
                    graphqlErrorException(e.httpkode.value, "Oppgave tildelt noen andre", "tildeling" to e.tildeling),
                )
            } catch (e: Exception) {
                teamLogs.error(e)
                byggFeilrespons(getUpdateError(oppgaveId))
            }
        }

    override fun fjernPaVent(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> =
        kanskjeMedMdc(saksbehandlerMediator.finnMdcParametreForOppgaveId(oppgaveId)) {
            val saksbehandler = env.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER)
            try {
                saksbehandlerMediator.påVent(ApiPaVentRequest.ApiFjernPaVent(oppgaveId.toLong()), saksbehandler)
                byggRespons(true)
            } catch (e: OppgaveIkkeTildelt) {
                e.logger()
                byggRespons(false)
            } catch (e: OppgaveTildeltNoenAndre) {
                e.logger()
                byggRespons(false)
            }
        }

    override fun endrePaVent(
        oppgaveId: String,
        notatTekst: String?,
        frist: LocalDate,
        tildeling: Boolean,
        arsaker: List<ApiPaVentRequest.ApiPaVentArsak>,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPaVent?> =
        kanskjeMedMdc(saksbehandlerMediator.finnMdcParametreForOppgaveId(oppgaveId)) {
            val saksbehandler = env.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER)
            try {
                saksbehandlerMediator.påVent(
                    ApiPaVentRequest.ApiEndrePaVent(
                        oppgaveId = oppgaveId.toLong(),
                        saksbehandlerOid = saksbehandler.id.value,
                        frist = frist,
                        skalTildeles = tildeling,
                        notatTekst = notatTekst,
                        årsaker = arsaker,
                    ),
                    saksbehandler,
                )
                byggRespons(ApiPaVent(frist = frist, oid = saksbehandler.id.value))
            } catch (e: FinnerIkkeLagtPåVent) {
                e.logger()
                byggFeilrespons(getUpdateError(oppgaveId))
            } catch (e: Exception) {
                teamLogs.error(e)
                byggFeilrespons(getUpdateError(oppgaveId))
            }
        }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        teamLogs.error(message)
        return graphqlErrorException(500, message)
    }
}
