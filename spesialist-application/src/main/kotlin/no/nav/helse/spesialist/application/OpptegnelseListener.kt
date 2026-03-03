package no.nav.helse.spesialist.application

import kotlinx.coroutines.flow.Flow
import no.nav.helse.spesialist.domain.Identitetsnummer

interface OpptegnelseListener : AutoCloseable {
    fun endringer(identitetsnummer: Identitetsnummer): Flow<Unit>
}
