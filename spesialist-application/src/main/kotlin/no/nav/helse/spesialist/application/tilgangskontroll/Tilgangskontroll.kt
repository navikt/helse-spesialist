package no.nav.helse.spesialist.application.tilgangskontroll

import java.util.UUID

enum class Gruppe(
    private val gruppenøkkel: String,
) {
    KODE7("KODE7_SAKSBEHANDLER_GROUP"),
    BESLUTTER("BESLUTTER_SAKSBEHANDLER_GROUP"),
    SKJERMEDE("SKJERMEDE_PERSONER_GROUP"),
    STIKKPRØVE("SAKSBEHANDLERE_MED_TILGANG_TIL_STIKKPROVER"),
    ;

    fun idFra(env: Map<String, String>): UUID = UUID.fromString(env.getValue(gruppenøkkel))

    companion object {
        @Suppress("ktlint:standard:function-naming")
        fun __indreInnhold_kunForTest() = entries.associate { it.name to it.gruppenøkkel }
    }
}

open class Tilgangsgrupper(
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

class SpeilTilgangsgrupper(
    private val env: Map<String, String>,
) : Tilgangsgrupper(
        kode7GruppeId = Gruppe.KODE7.idFra(env),
        beslutterGruppeId = Gruppe.BESLUTTER.idFra(env),
        skjermedePersonerGruppeId = Gruppe.SKJERMEDE.idFra(env),
        stikkprøveGruppeId = Gruppe.STIKKPRØVE.idFra(env),
    )
