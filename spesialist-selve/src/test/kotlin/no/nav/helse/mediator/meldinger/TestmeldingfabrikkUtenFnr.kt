package no.nav.helse.mediator.meldinger

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Testdata.VARSEL_KODE_1
import no.nav.helse.Testdata.VARSEL_KODE_2
import no.nav.helse.rapids_rivers.JsonMessage

internal class TestmeldingfabrikkUtenFnr {

    internal fun lagVarseldefinisjonerEndret(
        id: UUID,
        definisjoner: List<Map<String, Any>> =
            listOf(
                lagVarseldefinisjon(kode = VARSEL_KODE_1),
                lagVarseldefinisjon(kode = VARSEL_KODE_2)
            )
    ): String {
        return nyHendelse(id, "varseldefinisjoner_endret", mapOf("definisjoner" to definisjoner))
    }

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )

    internal fun lagVarseldefinisjon(
        id: UUID = UUID.randomUUID(),
        kode: String = "EN_KODE",
        tittel: String = "EN_TITTEL",
        forklaring: String = "EN_FORKLARING",
        handling: String = "EN_HANDLING",
        opprettet: LocalDateTime = LocalDateTime.now(),
    ): Map<String, Any> = mapOf(
        "id" to id,
        "kode" to kode,
        "tittel" to tittel,
        "forklaring" to forklaring,
        "handling" to handling,
        "avviklet" to false,
        "opprettet" to opprettet,
    )
}
