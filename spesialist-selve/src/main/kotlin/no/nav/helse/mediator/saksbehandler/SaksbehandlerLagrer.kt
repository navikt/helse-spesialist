package no.nav.helse.mediator.saksbehandler

import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.toDto

class SaksbehandlerLagrer(private val saksbehandlerDao: SaksbehandlerDao) {
    fun lagre(saksbehandler: Saksbehandler) {
        val dto = saksbehandler.toDto()
        saksbehandlerDao.opprettEllerOppdater(
            oid = dto.oid,
            navn = dto.navn,
            epost = dto.epostadresse,
            ident = dto.ident,
        )
        saksbehandlerDao.oppdaterSistObservert(dto.oid)
    }
}
