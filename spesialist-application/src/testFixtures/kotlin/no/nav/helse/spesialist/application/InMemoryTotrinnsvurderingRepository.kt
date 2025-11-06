package no.nav.helse.spesialist.application

import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId

class InMemoryTotrinnsvurderingRepository : TotrinnsvurderingRepository {
    val data = mutableMapOf<TotrinnsvurderingId, Totrinnsvurdering>()

    override fun lagre(totrinnsvurdering: Totrinnsvurdering) {
        if (!totrinnsvurdering.harFåttTildeltId()) {
            totrinnsvurdering.tildelId(
                TotrinnsvurderingId(
                    (data.keys.maxOfOrNull(TotrinnsvurderingId::value) ?: 0) + 1
                )
            )
        }
        data[totrinnsvurdering.id()] = totrinnsvurdering
    }

    override fun finn(id: TotrinnsvurderingId) = data[id]

    override fun finnAktivForPerson(fødselsnummer: String) = data.values.find { it.fødselsnummer == fødselsnummer }
}
