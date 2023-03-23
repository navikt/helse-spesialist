package no.nav.helse.modell.tildeling

import DatabaseIntegrationTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class ReservasjonDaoTest : DatabaseIntegrationTest() {

    private companion object {
        private const val FNR = "12345678911"
        private val objectMapper = jacksonObjectMapper()
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
    }

    @Test
    fun `reserverer person`() {
        opprettTabeller()
        val saksbehandlerOid = sessionOf(dataSource).use {
            reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, FNR)
            reservasjonDao.hentReservertTil(FNR)
        } ?: fail("Forventet at det skulle finnes en reservasjon i basen")
        assertEquals(SAKSBEHANDLER_OID, saksbehandlerOid)
        assertRiktigVarighet(72)
    }

    @Test
    fun `ny reservasjon forlenger fristen`() {
        opprettTabeller()
        val enAnnenSaksbehandler = UUID.randomUUID()
        saksbehandlerDao.opprettSaksbehandler(
            enAnnenSaksbehandler,
            "Siri Siksbehindler",
            "siri.siksbehindler@nav.no",
            "S666666"
        )

        val saksbehandlerOid = sessionOf(dataSource).use {
            reservasjonDao.reserverPerson(enAnnenSaksbehandler, FNR)
            val gyldigTil1 = finnGyldigTil()
            reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, FNR)
            val gyldigTil2 = finnGyldigTil()
            assertTrue(gyldigTil2.isAfter(gyldigTil1))
            reservasjonDao.hentReservertTil(FNR)
        } ?: fail("Forventet at det skulle finnes en reservasjon i basen")
        assertEquals(SAKSBEHANDLER_OID, saksbehandlerOid)
        assertRiktigVarighet(72)
    }

    private fun opprettTabeller(fødselsnummer: String = FNR) {
        val personinfoRef = personDao.insertPersoninfo(
            "KARI",
            "Mellomnavn",
            "Nordmann",
            LocalDate.EPOCH,
            Kjønn.Kvinne,
            Adressebeskyttelse.Ugradert
        )
        val utbetalingerRef = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        personDao.insertPerson(
            fødselsnummer,
            "4321098765432", personinfoRef, "0301".toInt(), utbetalingerRef
        )

        saksbehandlerDao.opprettSaksbehandler(
            SAKSBEHANDLER_OID,
            "Sara Saksbehandler",
            "sara.saksbehandler@nav.no",
            "S999999"
        )
    }

    // Pga. at det kan forekomme tidssonebytte i databasen må vi benytte oss av tidsbiblioteket for å vite hvor i
    // fremtiden vi er om 72 timer.
    // Eksempel: gyldig_til er vintertid mens "nå" er sommertid
    private fun assertRiktigVarighet(forventetVarighet: Long) {
        val om72Timer = ZonedDateTime.now().plusHours(forventetVarighet).toLocalDateTime()
        assertEquals(0.0, Duration.between(finnGyldigTil(), om72Timer).toSeconds().toDouble(), 5.0)
    }

    private fun finnGyldigTil(): LocalDateTime {
        @Language("PostgreSQL")
        val query = """
            SELECT r.gyldig_til
            FROM reserver_person r
            JOIN person p ON p.id = r.person_ref
            WHERE p.fodselsnummer = :fnr AND r.gyldig_til > now();
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("fnr" to FNR.toLong()))
                    .map { it.localDateTime("gyldig_til") }.asSingle
            )
        }!!
    }

}
