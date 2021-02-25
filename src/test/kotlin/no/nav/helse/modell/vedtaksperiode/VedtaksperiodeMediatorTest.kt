package no.nav.helse.modell.vedtaksperiode

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
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

    @Language("json")
    private val funn = objectMapper.readTree("""
            [{
                "kategori": ["8-4"],
                "beskrivelse": "8-4 ikke ok",
                "kreverSupersaksbehandler": false
            },{
                "kategori": [],
                "beskrivelse": "faresignal ikke ok",
                "kreverSupersaksbehandler": false
            }]
        """)

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
    fun `inntektsgrunnlag på personnivå blir faktisk tatt med til speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        assertFalse(speilSnapshot.inntektsgrunnlag.isNull)
        assertEquals(speilSnapshot.inntektsgrunnlag.first()["skjæringstidspunkt"].textValue(), "2018-01-01")
        assertEquals(speilSnapshot.inntektsgrunnlag.first()["sykepengegrunnlag"].doubleValue(), 581298.0)
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
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            funn = funn
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val risikovurdering = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("risikovurdering")

        assertEquals("8-4 ikke ok", risikovurdering["funn"].first()["beskrivelse"].asText())
        assertEquals("faresignal ikke ok", risikovurdering["funn"].last()["beskrivelse"].asText())
        assertTrue(risikovurdering["kontrollertOk"].isEmpty)

        // Bakoverkompatibilitet
        assertEquals("8-4 ikke ok", risikovurdering["arbeidsuførhetvurdering"].first().asText())
        assertFalse(risikovurdering["ufullstendig"].asBoolean())
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
            kanGodkjennesAutomatisk = false,
            funn = funn
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val varsler = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("varsler")

        assertEquals(2, varsler.size())
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
        assertEquals("UTBETALING", utbetaling2.type)
        assertEquals(utbetaling1.arbeidsgiverOppdrag, utbetaling2.arbeidsgiverOppdrag)
        assertEquals(utbetaling2.arbeidsgiverOppdrag.fagsystemId, arbeidsgiverFagsystemId)
        assertEquals(ORGNR, utbetaling2.arbeidsgiverOppdrag.organisasjonsnummer)
        assertEquals(2, utbetaling2.arbeidsgiverOppdrag.utbetalingslinjer.size)
    }

    @Test
    fun `om en annen person har utbetalinger blir ikke det med i snapshot for denne personen`() {
        val fødselsnummer1 = "12345789101"
        val aktørId1 = "100000000010000"
        val orgnr1 = "987654321"
        val vedtaksperiodeId1 = UUID.randomUUID()

        val godkjenningsmeldingId1 =
            sendGodkjenningsbehov(orgnr1, vedtaksperiodeId1, fødselsnummer = fødselsnummer1, aktørId = aktørId1)
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
        val godkjenningsmeldingId2 =
            sendGodkjenningsbehov(orgnr2, vedtaksperiodeId2, fødselsnummer = fødselsnummer2, aktørId = aktørId2)
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

    @Test
    fun `mapper arbeidsforhold`() {
        val arbeidsforholdløsning = listOf(
            Arbeidsforholdløsning.Løsning(
                stillingstittel = "Sykepleier",
                stillingsprosent = 100,
                startdato = LocalDate.now().minusYears(2),
                sluttdato = LocalDate.now().minusYears(1)
            ),
            Arbeidsforholdløsning.Løsning(
                stillingstittel = "Avdelingsleder",
                stillingsprosent = 100,
                startdato = LocalDate.now().minusYears(1),
                sluttdato = null
            )
        )

        every { miljøstyrtFeatureToggle.arbeidsforhold() } returns true
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            løsning = arbeidsforholdløsning
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
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        arbeidsforholdløsning[0].also {
            assertEquals(it.stillingstittel, speilSnapshot.arbeidsforhold[0].stillingstittel)
            assertEquals(it.stillingsprosent, speilSnapshot.arbeidsforhold[0].stillingsprosent)
            assertEquals(it.startdato, speilSnapshot.arbeidsforhold[0].startdato)
            assertEquals(it.sluttdato, speilSnapshot.arbeidsforhold[0].sluttdato)
        }

        arbeidsforholdløsning[1].also {
            assertEquals(it.stillingstittel, speilSnapshot.arbeidsforhold[1].stillingstittel)
            assertEquals(it.stillingsprosent, speilSnapshot.arbeidsforhold[1].stillingsprosent)
            assertEquals(it.startdato, speilSnapshot.arbeidsforhold[1].startdato)
            assertEquals(it.sluttdato, speilSnapshot.arbeidsforhold[1].sluttdato)
        }
    }

    @Test
    fun `mapper bransjer for arbeidsgiver`() {
        every { miljøstyrtFeatureToggle.arbeidsgiverinformasjon() } returns true
        val bransjer = listOf("En bransje", "En annen bransje")
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            bransjer = bransjer
        )
        sendArbeidsforholdløsning(
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
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        assertEquals(bransjer, speilSnapshot.arbeidsgivere.first().bransjer)
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
            ],
            "inntektsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-01-01",
                    "sykepengegrunnlag": 581298.0
                }
            ]
        }
        """.trimIndent()

}
