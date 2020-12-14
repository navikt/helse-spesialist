package no.nav.helse.modell.vedtaksperiode

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNotNull

internal class VedtaksperiodeMediatorTest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private val ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        every { restClient.hentSpeilSpapshot(any()) } returns SNAPSHOTV1
    }

    @AfterEach
    fun tearDown() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(false)
    }

    @Test
    fun `manglende risikovurdering mappes ikke til speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        assertTrue(
            speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("risikovurdering").isMissingOrNull()
        )
    }

    @Test
    fun `En satt risikovurdering mappes til speil`() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val risikovurdering = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("risikovurdering")

        assertEquals(false, risikovurdering["ufullstendig"].booleanValue())
        assertTrue(risikovurdering["arbeidsuførhetvurdering"].isEmpty)
    }

    @Test
    fun `Warnings mappes til speil som varsler`() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = "En eller flere bransjer"
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            begrunnelser = listOf("8-4 ikke oppfylt")
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val varsler = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("varsler")

        assertEquals(1, varsler.size())
    }

    @Test
    fun `Ingen warnings mappes til speil som tom liste`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val varsler = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("varsler")

        assertTrue(varsler.isEmpty)
    }

    @Test
    fun `om en person har utbetalinger blir dette en del av speil snapshot`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val arbeidsgiverFagsystemId = "JHKSDA3412SFHJKA489KASDJL"

        sendUtbetalingEndret("UTBETALING", "OVERFØRT", ORGNR, arbeidsgiverFagsystemId)
        val speilSnapshot1 = assertNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(UNG_PERSON_FNR_2018))
        assertEquals(1, speilSnapshot1.utbetalinger.size)
        val utbetaling1 = speilSnapshot1.utbetalinger.first()
        assertEquals("OVERFØRT", utbetaling1.status)

        sendUtbetalingEndret("UTBETALING", "UTBETALT", ORGNR, arbeidsgiverFagsystemId)
        val speilSnapshot2 = assertNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(UNG_PERSON_FNR_2018))
        val utbetaling2 = speilSnapshot2.utbetalinger.first()
        assertEquals("OVERFØRT", utbetaling2.status)
        assertEquals(utbetaling1.arbeidsgiverOppdrag, utbetaling2.arbeidsgiverOppdrag)
        assertEquals(utbetaling2.arbeidsgiverOppdrag.fagsystemId, arbeidsgiverFagsystemId)
        assertEquals(2, utbetaling2.arbeidsgiverOppdrag.utbetalingslinjer.size)
    }

    @Test
    fun `om en annen person har utbetalinger blir ikke det med i snapshot for denne personen`() {
        val fødselsnummer1 = "12345789101"
        val aktørId1 = "100000000010000"
        val orgnr1 = "987654321"
        val vedtaksperiodeId1 = UUID.randomUUID()

        val godkjenningsmeldingId1 = sendGodkjenningsbehov(orgnr1, vedtaksperiodeId1, fødselsnummer = fødselsnummer1, aktørId = aktørId1)
        sendPersoninfoløsning(godkjenningsmeldingId1, orgnr1, vedtaksperiodeId1)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId1,
            orgnr = orgnr1,
            vedtaksperiodeId = vedtaksperiodeId1
        )
        sendUtbetalingEndret(
            type = "UTBETALING",
            status = "OVERFØRT",
            fødselsnummer = fødselsnummer1,
            orgnr = orgnr1,
            arbeidsgiverFagsystemId = "JHKSDA3412SFHJKA489KASDJL"
        )

        val fødselsnummer2 = "23456789102"
        val aktørId2 = "100000000010123"
        val orgnr2 = "876543219"
        val vedtaksperiodeId2 = UUID.randomUUID()
        val godkjenningsmeldingId2 = sendGodkjenningsbehov(orgnr2, vedtaksperiodeId2, fødselsnummer = fødselsnummer2, aktørId = aktørId2)
        sendPersoninfoløsning(godkjenningsmeldingId2, orgnr2, vedtaksperiodeId2)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId2,
            orgnr = orgnr2,
            vedtaksperiodeId = vedtaksperiodeId2
        )

        val speilSnapshot1 = assertNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(fødselsnummer1))
        assertEquals(1, speilSnapshot1.utbetalinger.size)
        val speilSnapshot2 = assertNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(fødselsnummer2))
        assertEquals(0, speilSnapshot2.utbetalinger.size)
    }

    private val SNAPSHOTV1 = """
        {
            "aktørId": "$AKTØR",
            "fødselsnummer": "$FØDSELSNUMMER",
            "arbeidsgivere": [
                {
                    "id": "$ID",
                    "organisasjonsnummer": "$ORGNR",
                    "vedtaksperioder": [
                        {
                            "id": "$VEDTAKSPERIODE_ID"
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

}
