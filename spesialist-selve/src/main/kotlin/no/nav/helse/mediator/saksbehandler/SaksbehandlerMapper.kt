package no.nav.helse.mediator.saksbehandler

import java.util.UUID
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.SaksbehandlerVisitor
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

object SaksbehandlerMapper {

    fun Saksbehandler.tilApiversjon(): SaksbehandlerFraApi {
        return tilApiMapper.let {
            this.accept(it)
            it.saksbehandlerFraApi
        }
    }

    private val tilApiMapper get() = object: SaksbehandlerVisitor {
        lateinit var saksbehandlerFraApi: SaksbehandlerFraApi
        override fun visitSaksbehandler(epostadresse: String, oid: UUID, navn: String, ident: String) {
            saksbehandlerFraApi = SaksbehandlerFraApi(oid, navn, epostadresse, ident, emptyList())
        }
    }
 }