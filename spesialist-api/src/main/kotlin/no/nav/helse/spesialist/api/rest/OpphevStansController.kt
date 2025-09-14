package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.domain.NotatType

class OpphevStansController(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) {
    data class Parameters(
        val fødselsnummer: String,
        val begrunnelse: String,
    )

    fun addToRoute(route: Route) {
        route.post("/opphevstans") {
            val parameters = call.receive<Parameters>()
            saksbehandlerMediator.utførHandling("opphev_stans", call) { saksbehandler, _, tx ->
                val fodselsnummer = parameters.fødselsnummer
                val begrunnelse = parameters.begrunnelse
                tx.stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = fodselsnummer)
                tx.notatDao.lagreForOppgaveId(
                    oppgaveId =
                        tx.oppgaveDao.finnOppgaveId(fødselsnummer = fodselsnummer)
                            ?: tx.oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer = fodselsnummer),
                    tekst = begrunnelse,
                    saksbehandlerOid = saksbehandler.id().value,
                    notatType = NotatType.OpphevStans,
                    dialogRef = tx.dialogDao.lagre(),
                )
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
