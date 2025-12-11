package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak

interface VedtakRepository {
    fun lagre(vedtak: Vedtak)

    fun finn(spleisBehandlingId: SpleisBehandlingId): Vedtak?
}
