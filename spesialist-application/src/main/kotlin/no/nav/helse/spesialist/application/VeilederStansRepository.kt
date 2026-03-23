package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VeilederStans

interface VeilederStansRepository {
    fun lagre(veilederStans: VeilederStans)

    fun finnAlle(identitetsnummer: Identitetsnummer): List<VeilederStans>

    fun finnAktiv(identitetsnummer: Identitetsnummer): VeilederStans?
}
