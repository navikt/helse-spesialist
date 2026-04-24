package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans

interface SaksbehandlerStansRepository {
    fun lagre(saksbehandlerStans: SaksbehandlerStans)

    fun finnAlle(identitetsnummer: Identitetsnummer): List<SaksbehandlerStans>

    fun finnAktiv(identitetsnummer: Identitetsnummer): SaksbehandlerStans?
}
