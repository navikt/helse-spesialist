package no.nav.helse.spesialist.api

import java.util.UUID

class SaksbehandlerTilganger(
    private val gruppetilganger: List<UUID>,
    private val kode7Saksbehandlergruppe: UUID,
    private val beslutterSaksbehandlergruppe: UUID,
    private val skjermedePersonerSaksbehandlergruppe: UUID,
) {
    fun harTilgangTilKode7() = kode7Saksbehandlergruppe in gruppetilganger

    fun harTilgangTilBeslutterOppgaver() = beslutterSaksbehandlergruppe in gruppetilganger

    fun harTilgangTilSkjermedePersoner() = skjermedePersonerSaksbehandlergruppe in gruppetilganger
}
