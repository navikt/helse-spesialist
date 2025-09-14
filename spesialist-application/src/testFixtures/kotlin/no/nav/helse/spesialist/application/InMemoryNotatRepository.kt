package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import java.util.UUID

class InMemoryNotatRepository : NotatRepository {
    private val data = mutableMapOf<NotatId, Notat>()

    override fun lagre(notat: Notat) {
        if (!notat.harFåttTildeltId()) {
            notat.tildelId(NotatId((data.keys.maxOfOrNull(NotatId::value) ?: 0) + 1))
        }
        data[notat.id()] = notat
    }

    override fun finn(id: NotatId): Notat? =
        data[id]

    override fun finnAlleForVedtaksperiode(vedtaksperiodeId: UUID): List<Notat> =
        data.values.filter { it.vedtaksperiodeId == vedtaksperiodeId }
}
