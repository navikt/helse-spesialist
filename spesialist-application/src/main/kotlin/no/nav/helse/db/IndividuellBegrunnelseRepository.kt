package no.nav.helse.db

import no.nav.helse.spesialist.domain.IndividuellBegrunnelse
import no.nav.helse.spesialist.domain.SpleisBehandlingId

interface IndividuellBegrunnelseRepository {
    fun finnOrNull(spleisBehandlingId: SpleisBehandlingId): IndividuellBegrunnelse?

    fun lagre(individuellBegrunnelse: IndividuellBegrunnelse)
}
