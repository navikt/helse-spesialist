package no.nav.helse.spesialist.api

import java.util.UUID
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto

interface Godkjenninghåndterer {
    fun håndter(godkjenningDTO: GodkjenningDto, epost: String, oid: UUID, behandlingId: UUID)

}

