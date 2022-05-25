package no.nav.helse.modell.tildeling

import DatabaseIntegrationTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.Kjønn
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import java.time.LocalDate
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull

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
            reservasjonDao.hentReservasjonFor(FNR)
        } ?: fail("Forventet at det skulle finnes en reservasjon i basen")
        assertEquals(SAKSBEHANDLER_OID, saksbehandlerOid)
        assertEquals(12, varighetPåReservasjon())
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

    private fun varighetPåReservasjon(): Number {
        @Language("PostgreSQL")
        val query = """
        SELECT r.gyldig_til
        FROM reserver_person r
        JOIN person p ON p.id = r.person_ref
        WHERE p.fodselsnummer = :fnr AND r.gyldig_til > now();
        """
        val gyldigTil = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("fnr" to FNR.toLong()))
                    .map { it.localDateTime("gyldig_til") }.asSingle
            )
        }
        return Duration.between(LocalDateTime.now().minusSeconds(5), gyldigTil).toHours().toInt()
    }

}
