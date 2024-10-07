package no.nav.helse.db

import java.util.UUID

interface MeldingRepository {
    fun finnFødselsnummer(meldingId: UUID): String
}
