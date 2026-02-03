package no.nav.helse.spesialist.api.auth

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle

data class SaksbehandlerPrincipal(
    val saksbehandler: Saksbehandler,
    val brukerroller: Set<Brukerrolle>,
)
