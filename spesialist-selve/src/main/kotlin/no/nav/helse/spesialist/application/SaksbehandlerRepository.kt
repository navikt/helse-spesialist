package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.modell.Saksbehandler
import java.util.UUID

interface SaksbehandlerRepository {
    fun finn(saksbehandlerId: UUID): Saksbehandler?
}
