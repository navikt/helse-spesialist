package no.nav.helse.modell.vedtaksperiode

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.OVERFØRT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertNotNull

internal class PersonMediatorTest : AbstractE2ETest() {
    private companion object {
        private const val ORGNR2 = "13370420"
        private val ID = UUID.randomUUID()
    }

    private val funn = listOf(
        Risikofunn(kategori = listOf("8-4"), beskrivele = "8-4 ikke ok", kreverSupersaksbehandler = false),
        Risikofunn(kategori = emptyList(), beskrivele = "faresignal ikke ok", kreverSupersaksbehandler = false)
    )

    @BeforeEach
    fun setup() {
        every { restClient.hentSpeilSnapshot(any()) } returns TEST_SNAPSHOTV1
    }

    @Test
    fun `manglende risikovurdering mappes ikke til speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        assertTrue(
            speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("risikovurdering").isMissingOrNull()
        )
    }

    @Test
    fun `inntektsgrunnlag på personnivå blir faktisk tatt med til speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        assertFalse(speilSnapshot.inntektsgrunnlag.isNull)
        assertEquals(speilSnapshot.inntektsgrunnlag.first()["skjæringstidspunkt"].textValue(), "2018-01-01")
        assertEquals(speilSnapshot.inntektsgrunnlag.first()["sykepengegrunnlag"].doubleValue(), 581298.0)
    }

    @Test
    fun `En satt risikovurdering mappes til speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
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
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
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
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

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
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
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
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
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
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        val varsler = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("varsler")

        assertEquals(2, varsler.size())
    }

    @Test
    fun `Ingen warnings mappes til speil som tom liste`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
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
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        val varsler = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("varsler")

        assertTrue(varsler.isEmpty)
    }

    @Test
    fun `om en person har utbetalinger blir dette en del av speil snapshot`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
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

        sendUtbetalingEndret("UTBETALING", OVERFØRT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        val speilSnapshot1 = assertNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)
        assertEquals(1, speilSnapshot1.utbetalinger.size)
        val utbetaling1 = speilSnapshot1.utbetalinger.first()
        assertEquals("OVERFØRT", utbetaling1.status)

        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, arbeidsgiverFagsystemId, utbetalingId = UTBETALING_ID)
        val speilSnapshot2 = assertNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)
        val utbetaling2 = speilSnapshot2.utbetalinger.first()
        assertEquals("UTBETALT", utbetaling2.status)
        assertEquals("UTBETALING", utbetaling2.type)
        assertEquals(utbetaling1.arbeidsgiveroppdrag, utbetaling2.arbeidsgiveroppdrag)
        assertEquals(4000, utbetaling1.totalbeløp)

        assertEquals(utbetaling2.arbeidsgiveroppdrag?.fagsystemId, arbeidsgiverFagsystemId)
        assertEquals(ORGNR, utbetaling2.arbeidsgiveroppdrag?.mottaker)
        assertEquals(2, utbetaling2.arbeidsgiveroppdrag?.utbetalingslinjer?.size)
        assertEquals(4000, utbetaling2.totalbeløp)
    }

    @Test
    fun `om en annen person har utbetalinger blir ikke det med i snapshot for denne personen`() {
        val fødselsnummer1 = "12345789101"
        val aktørId1 = "100000000010000"
        val orgnr1 = "987654321"
        val vedtaksperiodeId1 = UUID.randomUUID()

        val godkjenningsmeldingId1 =
            sendGodkjenningsbehov(
                orgnr1,
                vedtaksperiodeId1,
                fødselsnummer = fødselsnummer1,
                aktørId = aktørId1,
                utbetalingId = UTBETALING_ID
            )
        sendPersoninfoløsning(godkjenningsmeldingId1, orgnr1, vedtaksperiodeId1)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId1,
            orgnummer = orgnr1,
            vedtaksperiodeId = vedtaksperiodeId1
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId1,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendUtbetalingEndret(
            type = "UTBETALING",
            status = OVERFØRT,
            orgnr = orgnr1,
            arbeidsgiverFagsystemId = "JHKSDA3412SFHJKA489KASDJL",
            fødselsnummer = fødselsnummer1,
            utbetalingId = UTBETALING_ID
        )

        val fødselsnummer2 = "23456789102"
        val aktørId2 = "100000000010123"
        val orgnr2 = "876543219"
        val vedtaksperiodeId2 = UUID.randomUUID()
        val godkjenningsmeldingId2 =
            sendGodkjenningsbehov(
                orgnr2,
                vedtaksperiodeId2,
                fødselsnummer = fødselsnummer2,
                aktørId = aktørId2,
                utbetalingId = UTBETALING_ID
            )
        sendPersoninfoløsning(godkjenningsmeldingId2, orgnr2, vedtaksperiodeId2)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId2,
            orgnummer = orgnr2,
            vedtaksperiodeId = vedtaksperiodeId2
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId2,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )

        val speilSnapshot1 = assertNotNull(personMediator.byggSpeilSnapshotForFnr(fødselsnummer1, false).snapshot)
        assertEquals(1, speilSnapshot1.utbetalinger.size)
        val speilSnapshot2 = assertNotNull(personMediator.byggSpeilSnapshotForFnr(fødselsnummer2, false).snapshot)
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

        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
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
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

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
        val bransjer = listOf("En bransje", "En annen bransje")
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
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
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        assertEquals(bransjer, speilSnapshot.arbeidsgivere.first().bransjer)
    }

    @Test
    fun `Mapper arbeidsgiverinfo for flere arbeidsgivere`() {
        val aktiveVedtaksperioder = listOf(
            Testmeldingfabrikk.AktivVedtaksperiodeJson(
                ORGNR,
                VEDTAKSPERIODE_ID,
                Periodetype.OVERGANG_FRA_IT
            ),
            Testmeldingfabrikk.AktivVedtaksperiodeJson(
                ORGNR2,
                UUID.randomUUID(),
                Periodetype.OVERGANG_FRA_IT
            )
        )
        val godkjenningsmeldingId =
            sendGodkjenningsbehov(
                ORGNR,
                VEDTAKSPERIODE_ID,
                aktiveVedtaksperioder = aktiveVedtaksperioder,
                utbetalingId = UTBETALING_ID
            )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            ekstraArbeidsgivere = listOf(
                Testmeldingfabrikk.ArbeidsgiverinformasjonJson(ORGNR2, "Eliteserien", listOf("Fotball"))
            )
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        assertEquals("Eliteserien", speilSnapshot.arbeidsgivere.last().navn)
    }

    @Test
    fun `saksbehandleroid på snapshot`() {
        val saksbehandlerOid = UUID.randomUUID()
        val saksbehandlerEpost = "saksbehandler@nav.no"
        val saksbehandlerIdent = "Z999999"
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsforholdløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendEgenAnsattløsning(godkjenningsmeldingId, false)
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(godkjenningsmeldingId, true)
        sendÅpneGosysOppgaverløsning(godkjenningsmeldingId)
        sendRisikovurderingløsning(
            godkjenningsmeldingId,
            VEDTAKSPERIODE_ID,
            funn = funn,
            kanGodkjennesAutomatisk = false
        )
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerOid, "Navn Navnesen", saksbehandlerEpost, saksbehandlerIdent)
        tildelingDao.opprettTildeling(testRapid.inspektør.oppgaveId(), saksbehandlerOid)
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        assertEquals(saksbehandlerOid, speilSnapshot.tildeling?.oid)
        assertEquals(saksbehandlerEpost, speilSnapshot.tildeling?.epost)
        assertEquals(false, speilSnapshot.tildeling?.påVent)
    }

    @Test
    fun `Ugradert adressebeskyttelse - alle saksbehandlere kan søke opp personer`() {
        vedtak(Adressebeskyttelse.Ugradert)
        assertNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        vedtak(Adressebeskyttelse.Ugradert)
        assertNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, true).snapshot)
    }

    @Test
    fun `Fortrolig adressebeskyttelse - kun saksbehandler med egen tilgang til kode 7 søke opp personer`() {
        vedtak(Adressebeskyttelse.Fortrolig)
        assertNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        vedtak(Adressebeskyttelse.Fortrolig)
        assertNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, true).snapshot)
    }

    @Test
    fun `Strengt fortrolig adressebeskyttelse - ingen saksbehandlere skal kunne søke opp personer`() {
        vedtak(Adressebeskyttelse.StrengtFortrolig)
        assertNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        vedtak(Adressebeskyttelse.StrengtFortrolig)
        assertNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, true).snapshot)
    }

    @Test
    fun `Strengt fortrolig utland adressebeskyttelse - ingen saksbehandlere skal kunne søke opp personer`() {
        vedtak(Adressebeskyttelse.StrengtFortroligUtland)
        assertNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        vedtak(Adressebeskyttelse.StrengtFortroligUtland)
        assertNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, true).snapshot)
    }

    @Test
    fun `Ukjent adressbeskyttelse - ingen saksbehandlere skal kunne søke opp personer`() {
        vedtak(Adressebeskyttelse.Ukjent)
        assertNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)

        vedtak(Adressebeskyttelse.Ukjent)
        assertNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, true).snapshot)
    }

    @Test
    fun `Ugradert adressebeskyttelse mappes til speil`() {
        vedtak(Adressebeskyttelse.Ugradert)
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, false).snapshot)
        assertEquals(Adressebeskyttelse.Ugradert, speilSnapshot.personinfo.adressebeskyttelse)
    }

    @Test
    fun `Fortrolig adressebeskyttelse mappes til speil`() {
        vedtak(Adressebeskyttelse.Fortrolig)
        val speilSnapshot = requireNotNull(personMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER, true).snapshot)
        assertEquals(Adressebeskyttelse.Fortrolig, speilSnapshot.personinfo.adressebeskyttelse)
    }

    private fun vedtak(adressebeskyttelse: Adressebeskyttelse) {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            adressebeskyttelse = adressebeskyttelse.name
        )
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
    }

    @Language("JSON")
    private val TEST_SNAPSHOTV1 = """{
  "versjon": 1,
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
      ],
      "utbetalingshistorikk": []
    },
    {
      "id": "${UUID.randomUUID()}",
      "organisasjonsnummer": "$ORGNR2",
      "vedtaksperioder": [
        {
          "id": "${UUID.randomUUID()}"
        }
      ],
      "utbetalingshistorikk": []
    }
  ],
  "inntektsgrunnlag": [
    {
      "skjæringstidspunkt": "2018-01-01",
      "sykepengegrunnlag": 581298.0
    }
  ]
}
        """
}
