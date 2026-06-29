package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.ForsikringsvurderingId

fun interface ForsikringsvurderingHenter {
    fun hent(forsikringsvurderingId: ForsikringsvurderingId): Forsikringsvurdering?
}
