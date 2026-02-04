package no.nav.helse.spesialist.api.auth

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

data class SaksbehandlerPrincipal(
    val saksbehandler: Saksbehandler,
    val brukerroller: Set<Brukerrolle>,
    val tilganger: Set<Tilgang>,
)
