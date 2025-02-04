package no.nav.helse.spesialist.api.graphql.mutation

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.modell.NotatType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertNotNull

internal class NotatMutationTest : AbstractGraphQLApiTest() {
    @Test
    fun `leggTilNotat fungerer som forventet`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body =
            runQuery(
                """
            mutation LeggTilNotat {
                leggTilNotat(
                    tekst: "Dette er et notat",
                    type: Generelt,
                    vedtaksperiodeId: "${PERIODE.id}",
                    saksbehandlerOid: "${SAKSBEHANDLER.oid}"
                ) {
                    id,
                    dialogRef,
                    tekst,
                    opprettet,
                    saksbehandlerOid,
                    saksbehandlerNavn,
                    saksbehandlerEpost,
                    saksbehandlerIdent,
                    vedtaksperiodeId,
                    feilregistrert,
                    feilregistrert_tidspunkt,
                    type,
                    kommentarer {
                        id,
                        tekst,
                        opprettet,
                        saksbehandlerident,
                        feilregistrert_tidspunkt,
                    }
                }
            }
        """,
            )

        val notatId = body["data"]?.get("leggTilNotat")?.get("id")?.asInt()
        assertNotNull(notatId, "Fikk ikke noen ID på opprettet notat i svaret: $body")

        val dialogRef = body["data"]?.get("leggTilNotat")?.get("dialogRef")?.asInt()
        assertNotNull(dialogRef, "Fikk ikke noen ID på opprettet dialog i svaret: $body")

        // Bekreft persistert resultat
        val lagretNotat = sessionFactory.transactionalSessionScope {
            it.notatRepository.finn(notatId)
        }
        assertNotNull(lagretNotat, "Lagret notat med ID $notatId ble ikke gjenfunnet i databasen")

        assertEquals(notatId, lagretNotat.id())
        assertEquals(NotatType.Generelt, lagretNotat.type)
        assertEquals("Dette er et notat", lagretNotat.tekst)
        assertEquals(dialogRef.toLong(), lagretNotat.dialogRef)
        assertEquals(PERIODE.id, lagretNotat.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER.oid, lagretNotat.saksbehandlerOid)
        val opprettetTidspunkt = lagretNotat.opprettetTidspunkt
        with(opprettetTidspunkt) {
            val now = LocalDateTime.now()
            assertTrue(isBefore(now)) { "Forventet at den lagrede verdien av opprettetTidspunkt var før nå ($now), men den var $this" }
            assertTrue(isAfter(now.minusSeconds(5))) { "Forventet at den lagrede verdien av opprettetTidspunkt var mindre enn fem sekunder før nå ($now), men den var $this" }
        }
        assertFalse(lagretNotat.feilregistrert)
        assertNull(lagretNotat.feilregistrertTidspunkt) { "feilregistrertTidspunkt ble lagret selv om notatet er nytt" }

