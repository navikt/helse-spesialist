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

interface Tilgangsgrupper {
    val kode7GruppeId: UUID
    val beslutterGruppeId: UUID
    val skjermedePersonerGruppeId: UUID
    val stikkprøveGruppeId: UUID

    fun gruppeId(gruppe: Gruppe): UUID

    fun gruppeUuidMap(): Map<Gruppe, UUID> =
        mapOf(
            Gruppe.KODE7 to kode7GruppeId,
            Gruppe.BESLUTTER to beslutterGruppeId,
            Gruppe.SKJERMEDE to skjermedePersonerGruppeId,
            Gruppe.STIKKPRØVE to stikkprøveGruppeId,
        )

    fun alleGrupper(): Set<Gruppe> = gruppeUuidMap().keys

    fun alleUuider(): Set<UUID> = gruppeUuidMap().values.toSet()

    fun tilGrupper(uuider: Set<UUID>) = gruppeUuidMap().filter { it.value in uuider }.keys
}

class SpeilTilgangsgrupper(
    private val env: Map<String, String>,
) : Tilgangsgrupper {
    override val kode7GruppeId: UUID by lazy { Gruppe.KODE7.idFra(env) }
    override val beslutterGruppeId: UUID by lazy { Gruppe.BESLUTTER.idFra(env) }
    override val skjermedePersonerGruppeId: UUID by lazy { Gruppe.SKJERMEDE.idFra(env) }
    override val stikkprøveGruppeId: UUID by lazy { Gruppe.STIKKPRØVE.idFra(env) }

    override fun gruppeId(gruppe: Gruppe): UUID = gruppe.idFra(env)
}
