package no.nav.helse.mediator.saksbehandler

import java.util.UUID
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.SaksbehandlerVisitor
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

internal object SaksbehandlerMapper {

    internal fun Saksbehandler.tilApiversjon(): SaksbehandlerFraApi {
        return tilApiMapper.let {
            this.accept(it)
            it.saksbehandlerFraApi
        }
    }

    internal fun Saksbehandler.tilDatabaseversjon(): SaksbehandlerFraDatabase {
        return tilDatabaseMapper.let {
            this.accept(it)
            it.saksbehandlerForDatabase
        }
    }

    internal fun SaksbehandlerFraApi.tilModellversjon(tilgangsgrupper: Tilgangsgrupper): Saksbehandler {
        return Saksbehandler(
            epostadresse = epost,
            oid = oid,
            navn = navn,
            ident = ident,
            tilgangskontroll = TilgangskontrollørForApi(grupper, tilgangsgrupper)
        )
    }

    private val tilApiMapper get() = object: SaksbehandlerVisitor {
        lateinit var saksbehandlerFraApi: SaksbehandlerFraApi
        override fun visitSaksbehandler(epostadresse: String, oid: UUID, navn: String, ident: String) {
            saksbehandlerFraApi = SaksbehandlerFraApi(oid, navn, epostadresse, ident, emptyList())
        }
    }

    private val tilDatabaseMapper get() = object: SaksbehandlerVisitor {
        lateinit var saksbehandlerForDatabase: SaksbehandlerFraDatabase
        override fun visitSaksbehandler(epostadresse: String, oid: UUID, navn: String, ident: String) {
            saksbehandlerForDatabase = SaksbehandlerFraDatabase(epostadresse, oid, navn, ident)
        }
    }
 }