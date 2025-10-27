package no.nav.helse.db

import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.time.LocalDate

interface BehandlingRepository {
    fun finn(id: SpleisBehandlingId): Behandling?

    fun finnBehandlingerISykefraværstilfelle(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): List<Behandling>

    fun lagre(behandling: Behandling)
}
