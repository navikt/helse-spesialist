package no.nav.helse.db

import java.util.UUID

interface MeldingDuplikatkontrollDao {
    fun lagre(
        meldingId: UUID,
        type: String,
    )

    fun erBehandlet(meldingId: UUID): Boolean
}
