package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.util.UUID

class Tilgangsgrupper(
    private val kode7GruppeId: UUID,
    private val beslutterGruppeId: UUID,
    private val skjermedePersonerGruppeId: UUID,
    private val stikkprøveGruppeId: UUID,
) {
    fun uuidFor(tilgangsgruppe: Tilgangsgruppe): UUID =
        when (tilgangsgruppe) {
            Tilgangsgruppe.KODE7 -> kode7GruppeId
            Tilgangsgruppe.BESLUTTER -> beslutterGruppeId
            Tilgangsgruppe.SKJERMEDE -> skjermedePersonerGruppeId
            Tilgangsgruppe.STIKKPRØVE -> stikkprøveGruppeId
        }

    fun alleGrupper(): Set<Tilgangsgruppe> = Tilgangsgruppe.entries.toSet()

    fun alleUuider(): Set<UUID> = Tilgangsgruppe.entries.map(::uuidFor).toSet()

    fun uuiderFor(grupper: Set<Tilgangsgruppe>) = grupper.map(::uuidFor).toSet()

    fun grupperFor(uuider: Set<UUID>) =
        Tilgangsgruppe.entries.associateBy { uuidFor(it) }.let { uuidMap ->
            uuider.mapNotNull { uuidMap[it] }.toSet()
        }
}
