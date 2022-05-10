package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class AdressebeskyttelseEndretE2ETest : AbstractE2ETest() {
    @Test
    fun `oppdaterer adressebeskyttelse på en person vi kjenner til fra før`() {
        vedtaksperiode(utbetalingId = UUID.randomUUID())
        val originaleBehov = testRapid.inspektør.behov().size
        val hendelseId = sendAdressebeskyttelseEndret()

        assertEquals(originaleBehov + 1, testRapid.inspektør.behov().size)
        assertEquals(Adressebeskyttelse.Ugradert, personDao.findAdressebeskyttelse(FØDSELSNUMMER))
        sendHentPersoninfoLøsning(hendelseId, adressebeskyttelse = "Fortrolig")
        assertEquals(Adressebeskyttelse.Fortrolig, personDao.findAdressebeskyttelse(FØDSELSNUMMER))
    }
    @Test
    fun `oppdaterer ikke adressebeskyttelse dersom vi ikke kjenner til fødselsnummer`() {
        sendAdressebeskyttelseEndret()
        assertEquals(emptyList<String>(), testRapid.inspektør.behov())
    }
}
