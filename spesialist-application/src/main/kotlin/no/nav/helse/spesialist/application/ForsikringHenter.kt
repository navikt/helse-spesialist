package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Forsikringsvurdering
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId

interface ForsikringHenter {
    fun hentForsikringsinformasjon(spleisBehandlingId: SpleisBehandlingId): ResultatAvForsikring
    fun hentForsikringsvurdering(forsikringsvurderingId: ForsikringsvurderingId): Forsikringsvurdering?
}
