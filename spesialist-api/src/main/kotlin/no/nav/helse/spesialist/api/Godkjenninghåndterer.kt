package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import java.util.UUID

interface Godkjenninghåndterer {
    fun håndter(
        godkjenningDTO: GodkjenningDto,
        epost: String,
        oid: UUID,
    )
}