        // Bekreft svaret
        assertEquals(
            expectedJsonString = """
                            {
                              "data" : {
                                "leggTilNotat" : {
                                  "id" : $notatId,
                                  "dialogRef" : $dialogRef,
                                  "tekst" : "Dette er et notat",
                                  "opprettet" : "$opprettetTidspunkt",
                                  "saksbehandlerOid" : "${SAKSBEHANDLER.oid}",
                                  "saksbehandlerNavn" : "${SAKSBEHANDLER.navn}",
                                  "saksbehandlerEpost" : "${SAKSBEHANDLER.epost}",
                                  "saksbehandlerIdent" : "${SAKSBEHANDLER.ident}",
                                  "vedtaksperiodeId" : "${PERIODE.id}",
                                  "feilregistrert" : false,
                                  "feilregistrert_tidspunkt" : null,
                                  "type" : "Generelt",
                                  "kommentarer" : [ ]
                                }
                              }
                            }
                        """.trimIndent(),
            actualJsonNode = body
        )
    }

    @Test
    fun `feilregistrererNotat fungerer som forventet`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val dialogRef = opprettDialog()
        val notatId = opprettNotat(dialogRef = dialogRef)

        val body =
            runQuery(
                """
            mutation FeilregistrerNotat {
                feilregistrerNotat(id: $notatId) { 
                    id,
                    dialogRef,
                    tekst,
                    opprettet,
                    saksbehandlerOid,
                    saksbehandlerNavn,
                    saksbehandlerEpost,
                    saksbehandlerIdent,
                    vedtaksperiodeId,
                    feilregistrert,
                    feilregistrert_tidspunkt,
                    type,
                    kommentarer {
                        id,
                        tekst,
                        opprettet,
                        saksbehandlerident,
                        feilregistrert_tidspunkt,
                    }
                }
            }
            """.trimIndent()
            )

        // Bekreft persistert resultat
        val lagretNotat = sessionFactory.transactionalSessionScope {
            it.notatRepository.finn(notatId)
        }
        assertTrue(lagretNotat!!.feilregistrert)

        val feilregistrertTidspunkt = lagretNotat.feilregistrertTidspunkt

        val now = LocalDateTime.now()
        assertNotNull(feilregistrertTidspunkt, "feilregistrertTidspunkt ble ikke lagret")
        assertTrue(feilregistrertTidspunkt.isBefore(now)) { "Forventet at den lagrede verdien av feilregistrertTidspunkt var før nå ($now), men den var $feilregistrertTidspunkt" }
        assertTrue(feilregistrertTidspunkt.isAfter(now.minusSeconds(5))) { "Forventet at den lagrede verdien av feilregistrertTidspunkt var mindre enn fem sekunder før nå ($now), men den var $feilregistrertTidspunkt" }

        val opprettetTidspunkt = lagretNotat.opprettetTidspunkt
        assertTrue(feilregistrertTidspunkt.isAfter(opprettetTidspunkt)) { "Forventet at den lagrede verdien av feilregistrertTidspunkt var etter opprettet tidspunkt ($opprettetTidspunkt), men den var $feilregistrertTidspunkt" }

        // Bekreft svaret
        assertEquals(
            expectedJsonString = """
                            {
                              "data" : {
                                "feilregistrerNotat" : {
                                  "id" : $notatId,
                                  "dialogRef" : $dialogRef,
                                  "tekst" : "Et notat",
                                  "opprettet" : "$opprettetTidspunkt",
                                  "saksbehandlerOid" : "${SAKSBEHANDLER.oid}",
                                  "saksbehandlerNavn" : "${SAKSBEHANDLER.navn}",
                                  "saksbehandlerEpost" : "${SAKSBEHANDLER.epost}",
                                  "saksbehandlerIdent" : "${SAKSBEHANDLER.ident}",
                                  "vedtaksperiodeId" : "${PERIODE.id}",
                                  "feilregistrert" : true,
                                  "feilregistrert_tidspunkt" : "$feilregistrertTidspunkt",
                                  "type" : "Generelt",
                                  "kommentarer" : [ ]
                                }
                              }
                            }
                        """.trimIndent(),
            actualJsonNode = body
        )
    }

    @Test
    fun `leggTilKommentar fungerer som forventet`() {
        opprettSaksbehandler()
        val dialogRef = opprettDialog()

        val body =
            runQuery(
                """
            mutation LeggTilKommentar {
                leggTilKommentar(dialogRef: $dialogRef, tekst: "En kommentar", saksbehandlerident: "${SAKSBEHANDLER.ident}") {
                    id,
                    tekst,
                    opprettet,
                    saksbehandlerident,
                    feilregistrert_tidspunkt,
                }
            }
        """,
            )

        val kommentarId = body["data"]?.get("leggTilKommentar")?.get("id")?.asInt()
        assertNotNull(kommentarId, "Fikk ikke noen ID på opprettet kommentar i svaret: $body")

        // Bekreft persistert resultat
        val lagretDialog = sessionFactory.transactionalSessionScope {
            it.dialogRepository.finnForKommentar(kommentarId)
        }
        assertNotNull(lagretDialog, "Lagret dialog for kommentar med ID $kommentarId ble ikke gjenfunnet i databasen")
        val lagretKommentar = lagretDialog.finnKommentar(kommentarId)
        assertNotNull(lagretKommentar, "Lagret kommentar med ID $kommentarId ble ikke gjenfunnet i databasen")

        assertEquals(kommentarId, lagretKommentar.id())
        assertEquals("En kommentar", lagretKommentar.tekst)
        assertEquals(SAKSBEHANDLER.ident, lagretKommentar.saksbehandlerident)
        val opprettetTidspunkt = lagretKommentar.opprettetTidspunkt
        with(opprettetTidspunkt) {
            val now = LocalDateTime.now()
            assertTrue(isBefore(now)) { "Forventet at den lagrede verdien av opprettetTidspunkt var før nå ($now), men den var $this" }
            assertTrue(isAfter(now.minusSeconds(5))) { "Forventet at den lagrede verdien av opprettetTidspunkt var mindre enn fem sekunder før nå ($now), men den var $this" }
        }
        assertNull(lagretKommentar.feilregistrertTidspunkt) { "feilregistrertTidspunkt ble lagret selv om kommentaren er ny" }

        // Bekreft svaret
        assertEquals(
            expectedJsonString = """
                            {
                              "data" : {
                                "leggTilKommentar" : {
                                  "id" : $kommentarId,
                                  "tekst" : "En kommentar",
                                  "opprettet" : "$opprettetTidspunkt",
                                  "saksbehandlerident" : "${SAKSBEHANDLER.ident}",
                                  "feilregistrert_tidspunkt" : null
                                }
                              }
                            }
                        """.trimIndent(),
            actualJsonNode = body
        )
    }

    @Test
    fun `feilregistrererKommentar fungerer som forventet`() {
        opprettSaksbehandler()
        val dialogRef = opprettDialog()
        val kommentarId = opprettKommentar(dialogRef = dialogRef)

        val body =
            runQuery(
                """
            mutation FeilregistrerKommentar {
                feilregistrerKommentar(id: $kommentarId) {
                    id,
                    tekst,
                    opprettet,
                    saksbehandlerident,
                    feilregistrert_tidspunkt,
                }
            }
        """,
            )

        // Bekreft persistert resultat
        val lagretDialog = sessionFactory.transactionalSessionScope {
            it.dialogRepository.finn(dialogRef)
        }
        assertNotNull(lagretDialog, "Lagret dialog med ID $dialogRef ble ikke gjenfunnet i databasen")
        val lagretKommentar = lagretDialog.finnKommentar(kommentarId)
        assertNotNull(lagretKommentar, "Lagret kommentar med ID $kommentarId ble ikke gjenfunnet i databasen")

        val feilregistrertTidspunkt = lagretKommentar.feilregistrertTidspunkt

        val now = LocalDateTime.now()
        assertNotNull(feilregistrertTidspunkt, "feilregistrertTidspunkt ble ikke lagret")
        assertTrue(feilregistrertTidspunkt.isBefore(now)) { "Forventet at den lagrede verdien av feilregistrertTidspunkt var før nå ($now), men den var $feilregistrertTidspunkt" }
        assertTrue(feilregistrertTidspunkt.isAfter(now.minusSeconds(5))) { "Forventet at den lagrede verdien av feilregistrertTidspunkt var mindre enn fem sekunder før nå ($now), men den var $feilregistrertTidspunkt" }

        val opprettetTidspunkt = lagretKommentar.opprettetTidspunkt
        assertTrue(feilregistrertTidspunkt.isAfter(opprettetTidspunkt)) { "Forventet at den lagrede verdien av feilregistrertTidspunkt var etter opprettet tidspunkt ($opprettetTidspunkt), men den var $feilregistrertTidspunkt" }

        // Bekreft svaret
        assertEquals(
            expectedJsonString = """
                            {
                              "data" : {
                                "feilregistrerKommentar" : {
                                  "id" : $kommentarId,
                                  "tekst" : "En kommentar",
                                  "opprettet" : "$opprettetTidspunkt",
                                  "saksbehandlerident" : "${SAKSBEHANDLER.ident}",
                                  "feilregistrert_tidspunkt" : "$feilregistrertTidspunkt"
                                }
                              }
                            }
                        """.trimIndent(),
            actualJsonNode = body
        )
    }

    private fun assertEquals(expectedJsonString: String, actualJsonNode: JsonNode) {
        val writer = objectMapper.writerWithDefaultPrettyPrinter()
        assertEquals(
            writer.writeValueAsString(objectMapper.readTree(expectedJsonString)),
            writer.writeValueAsString(actualJsonNode)
        )
    }
}
