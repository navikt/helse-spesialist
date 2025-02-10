package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.util.UUID

interface Overstyring : Personhandling {
    val saksbehandler: Saksbehandler
    val eksternHendelseId: UUID
    val aktørId: String
    val vedtaksperiodeId: UUID
}
