package no.nav.helse.spesialist.api.rest

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.routing.Route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.send
import io.ktor.server.sse.sse
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Sekvensnummer
import kotlin.time.Duration.Companion.seconds

internal fun Route.sse(sessionFactory: SessionFactory) {
    sse("/personer/{personPseudoId}/opptegnelser-stream", serialize = { typeInfo, it ->
        val serializer = Json.serializersModule.serializer(typeInfo.kotlinType!!)
        Json.encodeToString(serializer, it)
    }) {
        heartbeat {
            period = 10.seconds
        }
        val personPseudoId =
            call.parameters["personPseudoId"]
                ?.let { PersonPseudoId.fraString(it) }
                ?: throw BadRequestException("Missing required query param: personPseudoId")

        lateinit var identitetsnummer: Identitetsnummer
        lateinit var sisteSekvensnummer: Sekvensnummer

        sessionFactory.transactionalSessionScope {
            identitetsnummer = it.personPseudoIdDao.hentIdentitetsnummer(personPseudoId)
                ?: throw NotFoundException("Fant ikke person med personPseudoId: $personPseudoId")
            sisteSekvensnummer = it.opptegnelseRepository.finnNyesteSekvensnummer()
        }

        while (true) {
            val opptegnelser =
                sessionFactory.transactionalSessionScope {
                    it.opptegnelseRepository.finnAlleForPersonEtter(sisteSekvensnummer, identitetsnummer)
                }
            if (opptegnelser.isNotEmpty()) {
                sisteSekvensnummer = opptegnelser.map { it.id() }.maxBy { it.value }
            }
            opptegnelser.forEach { send(it.tilApiOpptegnelse()) }
            delay(100)
        }
    }
}

private fun Opptegnelse.tilApiOpptegnelse() =
    ApiOpptegnelse(
        sekvensnummer = id().value,
        type =
            when (type) {
                Opptegnelse.Type.UTBETALING_ANNULLERING_FEILET -> ApiOpptegnelse.Type.UTBETALING_ANNULLERING_FEILET
                Opptegnelse.Type.UTBETALING_ANNULLERING_OK -> ApiOpptegnelse.Type.UTBETALING_ANNULLERING_OK
                Opptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV -> ApiOpptegnelse.Type.FERDIGBEHANDLET_GODKJENNINGSBEHOV
                Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE -> ApiOpptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE
                Opptegnelse.Type.REVURDERING_AVVIST -> ApiOpptegnelse.Type.REVURDERING_AVVIST
                Opptegnelse.Type.REVURDERING_FERDIGBEHANDLET -> ApiOpptegnelse.Type.REVURDERING_FERDIGBEHANDLET
                Opptegnelse.Type.PERSONDATA_OPPDATERT -> ApiOpptegnelse.Type.PERSONDATA_OPPDATERT
                Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING -> ApiOpptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING
            },
    )
