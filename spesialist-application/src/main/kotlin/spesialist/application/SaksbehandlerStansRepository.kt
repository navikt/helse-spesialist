package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans

interface SaksbehandlerStansRepository {
    fun lagre(saksbehandlerStans: SaksbehandlerStans)

    fun finn(identitetsnummer: Identitetsnummer): SaksbehandlerStans?
}
