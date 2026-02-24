package no.nav.helse.spesialist.application

import kotlinx.coroutines.flow.Flow
import no.nav.helse.spesialist.domain.Identitetsnummer

interface OpptegnelseListener : AutoCloseable {
    fun notifications(identitetsnummer: Identitetsnummer): Flow<Unit>
}
