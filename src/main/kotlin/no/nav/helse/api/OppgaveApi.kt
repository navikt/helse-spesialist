package no.nav.helse.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.dto.SaksbehandleroppgaveDto

internal fun Application.oppgaveApi(
    oppgaveDao: OppgaveDao,
    personDao: PersonDao,
    vedtakDao: VedtakDao
) {
    routing {
        authenticate {
            get("/api/oppgaver") {
                val oppgaver = oppgaveDao.findSaksbehandlerOppgaver()
                if (oppgaver == null || oppgaver.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound, "Fant ingen oppgaver")
                    return@get
                }
                val saksbehandlerOppgaver = oppgaver
                    .map { it to vedtakDao.findVedtak(it.vedtaksref!!) }
                    .map {
                        val personDto = personDao.findPerson(it.second.personRef) ?: return@map null
                        SaksbehandleroppgaveDto(
                            it.second.vedtaksperiodeId,
                            it.second.fom,
                            it.second.tom,
                            personDto.navn,
                            personDto.fødselsnummer
                        )
                    }.filterNotNull()
                call.respond(saksbehandlerOppgaver)
            }
        }
    }
}
