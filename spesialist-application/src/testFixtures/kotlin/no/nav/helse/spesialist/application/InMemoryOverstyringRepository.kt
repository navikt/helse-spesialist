package no.nav.helse.spesialist.application

import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId

class InMemoryOverstyringRepository : OverstyringRepository {
    val data = mutableMapOf<TotrinnsvurderingId, MutableList<Overstyring>>()

    override fun lagre(
        overstyringer: List<Overstyring>,
        totrinnsvurderingId: TotrinnsvurderingId
    ) {
        val list = data.getOrPut(totrinnsvurderingId) { mutableListOf() }
        list.removeIf { it.id() in overstyringer.map(Overstyring::id).toSet() }
        list.addAll(overstyringer)
    }

    override fun finnAktive(totrinnsvurderingId: TotrinnsvurderingId): List<Overstyring> =
        data[totrinnsvurderingId].orEmpty().filter { !it.ferdigstilt }
}
