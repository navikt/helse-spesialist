package no.nav.helse.spesialist.domain.tilgangskontroll

class SaksbehandlerTilganger(
    private val tilgangsgrupper: Set<Tilgangsgruppe>,
) {
    fun harTilgangTilKode7() = Tilgangsgruppe.KODE7 in tilgangsgrupper

    fun harTilgangTilBeslutterOppgaver() = Tilgangsgruppe.BESLUTTER in tilgangsgrupper

    fun harTilgangTilSkjermedePersoner() = Tilgangsgruppe.SKJERMEDE in tilgangsgrupper
}
