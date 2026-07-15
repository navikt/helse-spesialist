package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.application.testfixtures.lagForsikringsvurdering
import no.nav.helse.spesialist.domain.ForsikringsvurderingId

class MockForsikringsvurderingHenter : ForsikringsvurderingHenter {
    var forsikringsvurdering = lagForsikringsvurdering()

    override fun hent(forsikringsvurderingId: ForsikringsvurderingId) = forsikringsvurdering
}
