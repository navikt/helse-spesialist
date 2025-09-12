package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

enum class Gruppe { KODE7, BESLUTTER, SKJERMEDE, STIKKPRØVE }

class Tilgangsgrupper(
    private val kode7GruppeId: UUID,
    private val beslutterGruppeId: UUID,
    private val skjermedePersonerGruppeId: UUID,
    private val stikkprøveGruppeId: UUID,
) {
    fun uuidFor(gruppe: Gruppe): UUID =
        when (gruppe) {
            Gruppe.KODE7 -> kode7GruppeId
            Gruppe.BESLUTTER -> beslutterGruppeId
            Gruppe.SKJERMEDE -> skjermedePersonerGruppeId
            Gruppe.STIKKPRØVE -> stikkprøveGruppeId
        }

    fun alleGrupper(): Set<Gruppe> = Gruppe.entries.toSet()

    fun alleUuider(): Set<UUID> = Gruppe.entries.map(::uuidFor).toSet()

    fun uuiderFor(grupper: Set<Gruppe>) = grupper.map(::uuidFor).toSet()

    fun grupperFor(uuider: Set<UUID>) =
        Gruppe.entries.associateBy { uuidFor(it) }.let { uuidMap ->
            uuider.mapNotNull { uuidMap[it] }.toSet()
        }
}
