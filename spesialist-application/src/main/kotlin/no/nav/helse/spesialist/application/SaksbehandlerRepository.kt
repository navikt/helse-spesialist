package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid

interface SaksbehandlerRepository {
    fun lagre(saksbehandler: Saksbehandler)

    fun finn(oid: SaksbehandlerOid): Saksbehandler?
}
