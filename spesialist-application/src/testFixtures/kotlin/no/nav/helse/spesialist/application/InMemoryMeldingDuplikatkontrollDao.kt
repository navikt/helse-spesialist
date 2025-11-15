package no.nav.helse.spesialist.application

import no.nav.helse.db.MeldingDuplikatkontrollDao
import java.util.UUID

class InMemoryMeldingDuplikatkontrollDao : MeldingDuplikatkontrollDao {
    override fun lagre(meldingId: UUID, type: String) {}

    override fun erBehandlet(meldingId: UUID) = false
}
