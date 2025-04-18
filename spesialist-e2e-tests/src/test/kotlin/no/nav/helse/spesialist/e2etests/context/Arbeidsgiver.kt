package no.nav.helse.spesialist.e2etests.context

import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer

data class Arbeidsgiver(
    val organisasjonsnummer: String = lagOrganisasjonsnummer(),
    val navn: String = lagOrganisasjonsnavn()
)
