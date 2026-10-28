package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserEvent
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId

class InMemoryGraderteAndreYtelserRepository : GraderteAndreYtelserRepository {
    private val eventsPerStream = mutableMapOf<GraderteAndreYtelserId, List<GraderteAndreYtelserEvent>>()

    override fun finnAlleForIdentitetsnummer(identitetsnummer: Identitetsnummer): List<GraderteAndreYtelser> =
        eventsPerStream
            .filterKeys { it.identitetsnummer == identitetsnummer }
            .map { (id, events) -> GraderteAndreYtelser.fraLagring(id, events) }

    override fun finnOrNull(id: GraderteAndreYtelserId): GraderteAndreYtelser? = eventsPerStream[id]?.let { GraderteAndreYtelser.fraLagring(id, it) }

    override fun lagre(graderteAndreYtelser: GraderteAndreYtelser) {
        val lagrede = eventsPerStream[graderteAndreYtelser.id].orEmpty()
        val lagredeVersjoner = lagrede.map { it.versjon }.toSet()
        eventsPerStream[graderteAndreYtelser.id] = lagrede + graderteAndreYtelser.events.filterNot { it.versjon in lagredeVersjoner }
    }
}
