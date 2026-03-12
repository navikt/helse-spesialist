package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.util.UUID

interface NotatRepository {
    fun lagre(notat: Notat)

    fun finn(id: NotatId): Notat?

    fun finnAlleForVedtaksperiode(vedtaksperiodeId: UUID): List<Notat>

    fun finnAlleForVedtaksperioder(vedtaksperiodeIds: Set<VedtaksperiodeId>): List<Notat>
}
