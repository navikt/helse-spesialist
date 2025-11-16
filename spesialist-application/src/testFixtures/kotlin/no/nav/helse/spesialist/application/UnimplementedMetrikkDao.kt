package no.nav.helse.spesialist.application

import no.nav.helse.db.GodkjenningsbehovUtfall
import no.nav.helse.db.MetrikkDao
import java.util.UUID

class UnimplementedMetrikkDao : MetrikkDao {
    override fun finnUtfallForGodkjenningsbehov(contextId: UUID): GodkjenningsbehovUtfall {
        TODO("Not yet implemented")
    }
}
