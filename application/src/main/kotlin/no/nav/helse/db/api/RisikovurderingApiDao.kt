package no.nav.helse.db.api

import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import java.util.UUID

interface RisikovurderingApiDao {
    fun finnRisikovurderinger(fødselsnummer: String): Map<UUID, RisikovurderingApiDto>
}
