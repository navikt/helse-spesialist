package no.nav.helse.spesialist.bootstrap.context

import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer

data class Arbeidsgiver(
    val organisasjonsnummer: String = lagOrganisasjonsnummer(),
    val navn: String = lagOrganisasjonsnavn(),
)
