package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.util.UUID

class TilgangsgruppeUuider(
    private val egenAnsattGruppeUuid: UUID,
    private val kode7GruppeUuid: UUID,
) {
    fun uuidFor(tilgangsgruppe: Tilgangsgruppe): UUID =
        when (tilgangsgruppe) {
            Tilgangsgruppe.EGEN_ANSATT -> egenAnsattGruppeUuid
            Tilgangsgruppe.KODE_7 -> kode7GruppeUuid
        }

    fun uuiderFor(grupper: Collection<Tilgangsgruppe>) = grupper.map(::uuidFor).toSet()

    fun grupperFor(uuider: Collection<UUID>) =
        Tilgangsgruppe.entries.associateBy { uuidFor(it) }.let { uuidMap ->
            uuider.mapNotNull { uuidMap[it] }.toSet()
        }
}
