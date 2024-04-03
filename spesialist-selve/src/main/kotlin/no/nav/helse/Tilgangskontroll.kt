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
        fun __indreInnhold_kunForTest() = entries.associate { it.name to it.gruppenøkkel }
    }
}

internal class Tilgangsgrupper(private val env: Map<String, String>) {
    val kode7GruppeId: UUID by lazy { Gruppe.KODE7.idFra(env) }
    val beslutterGruppeId: UUID by lazy { Gruppe.BESLUTTER.idFra(env) }
    val skjermedePersonerGruppeId: UUID by lazy { Gruppe.SKJERMEDE.idFra(env) }
    val stikkprøveGruppeId: UUID by lazy { Gruppe.STIKKPRØVE.idFra(env) }
    val spesialsakGruppeId: UUID by lazy { Gruppe.SPESIALSAK.idFra(env) }

    fun gruppeId(gruppe: Gruppe): UUID = gruppe.idFra(env)
}
