package no.nav.helse.spesialist.api.sse

import io.github.smiley4.ktoropenapi.documentation
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.routing.Route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import kotlinx.coroutines.delay
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.domain.Opptegnelse
import kotlin.time.Duration.Companion.seconds

internal fun Route.sse(sessionFactory: SessionFactory) {
    documentation({
        description = "Operasjon for Server Sent Events. NB: Gir en strøm av elementer." +
            " Ikke ment for bruk som normal GET-operasjon med f. eks. autogenerert Tanstack Query-hook!"
        tags = setOf("Events")
        request {
            pathParameter<String>("personPseudoId")
        }
        response {
            default {
                description = "En strøm med Server Sent Events."
                body<ApiServerSentEvent>()
            }
        }
    }) {
        sse("/personer/{personPseudoId}/sse") {
            heartbeat {
                period = 10.seconds
            }

            val personPseudoId =
                call.parameters["personPseudoId"]
                    ?.let { PersonPseudoId.fraString(it) }
                    ?: throw BadRequestException("Mangler påkrevd query param: personPseudoId")

            val (identitetsnummer, sisteSekvensnummerVedInitiering) =
                sessionFactory.transactionalSessionScope {
                    val identitetsnummer =
                        it.personPseudoIdDao.hentIdentitetsnummer(personPseudoId)
                            ?: throw SseException.PersonIkkeFunnet("Fant ikke person med pseudoId: $personPseudoId")
                    val sisteSekvensnummer = it.opptegnelseRepository.finnNyesteSekvensnummer()
                    identitetsnummer to sisteSekvensnummer
                }
            loggDebug("SSE-tilkobling startet", "identitetsnummer" to identitetsnummer.value)
            var sisteSekvensnummer = sisteSekvensnummerVedInitiering

            while (true) {
                val opptegnelser =
                    sessionFactory.transactionalSessionScope {
                        it.opptegnelseRepository.finnAlleForPersonEtter(sisteSekvensnummer, identitetsnummer)
                    }
                if (opptegnelser.isNotEmpty()) {
                    sisteSekvensnummer = opptegnelser.map { it.id() }.maxBy { it.value }
                }
                opptegnelser.forEach { send(event = it.type.tilEvent(), data = "{}") }
                delay(100)
            }
        }
    }
}

private fun Opptegnelse.Type.tilEvent(): String =
    when (this) {
        Opptegnelse.Type.UTBETALING_ANNULLERING_FEILET -> "UTBETALING_ANNULLERING_FEILET"
        Opptegnelse.Type.UTBETALING_ANNULLERING_OK -> "UTBETALING_ANNULLERING_OK"
        Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV -> "FERDIGBEHANDLET_GODKJENNINGSBEHOV"
        Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE -> "NY_SAKSBEHANDLEROPPGAVE"
        Opptegnelse.Type.REVURDERING_AVVIST -> "REVURDERING_AVVIST"
        Opptegnelse.Type.REVURDERING_FERDIGBEHANDLET -> "REVURDERING_FERDIGBEHANDLET"
        Opptegnelse.Type.PERSONDATA_OPPDATERT -> "PERSONDATA_OPPDATERT"
        Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING -> "PERSON_KLAR_TIL_BEHANDLING"
    }
