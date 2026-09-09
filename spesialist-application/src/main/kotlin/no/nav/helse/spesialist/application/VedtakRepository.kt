package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak

interface VedtakRepository {
    fun lagre(vedtak: Vedtak)

    fun finnOrNull(spleisBehandlingId: SpleisBehandlingId): Vedtak?

    fun slett(spleisBehandlingId: SpleisBehandlingId)
}
