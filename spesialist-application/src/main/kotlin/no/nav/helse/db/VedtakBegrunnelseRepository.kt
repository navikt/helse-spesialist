package no.nav.helse.db

import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtakBegrunnelse

interface VedtakBegrunnelseRepository {
    fun finn(spleisBehandlingId: SpleisBehandlingId): VedtakBegrunnelse?

    fun lagre(vedtakBegrunnelse: VedtakBegrunnelse)
}
