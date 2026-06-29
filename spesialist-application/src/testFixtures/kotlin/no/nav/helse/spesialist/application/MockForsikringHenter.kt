package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Forsikring
import no.nav.helse.spesialist.domain.Forsikringsvurdering
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer

class MockForsikringHenter : ForsikringHenter {
    var forsikring = Forsikring(17, 100)

    override fun hentForsikringsinformasjon(spleisBehandlingId: SpleisBehandlingId): ResultatAvForsikring =
        ResultatAvForsikring.MottattForsikring(forsikring)

    override fun hentForsikringsvurdering(forsikringsvurderingId: ForsikringsvurderingId): Forsikringsvurdering? =
        Forsikringsvurdering(
            identitetsnummer = lagIdentitetsnummer(),
            harForsikring = true,
            dekning = Forsikringsvurdering.Dekning(
                grad = 100,
                fraDag = 17
            )
        )
}
