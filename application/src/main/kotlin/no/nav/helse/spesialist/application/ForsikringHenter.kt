package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId

fun interface ForsikringHenter {
    fun hentForsikringsinformasjon(spleisBehandlingId: SpleisBehandlingId): ResultatAvForsikring
}
