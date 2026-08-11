package no.nav.helse.spesialist.application.testfixtures

import no.nav.helse.spesialist.application.EkskludertForsikring
import no.nav.helse.spesialist.application.Forsikring
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer

fun lagForsikringsvurdering(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    harForsikring: Boolean = false,
    dekning: Forsikringsvurdering.Dekning? = null,
    ekskluderteForsikringer: List<EkskludertForsikring> = emptyList(),
    gjeldendeForsikring: Forsikring? = null,
): Forsikringsvurdering =
    Forsikringsvurdering(
        identitetsnummer = identitetsnummer,
        harForsikring = harForsikring,
        dekning = dekning,
        ekskluderteForsikringer = ekskluderteForsikringer,
        gjeldendeForsikring = gjeldendeForsikring,
    )
