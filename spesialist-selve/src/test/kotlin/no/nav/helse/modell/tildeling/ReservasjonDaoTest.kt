package no.nav.helse.modell.tildeling

import DatabaseIntegrationTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.util.UUID
import kotliquery.sessionOf
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.Kjønn
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ReservasjonDaoTest : DatabaseIntegrationTest() {

    private companion object {
        private const val FNR = "12345678911"
        private val objectMapper = jacksonObjectMapper()
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
    }

    @Test
    fun `reserverer person`() {
        opprettTabeller()
        val (saksbehandlerOid, _) = sessionOf(dataSource).use {
            reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, FNR)
            reservasjonDao.hentReservasjonFor(FNR)
        } ?: fail("Forventet at det skulle finnes en reservasjon i basen")
        assertEquals(SAKSBEHANDLER_OID, saksbehandlerOid)
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
        val personRef = personDao.insertPerson(
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

}
