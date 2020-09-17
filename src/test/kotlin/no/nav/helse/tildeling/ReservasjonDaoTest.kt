package no.nav.helse.tildeling

import AbstractEndToEndTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.sessionOf
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.saksbehandler.persisterSaksbehandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ReservasjonDaoTest : AbstractEndToEndTest() {

    private companion object {
        private const val FNR = "12345678911"
        private val objectMapper = jacksonObjectMapper()
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
    }

    private lateinit var reservasjonDao: ReservasjonDao

    @BeforeEach
    fun setup() {
        personDao = PersonDao(dataSource)
        reservasjonDao = ReservasjonDao(dataSource)
        snapshotDao = SnapshotDao(dataSource)

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

    private fun opprettTabeller(): Long {
        val personinfoRef = personDao.insertPersoninfo(
            "KARI",
            "Mellomnavn",
            "Nordmann",
            LocalDate.EPOCH,
            Kjønn.Kvinne
        )
        val utbetalingerRef = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val personRef = personDao.insertPerson(
            FNR,
            "4321098765432", personinfoRef, "0301".toInt(), utbetalingerRef
        ) ?: org.junit.jupiter.api.fail { "Kunne ikke opprette person" }

        sessionOf(dataSource).use {
            it.persisterSaksbehandler(SAKSBEHANDLER_OID, "Sara Saksbehandler", "sara.saksbehandler@nav.no")
        }

        return personRef.toLong()
    }

}
