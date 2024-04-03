package no.nav.helse.mediator.saksbehandler

import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.SaksbehandlerVisitor
import java.util.UUID

class SaksbehandlerLagrer(private val saksbehandlerDao: SaksbehandlerDao) : SaksbehandlerVisitor {
    private lateinit var saksbehandlerFraDatabase: SaksbehandlerFraDatabase

    internal fun lagre(saksbehandler: Saksbehandler) {
        saksbehandler.accept(this)
        saksbehandlerDao.opprettSaksbehandler(
            oid = saksbehandlerFraDatabase.oid,
            navn = saksbehandlerFraDatabase.navn,
            epost = saksbehandlerFraDatabase.epostadresse,
            ident = saksbehandlerFraDatabase.ident,
        )
    }

    override fun visitSaksbehandler(
        epostadresse: String,
        oid: UUID,
        navn: String,
        ident: String,
    ) {
        saksbehandlerFraDatabase = SaksbehandlerFraDatabase(epostadresse, oid, navn, ident)
    }
}
