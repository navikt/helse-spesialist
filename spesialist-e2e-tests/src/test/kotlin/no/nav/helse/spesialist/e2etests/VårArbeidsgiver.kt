package no.nav.helse.spesialist.e2etests

import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer

data class VårArbeidsgiver(
    val organisasjonsnummer: String = lagOrganisasjonsnummer(),
    val navn: String = lagOrganisasjonsnavn()
)
