package no.nav.helse

import java.util.UUID

// Definisjoner på gruppene vi har et forhold til, og deres runtime UUID-er

enum class Gruppe(private val gruppenøkkel: String) {
    KODE7("KODE7_SAKSBEHANDLER_GROUP"),
    BESLUTTER("BESLUTTER_SAKSBEHANDLER_GROUP"),
    SKJERMEDE("SKJERMEDE_PERSONER_GROUP"),
    STIKKPRØVE("SAKSBEHANDLERE_MED_TILGANG_TIL_STIKKPROVER"),
    SPESIALSAK("SAKSBEHANDLERE_MED_TILGANG_TIL_SPESIALSAKER"),
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
    val spesialsakGruppeId: UUID

    fun gruppeId(gruppe: Gruppe): UUID
}

internal class SpeilTilgangsgrupper(private val env: Map<String, String>) : Tilgangsgrupper {
    override val kode7GruppeId: UUID by lazy { Gruppe.KODE7.idFra(env) }
    override val beslutterGruppeId: UUID by lazy { Gruppe.BESLUTTER.idFra(env) }
    override val skjermedePersonerGruppeId: UUID by lazy { Gruppe.SKJERMEDE.idFra(env) }
    override val stikkprøveGruppeId: UUID by lazy { Gruppe.STIKKPRØVE.idFra(env) }
    override val spesialsakGruppeId: UUID by lazy { Gruppe.SPESIALSAK.idFra(env) }

    override fun gruppeId(gruppe: Gruppe): UUID = gruppe.idFra(env)
}
