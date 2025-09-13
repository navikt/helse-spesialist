package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.util.UUID

class TilgangsgruppeUuider(
    private val kode7GruppeUuid: UUID,
    private val beslutterGruppeUuid: UUID,
    private val skjermedePersonerGruppeUuid: UUID,
    private val stikkprøveGruppeUuid: UUID,
) {
    fun uuidFor(tilgangsgruppe: Tilgangsgruppe): UUID =
        when (tilgangsgruppe) {
            Tilgangsgruppe.KODE7 -> kode7GruppeUuid
            Tilgangsgruppe.BESLUTTER -> beslutterGruppeUuid
            Tilgangsgruppe.SKJERMEDE -> skjermedePersonerGruppeUuid
            Tilgangsgruppe.STIKKPRØVE -> stikkprøveGruppeUuid
        }

    fun uuiderFor(grupper: Collection<Tilgangsgruppe>) = grupper.map(::uuidFor).toSet()

    fun grupperFor(uuider: Collection<UUID>) =
        Tilgangsgruppe.entries.associateBy { uuidFor(it) }.let { uuidMap ->
            uuider.mapNotNull { uuidMap[it] }.toSet()
        }
}
