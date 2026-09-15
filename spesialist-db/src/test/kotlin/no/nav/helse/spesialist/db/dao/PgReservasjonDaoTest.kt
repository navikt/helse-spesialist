package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class PgReservasjonDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()

    @Test
    fun `reserverer person`() {
        val saksbehandler = opprettSaksbehandler()
        reservasjonDao.reserverPerson(saksbehandler.id.value, person.id.value)
        val saksbehandlerMedReservasjone =
            reservasjonDao.hentReservasjonFor(person.id.value)?.reservertTil
                ?: fail("Forventet at det skulle finnes en reservasjon i basen")
        assertEquals(saksbehandler.id.value, saksbehandlerMedReservasjone.id.value)
        assertRiktigVarighet(finnGyldigTil(person))
    }

    @Test
    fun `ny reservasjon forlenger ikke fristen`() {
        val saksbehandler = opprettSaksbehandler()
        val enAnnenSaksbehandler = UUID.randomUUID()
        saksbehandlerDao.opprettEllerOppdater(
            enAnnenSaksbehandler,
            "Siri Siksbehindler",
            "siri.siksbehindler@nav.no",
            "S666666",
        )

        reservasjonDao.reserverPerson(enAnnenSaksbehandler, person.id.value)
        val gyldigTil1 = finnGyldigTil(person)
        reservasjonDao.reserverPerson(saksbehandler.id.value, person.id.value)
        val gyldigTil2 = finnGyldigTil(person)
        assertTrue(gyldigTil2.isEqual(gyldigTil1))

        val saksbehandlerMedReservasjon =
            reservasjonDao.hentReservasjonFor(person.id.value)?.reservertTil
                ?: fail("Forventet at det skulle finnes en reservasjon i basen")
        assertEquals(saksbehandler.id.value, saksbehandlerMedReservasjon.id.value)
        assertRiktigVarighet(finnGyldigTil(person))
    }

    private fun assertRiktigVarighet(gyldigTil: LocalDateTime) {
        assertEquals(LocalDate.now().atTime(23, 59, 59), gyldigTil)
    }

    private fun finnGyldigTil(person: Person): LocalDateTime =
        dbQuery.single(
            """
            SELECT r.gyldig_til
            FROM reserver_person r
            JOIN person p ON p.id = r.person_ref
            WHERE p.fødselsnummer = :fnr AND r.gyldig_til > now()
            """,
            "fnr" to person.id.value,
        ) { it.localDateTime("gyldig_til") }
}
