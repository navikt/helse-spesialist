package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.modell.Saksbehandler
import no.nav.helse.spesialist.modell.SaksbehandlerOid

interface SaksbehandlerRepository {
    fun finn(oid: SaksbehandlerOid): Saksbehandler?
}
