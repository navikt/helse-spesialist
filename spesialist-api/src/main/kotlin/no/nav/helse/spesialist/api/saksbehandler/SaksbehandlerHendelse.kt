package no.nav.helse.spesialist.api.saksbehandler

import java.util.UUID

internal interface SaksbehandlerHendelse {
    fun tellernavn(): String
    fun saksbehandlerOid(): UUID
    fun håndter(saksbehandler: Saksbehandler)
}