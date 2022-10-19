package no.nav.helse.e2e

import AbstractE2ETest
import ToggleHelpers.disable
import ToggleHelpers.enable
import no.nav.helse.Meldingssender.sendVedtaksperiodeEndret
import no.nav.helse.Testdata
import no.nav.helse.mediator.Toggle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
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
    fun `vedtaksperiode_endret oppretter ny vedtaksperiode generasjon, når det ikke finnes eksisterende generasjoner`() {
        settOppBruker() // TODO Dette skal bort når vi oppretter personer før godkjenningsbehovet kommer inn
        sendVedtaksperiodeEndret(Testdata.ORGNR, Testdata.VEDTAKSPERIODE_ID)
        val førsteGenerasjon = generasjonDao.generasjon(Testdata.VEDTAKSPERIODE_ID)
        assertNotNull(førsteGenerasjon)
    }
}