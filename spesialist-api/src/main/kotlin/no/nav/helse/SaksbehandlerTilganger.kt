package no.nav.helse

import java.util.*

class SaksbehandlerTilganger(
    private val gruppetilganger: List<UUID>,
    private val kode7Saksbehandlergruppe: UUID,
    private val riskSaksbehandlergruppe: UUID,
    private val NAVident: String = ""
) {

    fun harTilgangTilKode7Oppgaver() = kode7Saksbehandlergruppe in gruppetilganger

    fun harTilgangTilRiskOppgaver() = riskSaksbehandlergruppe in gruppetilganger

    fun kanSeAlleOppgaver() = NAVident in listOf("G103083", "D117949", "A148751", "N115007", "C117102", "S145454", "E148846")
}
