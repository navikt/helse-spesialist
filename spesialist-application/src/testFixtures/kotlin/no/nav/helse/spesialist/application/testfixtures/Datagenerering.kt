package no.nav.helse.spesialist.application.testfixtures

import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer

fun lagForsikringsvurdering(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    harForsikring: Boolean = false,
    dekning: Forsikringsvurdering.Dekning? = null,
): Forsikringsvurdering =
    Forsikringsvurdering(
        identitetsnummer = identitetsnummer,
        harForsikring = harForsikring,
        dekning = dekning,
    )
