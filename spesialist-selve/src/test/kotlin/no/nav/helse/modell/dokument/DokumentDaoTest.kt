package no.nav.helse.modell.dokument

import DatabaseIntegrationTest

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
    fun `returnerer null dersom dokument ikke er lagret i DB`() {
        val dokumentFraDB = dokumentDao.hent(FNR, UUID.randomUUID())

        assertNull(dokumentFraDB)
    }
}