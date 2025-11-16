package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import io.ktor.util.logging.error
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.feilhåndtering.FinnerIkkeLagtPåVent
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVent
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
import no.nav.helse.spesialist.domain.Saksbehandler
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
        val saksbehandler = env.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.påVent(
                ApiPaVentRequest.ApiLeggPaVent(
                    oppgaveId.toLong(),
                    saksbehandler.id.value,
                    frist,
                    tildeling,
                    notatTekst,
                    arsaker ?: emptyList(),
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
            sikkerlogg.error(e)
            byggFeilrespons(getUpdateError(oppgaveId))
        }
    }

    override fun fjernPaVent(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> {
        val saksbehandler = env.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER)
        return try {
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
    ): DataFetcherResult<ApiPaVent?> {
        val saksbehandler = env.graphQlContext.get<Saksbehandler>(SAKSBEHANDLER)
        return try {
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
            sikkerlogg.error(e)
            byggFeilrespons(getUpdateError(oppgaveId))
        }
    }

    private fun getUpdateError(oppgaveId: String): GraphQLError {
        val message = "Kunne ikke tildele oppgave med oppgaveId=$oppgaveId"
        sikkerlogg.error(message)
        return graphqlErrorException(500, message)
    }
}
