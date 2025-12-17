package no.nav.helse.spesialist.db.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class PgReservasjonDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `reserverer person`() {
        opprettData()
        val saksbehandler =
            sessionOf(dataSource).use {
                reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, FNR)
                reservasjonDao.hentReservasjonFor(FNR)?.reservertTil
                    ?: fail("Forventet at det skulle finnes en reservasjon i basen")
            }
        assertEquals(SAKSBEHANDLER_OID, saksbehandler.id.value)
        assertRiktigVarighet(finnGyldigTil())
    }

    @Test
    fun `ny reservasjon forlenger ikke fristen`() {
        opprettData()
        val enAnnenSaksbehandler = UUID.randomUUID()
        saksbehandlerDao.opprettEllerOppdater(
            enAnnenSaksbehandler,
            "Siri Siksbehindler",
            "siri.siksbehindler@nav.no",
            "S666666",
        )

        val saksbehandler =
            sessionOf(dataSource).use {
                reservasjonDao.reserverPerson(enAnnenSaksbehandler, FNR)
                val gyldigTil1 = finnGyldigTil()
                reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, FNR)
                val gyldigTil2 = finnGyldigTil()
                assertTrue(gyldigTil2.isEqual(gyldigTil1))
                reservasjonDao.hentReservasjonFor(FNR)?.reservertTil
                    ?: fail("Forventet at det skulle finnes en reservasjon i basen")
            }
        assertEquals(SAKSBEHANDLER_OID, saksbehandler.id.value)
        assertRiktigVarighet(finnGyldigTil())
    }

    private fun opprettData(fødselsnummer: String = FNR) {
        opprettPerson(person = lagPerson(id = Identitetsnummer.fraString(fødselsnummer)))
        saksbehandlerDao.opprettEllerOppdater(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT.value,
        )
    }

    private fun assertRiktigVarighet(gyldigTil: LocalDateTime) {
        assertEquals(LocalDate.now().atTime(23, 59, 59), gyldigTil)
    }

    private fun finnGyldigTil(): LocalDateTime {
        @Language("PostgreSQL")
        val query = """
            SELECT r.gyldig_til
            FROM reserver_person r
            JOIN person p ON p.id = r.person_ref
            WHERE p.fødselsnummer = :fnr AND r.gyldig_til > now();
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("fnr" to FNR))
                    .map { it.localDateTime("gyldig_til") }
                    .asSingle,
            )
        }!!
    }
}
