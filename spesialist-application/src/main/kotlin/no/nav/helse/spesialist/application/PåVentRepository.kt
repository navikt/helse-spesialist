package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.PåVentId

interface PåVentRepository {
    fun finnAlle(ider: Set<PåVentId>): List<PåVent>
}
