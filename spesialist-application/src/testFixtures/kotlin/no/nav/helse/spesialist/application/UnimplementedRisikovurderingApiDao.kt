package no.nav.helse.spesialist.application

import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import java.util.UUID

class UnimplementedRisikovurderingApiDao : RisikovurderingApiDao {
    override fun finnRisikovurderinger(fødselsnummer: String): Map<UUID, RisikovurderingApiDto> {
        TODO("Not yet implemented")
    }
}
