package no.nav.helse.modell.tildeling

import DatabaseIntegrationTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.sessionOf
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.Kjønn
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ReservasjonDaoTest : DatabaseIntegrationTest() {

    private companion object {
        private const val FNR = "12345678911"
        private val objectMapper = jacksonObjectMapper()
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
    }

    @Test
    fun `reserverer person`() {
        val personRef = opprettTabeller()
        val saksbehandlerOid = sessionOf(dataSource).use {
            reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, FNR)
            reservasjonDao.hentReservasjonFor(personRef)
        }
        assertEquals(SAKSBEHANDLER_OID, saksbehandlerOid)
    }

    @Test
    fun `sletter reservert person`() {
        val personRef = opprettTabeller()
        val reservertPerson = sessionOf(dataSource).use {
            reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, FNR)
            reservasjonDao.slettReservasjon(SAKSBEHANDLER_OID, personRef)
            reservasjonDao.hentReservasjonFor(personRef)
        }
        assertNull(reservertPerson)
    }

    @Test
    fun `sletter reservert person med fnr`() {
        val fnr1 = "2444222"
        val fnr2 = "1231313"

        val personRef1 = opprettTabeller(fnr1)
        val personRef2 = opprettTabeller(fnr2)

        val reservertPerson1 = sessionOf(dataSource).use {
            reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, fnr1)
            reservasjonDao.slettReservasjon(fnr1)
            reservasjonDao.hentReservasjonFor(personRef1)
        }

        val reservertPerson2 = sessionOf(dataSource).use {
            reservasjonDao.reserverPerson(SAKSBEHANDLER_OID, fnr2)
            reservasjonDao.hentReservasjonFor(personRef2)
        }

        assertNull(reservertPerson1)
        assertNotNull(reservertPerson2)
    }

    private fun opprettTabeller(fødselsnummer: String = FNR): Long {
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

        return personRef
    }

}
