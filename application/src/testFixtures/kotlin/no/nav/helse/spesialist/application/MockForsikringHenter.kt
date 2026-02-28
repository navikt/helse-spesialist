package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Forsikring
import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class MockForsikringHenter : ForsikringHenter {
    var forsikring = Forsikring(17, 100)
    override fun hentForsikringsinformasjon(spleisBehandlingId: SpleisBehandlingId): ResultatAvForsikring =
        ResultatAvForsikring.MottattForsikring(forsikring)
}