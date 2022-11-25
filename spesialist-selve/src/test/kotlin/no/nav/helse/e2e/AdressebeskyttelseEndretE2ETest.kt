package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.Meldingssender
import no.nav.helse.Meldingssender.sendAdressebeskyttelseEndret
import no.nav.helse.Meldingssender.sendHentPersoninfoLøsning
import no.nav.helse.TestRapidHelpers.behov
import no.nav.helse.TestRapidHelpers.hendelseId
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

    @Test
    fun `Etterspør personinfo uten å sjekke ferskhet når adressebeskyttelse har blitt endret`() {
        settOppBruker()
        testRapid.reset()

        // Etterspør personinfo selv om det nettopp er gjort
        sendAdressebeskyttelseEndret()
        assertBehov("HentPersoninfoV2")
    }

    @Test
    fun `Etterspør personinfo men avviser ikke når det ikke er noen åpne oppgaver`() {
        settOppBruker()

        sendSaksbehandlerløsningFraAPI(testRapid.inspektør.oppgaveId(), "ident", "epost", UUID.randomUUID(), false)
        Meldingssender.sendUtbetalingEndret(
            aktørId = Testdata.AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = Testdata.ORGNR,
            utbetalingId = Testdata.UTBETALING_ID,
            type = "UTBETALING",
            status = Utbetalingsstatus.UTBETALT,
        )
        testRapid.reset()
        assertOppgaver(0)

        sendAdressebeskyttelseEndret()
        assertBehov("HentPersoninfoV2")
        val hendelseId = testRapid.inspektør.hendelseId()
        sendHentPersoninfoLøsning(hendelseId, adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig.name)
        assertEquals(0, testRapid.inspektør.hendelser("vedtaksperiode_avvist").size)
    }

    @Test
    fun `Behandler ny strengt fortrolig adressebeskyttelse og avviser`() {
        settOppBruker()
        sendAdressebeskyttelseEndret()
        val hendelseId = testRapid.inspektør.hendelseId()
        sendHentPersoninfoLøsning(hendelseId, adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig.name)

        assertVedtaksperiodeAvvist(
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING.name,
            begrunnelser = listOf("Adressebeskyttelse strengt fortrolig"),
            kommentar = null)
    }

    @Test
    fun `Behandler ny fortrolig adressebeskyttelse og avviser ikke`() {
        settOppBruker()
        sendAdressebeskyttelseEndret()
        val hendelseId = testRapid.inspektør.hendelseId()
        sendHentPersoninfoLøsning(hendelseId, adressebeskyttelse = Adressebeskyttelse.Fortrolig.name)
        assertEquals(0, testRapid.inspektør.hendelser("vedtaksperiode_avvist").size)
    }
}
