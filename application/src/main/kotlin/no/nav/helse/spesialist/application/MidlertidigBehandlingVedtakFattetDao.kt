package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.SpleisBehandlingId

interface MidlertidigBehandlingVedtakFattetDao {
    fun vedtakFattet(spleisBehandlingId: SpleisBehandlingId)

    fun erVedtakFattet(spleisBehandlingId: SpleisBehandlingId): Boolean
}
