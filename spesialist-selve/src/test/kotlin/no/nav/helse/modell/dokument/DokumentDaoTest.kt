package no.nav.helse.modell.dokument

import DatabaseIntegrationTest
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class DokumentDaoTest : DatabaseIntegrationTest() {
    @BeforeEach
    fun setup() {
        testhendelse()
        opprettPerson()
    }

    @Test
    fun `lagrer dokument`() {
        val dokumentId = UUID.randomUUID()
        val dokument = objectMapper.readTree("""{"testsøknad":"hei"}""")
        dokumentDao.lagre(FNR, dokumentId, dokument)

        val dokumentFraDB = dokumentDao.hent(FNR, dokumentId)

        assertNotNull(dokumentFraDB)

        if (dokumentFraDB != null) {
            assertEquals("hei", dokumentFraDB["testsøknad"].asText())
        }
    }

    @Test
    fun `Overskriver dokument dersom det kommer in på nytt`() {
        val dokumentId = UUID.randomUUID()
        val dokument = objectMapper.readTree("""{"testsøknad":"hei"}""")
        val dokument2 = objectMapper.readTree("""{"testsøknad":"hade"}""")
        dokumentDao.lagre(FNR, dokumentId, dokument)
        dokumentDao.lagre(FNR, dokumentId, dokument2)

        val dokumentFraDB = dokumentDao.hent(FNR, dokumentId)

        if (dokumentFraDB != null) {
            assertEquals("hade", dokumentFraDB["testsøknad"].asText())
        }
    }

    @Test
    fun `Sletter dokumenter eldre enn 3 mnd`() {
        val dokumentId1 = UUID.randomUUID()
        val dokumentId2 = UUID.randomUUID()
        val dokument1 = objectMapper.readTree("""{"testsøknad":"gammel"}""")
        val dokument2 = objectMapper.readTree("""{"testsøknad":"ny"}""")
        lagreDokumentMedOpprettet(LocalDateTime.now().minusMonths(4), FNR, dokumentId1, dokument1)
        lagreDokumentMedOpprettet(LocalDateTime.now(), FNR, dokumentId2, dokument2)
        val antallSlettet = dokumentDao.slettGamleDokumenter()

        assertNull(dokumentDao.hent(FNR, dokumentId1))
        assertNotNull(dokumentDao.hent(FNR, dokumentId2))
        assertEquals(1, antallSlettet)
    }

    @Test
    fun `returnerer null dersom dokument ikke er lagret i DB`() {
        val dokumentFraDB = dokumentDao.hent(FNR, UUID.randomUUID())

        assertNull(dokumentFraDB)
    }

    private fun lagreDokumentMedOpprettet(
        opprettet: LocalDateTime,
        fødselsnummer: String,
        dokumentId: UUID,
        dokument: JsonNode,
    ) = query(
        """
            SELECT id FROM person WHERE fodselsnummer=:fodselsnummer
        """.trimIndent(), "fodselsnummer" to fødselsnummer.toLong()
    ).single { it.int("id") }?.let { personId ->
        query(
        """
            INSERT INTO dokumenter (dokument_id, person_ref, dokument, opprettet)
            VALUES (
                :dokumentId,
                :personId,
                :dokument::json,
                :opprettet
            )
        """.trimIndent(),
            "dokumentId" to dokumentId,
            "personId" to personId,
            "dokument" to objectMapper.writeValueAsString(dokument),
            "opprettet" to opprettet
        ).update()
    }
}