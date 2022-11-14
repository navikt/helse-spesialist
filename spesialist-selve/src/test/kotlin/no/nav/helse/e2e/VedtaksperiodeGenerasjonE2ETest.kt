package no.nav.helse.e2e

import AbstractE2ETest
import ToggleHelpers.disable
import ToggleHelpers.enable
import no.nav.helse.Meldingssender.sendSøknadSendt
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.Toggle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeGenerasjonE2ETest : AbstractE2ETest() {

    @BeforeEach
    fun før() {
        Toggle.VedtaksperiodeGenerasjoner.enable()
    }

    @AfterEach
    fun etter() {
        Toggle.VedtaksperiodeGenerasjoner.disable()
    }

    @Test
    fun `vedtaksperiode_endret oppretter ny generasjon når det ikke finnes eksisterende generasjoner og forrigeTilstand er 'START'`() {
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, "START")
        val førsteGenerasjon = generasjonDao.generasjon(VEDTAKSPERIODE_ID)

        assertNotNull(førsteGenerasjon)
    }

    @Test
    fun `vedtaksperiode_endret oppretter ikke ny generasjon når det ikke finnes eksisterende generasjoner og forrigeTilstand ikke er 'START'`() {
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, "ANNEN_TILSTAND")
        val førsteGenerasjon = generasjonDao.generasjon(VEDTAKSPERIODE_ID)

        assertNull(førsteGenerasjon)
    }

    @Test
    fun `vedtaksperiode_endret oppretter ikke ny generasjon når det finnes eksisterende ulåst generasjon`() {
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, "START")
        val førsteGenerasjon = generasjonDao.generasjon(VEDTAKSPERIODE_ID)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        val skalFortsattVæreførsteGenerasjon = generasjonDao.generasjon(VEDTAKSPERIODE_ID)

        assertEquals(skalFortsattVæreførsteGenerasjon, førsteGenerasjon)
    }

    @Test
    fun `vedtak_fattet låser generasjon, ny vedtaksperiode_endret vil da opprette ny generasjon`() {
        sendSøknadSendt(AKTØR, FØDSELSNUMMER, ORGNR)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, "START")
        val førsteGenerasjon = generasjonDao.generasjon(VEDTAKSPERIODE_ID)
        sendVedtakFattet(FØDSELSNUMMER, VEDTAKSPERIODE_ID)
        sendVedtaksperiodeEndret(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID)
        val andreGenerasjon = generasjonDao.generasjon(VEDTAKSPERIODE_ID)

        assertNotNull(førsteGenerasjon)
        assertNotNull(andreGenerasjon)
        assertNotEquals(førsteGenerasjon, andreGenerasjon)
    }
}