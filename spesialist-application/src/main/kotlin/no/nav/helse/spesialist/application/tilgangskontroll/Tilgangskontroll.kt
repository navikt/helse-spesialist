package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

enum class Gruppe { KODE7, BESLUTTER, SKJERMEDE, STIKKPRØVE }

class Tilgangsgrupper(
    kode7GruppeId: UUID,
    beslutterGruppeId: UUID,
    skjermedePersonerGruppeId: UUID,
    stikkprøveGruppeId: UUID,
) {
    private val gruppeUuidMap: Map<Gruppe, UUID> =
        mapOf(
            Gruppe.KODE7 to kode7GruppeId,
            Gruppe.BESLUTTER to beslutterGruppeId,
            Gruppe.SKJERMEDE to skjermedePersonerGruppeId,
            Gruppe.STIKKPRØVE to stikkprøveGruppeId,
        )

    fun gruppeId(gruppe: Gruppe): UUID = gruppeUuidMap[gruppe]!!

    fun alleGrupper(): Set<Gruppe> = gruppeUuidMap.keys

    fun alleUuider(): Set<UUID> = gruppeUuidMap.values.toSet()

    fun tilUuider(grupper: Set<Gruppe>) = gruppeUuidMap.filter { it.key in grupper }.values.toSet()

    fun tilGrupper(uuider: Set<UUID>) = gruppeUuidMap.filter { it.value in uuider }.keys
}
