package no.nav.helse.modell.saksbehandler

import java.util.UUID

interface SaksbehandlerVisitor {
    fun visitSaksbehandler(epostadresse: String, oid: UUID, navn: String, ident: String) {}
}