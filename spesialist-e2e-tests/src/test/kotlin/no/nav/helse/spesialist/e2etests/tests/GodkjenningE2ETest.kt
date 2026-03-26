package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import no.nav.helse.spesialist.e2etests.behovløserstubs.FullmaktBehovLøser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GodkjenningE2ETest : AbstractE2EIntegrationTest() {

    @Test
    fun `oppretter vedtak ved godkjenningsbehov`() {
        søknadOgGodkjenningbehovKommerInn()

        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `oppretter ikke oppgave om bruker tilhører utlandsenhet`() {
        hentEnhetBehovLøser.enhet = "0393"

        søknadOgGodkjenningbehovKommerInn()

        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true, "Utland")
    }

    @Test
    fun `oppdaterer behandlingsinformasjon ved påminnet godkjenningsbehov`() {
        val tags1 = listOf("tag 1", "tag 2")
        val tags2 = listOf("tag 2", "tag 3")

        spleisIgnorererMeldinger()
        val vedtaksperiode = søknadOgGodkjenningbehovKommerInn(tags = tags1)
        assertBehandlingsinformasjon(vedtaksperiode, tags1)

        spleisSenderGodkjenningsbehov(vedtaksperiode, tags = tags2)
        assertBehandlingsinformasjon(vedtaksperiode, tags2)
    }

    @Test
    fun `legger ved ukjente organisasjonsnumre på behov for Arbeidsgiverinformasjon`() {
        val ekstraOrgnummer = lagOrganisasjonsnummer()

        søknadOgGodkjenningbehovKommerInn(
            orgnummereMedRelevanteArbeidsforhold = listOf(ekstraOrgnummer),
        )

        val arbeidsgiverinformasjonBehov =
            meldinger().first { melding ->
                melding["@event_name"].asText() == "behov" &&
                    melding["@behov"].any { it.asText() == "Arbeidsgiverinformasjon" } &&
                    !melding.has("@løsning")
            }
        val arbeidsgivere = arbeidsgiverinformasjonBehov["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
        assertEquals(
            setOf(ekstraOrgnummer, organisasjonsnummer()),
            arbeidsgivere.toSet(),
        )
    }

    @Test
    fun `skiller arbeidsgiverinformasjon- og personinfo-behov etter om det er et orgnr eller ikke`() {
        val orgnummer2 = lagOrganisasjonsnummer()
        val enkeltpersonforetak = lagFødselsnummer()

        søknadOgGodkjenningbehovKommerInn(
            orgnummereMedRelevanteArbeidsforhold = listOf(orgnummer2, enkeltpersonforetak),
        )

        val arbeidsgiverinformasjonBehov =
            meldinger().first { melding ->
                melding["@event_name"].asText() == "behov" &&
                    melding["@behov"].any { it.asText() == "Arbeidsgiverinformasjon" } &&
                    !melding.has("@løsning")
            }
        val arbeidsgivere = arbeidsgiverinformasjonBehov["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map { it.asText() }
        assertEquals(setOf(orgnummer2, organisasjonsnummer()), arbeidsgivere.toSet())

        val personinfoBehov =
            meldinger().first { melding ->
                melding["@event_name"].asText() == "behov" &&
                    melding["@behov"].any { it.asText() == "HentPersoninfoV2" } &&
                    !melding.has("@løsning") &&
                    melding["HentPersoninfoV2"]?.has("ident") == true
            }
        val enkeltpersonforetak2 = personinfoBehov["HentPersoninfoV2"]["ident"].map { it.asText() }
        assertEquals(listOf(enkeltpersonforetak), enkeltpersonforetak2)
    }

    @Test
    fun `legger til riktig felt for adressebeskyttelse i Personinfo`() {
        hentPersoninfoV2BehovLøser.adressebeskyttelse = "Fortrolig"
        saksbehandlerHarRolle(Brukerrolle.Kode7)

        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            assertAdressebeskyttelse("Fortrolig")
        }
    }

    @Test
    fun `avbryter saksbehandling og avviser godkjenning på person med verge`() {
        vergemålBehovLøser.vergemål = listOf(mapOf("type" to "voksen"))

        søknadOgGodkjenningbehovKommerInn()

        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true, "Vergemål")
    }

    @Test
    fun `avbryter ikke saksbehandling for person uten verge`() {
        søknadOgGodkjenningbehovKommerInn()

        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `avbryter ikke saksbehandling ved fullmakt eller fremtidsfullmakt`() {
        vergemålBehovLøser.fremtidsfullmakter = listOf(mapOf("type" to "voksen"))
        fullmaktBehovLøser.fullmakter = listOf(FullmaktBehovLøser.gyldigFullmakt())

        søknadOgGodkjenningbehovKommerInn()

        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `avbryter saksbehandling og avvise godkjenning pga vergemål`() {
        egenAnsattBehovLøser.erEgenAnsatt = true
        vergemålBehovLøser.vergemål = listOf(mapOf("type" to "voksen"))

        søknadOgGodkjenningbehovKommerInn()

        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true, "Vergemål")
    }

    @Test
    fun `avviser ikke godkjenningsbehov når kanAvvises-flagget er false`() {
        vergemålBehovLøser.vergemål = listOf(mapOf("type" to "mindreaarig"))

        søknadOgGodkjenningbehovKommerInn(kanAvvises = false)

        assertGjeldendeOppgavestatus("AvventerSaksbehandler")
    }

    @Test
    fun `flytt eventuelle aktive avviksvarsler til periode som nå er til godkjenning`() {
        leggTilVedtaksperiode()
        personSenderSøknad()

        val periode1 = førsteVedtaksperiode().also {
            it.fom = 1 jan 2018
            it.tom = 15 jan 2018
            it.skjæringstidspunkt = 1 jan 2018
        }
        val periode2 = andreVedtaksperiode().also {
            it.fom = 1 jan 2018
            it.tom = 31 jan 2018
            it.skjæringstidspunkt = 1 jan 2018
        }

        spleisForberederBehandling(periode1) {}
        spleisForberederBehandling(periode2) {
            aktivitetsloggNyAktivitet(listOf("RV_IV_2"))
        }

        risikovurderingBehovLøser.kanGodkjenneAutomatisk = true
        spleisSenderGodkjenningsbehov(
            periode1,
            perioderMedSammeSkjæringstidspunkt = listOf(periode1, periode2),
        )

        medPersonISpeil {
            assertVarselkoder(listOf("RV_IV_2"), periode1)
            assertVarselkoder(emptyList(), periode2)
        }
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom det eksisterer en aktiv oppgave`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        val vedtaksperiode = søknadOgGodkjenningbehovKommerInn()

        spleisSenderGodkjenningsbehov(vedtaksperiode)

        assertOppgavestatuserKronoligisk("AvventerSaksbehandler")
    }

    @Test
    fun `ignorerer påminnet godkjenningsbehov dersom vedtaket er automatisk godkjent`() {
        spleisIgnorererMeldinger()
        val vedtaksperiode = søknadOgGodkjenningbehovKommerInn()

        spleisSenderGodkjenningsbehov(vedtaksperiode)

        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
        val antallGodkjenningssvar = meldinger().count { it["@løsning"]?.get("Godkjenning") != null }
        assertEquals(1, antallGodkjenningssvar)
    }

    @Test
    fun `oppdaterer skjæringstidspunkt på AUU-behandlinger når senere periode går til godkjenning`() {
        leggTilVedtaksperiode()
        personSenderSøknad()

        val periode1 = førsteVedtaksperiode().also {
            it.fom = 10 jan 2018
            it.tom = 19 jan 2018
            it.skjæringstidspunkt = 10 jan 2018
        }
        val periode2 = andreVedtaksperiode().also {
            it.fom = 20 jan 2018
            it.tom = 29 jan 2018
            it.skjæringstidspunkt = 15 jan 2018
        }

        spleisForberederBehandling(periode1) {}
        spleisAvslutterUtenVedtak(periode1)

        spleisForberederBehandling(periode2) {}
        spleisSenderGodkjenningsbehov(
            periode2,
            perioderMedSammeSkjæringstidspunkt = listOf(periode1, periode2),
        )

        assertSkjæringstidspunktForBehandling(periode1, 15 jan 2018)
    }

    @Test
    fun `flytter varsel fra en AUVMV, AKA AUU, også når skjæringstidspunktet er flyttet`() {
        leggTilVedtaksperiode()
        personSenderSøknad()

        val periode1 = førsteVedtaksperiode().also {
            it.fom = 10 jan 2018
            it.tom = 19 jan 2018
            it.skjæringstidspunkt = 10 jan 2018
        }
        val periode2 = andreVedtaksperiode().also {
            it.fom = 20 jan 2018
            it.tom = 29 jan 2018
            it.skjæringstidspunkt = 15 jan 2018
        }

        spleisForberederBehandling(periode1) {
            aktivitetsloggNyAktivitet(listOf("RV_YS_1"))
        }
        spleisAvslutterUtenVedtak(periode1)

        spleisForberederBehandling(periode2) {}
        spleisSenderGodkjenningsbehov(
            periode2,
            perioderMedSammeSkjæringstidspunkt = listOf(periode1, periode2),
        )

        assertGjeldendeOppgavestatus("AvventerSaksbehandler", periode2)
    }
}
