package no.nav.helse.mediator.api

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.SaksbehandlerTilganger
import no.nav.helse.getGrupper
import no.nav.helse.getNAVident
import no.nav.helse.oppgave.OppgaveDto
import no.nav.helse.oppgave.OppgaveMediator
import org.slf4j.LoggerFactory

internal fun Route.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    riskSupersaksbehandlergruppe: UUID,
    kode7Saksbehandlergruppe: UUID
) {
    val log = LoggerFactory.getLogger("OppgaveApi")

    fun loggDebuginfoOmSaker(oppgaver: List<OppgaveDto>, ident: String) {
        val antallSakerByType = oppgaver.fold(mutableMapOf<String, Int>()) { acc, oppgave ->
            acc.apply { merge("${oppgave.oppgavetype} - ${oppgave.type}", 1, Int::plus) }
        }

        log.info(
            """
Saker til $ident:
  ${
                antallSakerByType.map { it.key to it.value }.joinToString("\n  ")
            }
""".trimIndent()
        )
    }

    get("/api/oppgaver") {
        val saksbehandlerOppgaver = withContext(Dispatchers.IO) {
            oppgaveMediator.hentOppgaver(
                saksbehandlerTilganger = SaksbehandlerTilganger(
                    gruppetilganger = getGrupper(),
                    kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
                    riskSaksbehandlergruppe = riskSupersaksbehandlergruppe,
                    NAVident = getNAVident()
                )
            )
        }
        if (getNAVident() == "F131883" || getNAVident() == "E156407")
            loggDebuginfoOmSaker(saksbehandlerOppgaver, getNAVident())
        log.info("Returnerer ${saksbehandlerOppgaver.size} oppgaver til ${getNAVident()}")
        call.respond(saksbehandlerOppgaver)
    }
}

