package no.nav.helse.spesialist.application.modell

import io.mockk.mockk
import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.Automatiseringsresultat
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver.Configuration
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.stoppautomatiskbehandling.VeilederStansSubsumsjonmelder
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.kommando.ApplicationTest
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.*
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import no.nav.helse.spesialist.domain.testfixtures.des
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.JsonNodeFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class AutomatiseringTest : ApplicationTest() {
    private val utbetalingId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()

    private val veilederStansSubsumsjonmelder = VeilederStansSubsumsjonmelder { mockk(relaxed = true) }
    private var stikkprøveFullRefusjonEnArbeidsgiver = false
    private var stikkprøveUtsEnArbeidsgiverFørstegangsbehandling = false
    private var stikkprøveUtsEnArbeidsgiverForlengelse = false
    private var stikkprøveSelvstendigNæringsdrivendeForlengelse = false
    private val stikkprøver =
        Stikkprøver(
            object : Configuration {
                override fun utsFlereArbeidsgivereFørstegangsbehandling() = false

                override fun utsFlereArbeidsgivereForlengelse() = false

                override fun selvstendigNæringsdrivendeForlengelse() = stikkprøveSelvstendigNæringsdrivendeForlengelse

                override fun utsEnArbeidsgiverFørstegangsbehandling() = stikkprøveUtsEnArbeidsgiverFørstegangsbehandling

                override fun utsEnArbeidsgiverForlengelse() = stikkprøveUtsEnArbeidsgiverForlengelse

                override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false

                override fun fullRefusjonFlereArbeidsgivereForlengelse() = false

                override fun fullRefusjonEnArbeidsgiver() = stikkprøveFullRefusjonEnArbeidsgiver
            },
        )

    private val automatisering =
        Automatisering(
            veilederStansSubsumsjonmelder = veilederStansSubsumsjonmelder,
            stikkprøver = stikkprøver,
            sessionContext = sessionContext,
        )

    @BeforeEach
    fun setupDefaultTilHappyCase() {
        sessionContext.risikovurderingDao.lagre(vedtaksperiode1.id.value, true, JsonNodeFactory.instance.objectNode(), LocalDateTime.now())
        sessionContext.vedtakDao.leggTilVedtaksperiodetype(vedtaksperiode1.id.value, FØRSTEGANGSBEHANDLING, Inntektskilde.EN_ARBEIDSGIVER)
        sessionContext.åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(ÅpneGosysOppgaverDto(person.id.value, 0, false, LocalDateTime.now()))
        sessionContext.legacyBehandlingDao.settFørsteLegacyBehandlingVedtakFattetTidspunkt(
            vedtaksperiode1.id.value,
            LocalDateTime.now().minusMonths(6).plusDays(1),
        )
        stikkprøveFullRefusjonEnArbeidsgiver = false
        stikkprøveUtsEnArbeidsgiverForlengelse = false
    }

    @Test
    fun `tvinger automatisering hvis vedtaksperiodeId ligger i force_automatisering tabellen`() {
        sessionContext.automatiseringDao.tvingAutomatisering(vedtaksperiode1.id.value)
        blirAutomatiskBehandlet()
    }

    @Test
    fun `vedtaksperiode som oppfyller krav blir automatisk godkjent og lagret`() {
        blirAutomatiskBehandlet(enUtbetaling())
    }

    @Test
    fun `vedtaksperiode med warnings er ikke automatiserbar`() {
        val gjeldendeBehandling = enBehandling()
        sessionContext.varselRepository.lagre(Varsel.nytt(gjeldendeBehandling.id, gjeldendeBehandling.spleisBehandlingId, "RV_IM_1"))
        blirManuellOppgave(gjeldendeBehandling = gjeldendeBehandling)
    }

    @Test
    fun `vedtaksperiode med 2 tidligere korrigerte søknader er ikke automatiserbar`() {
        val gjeldendeBehandling = enBehandling()
        sessionContext.meldingDao.registrerBehandlingOpprettetKorrigertSøknad(
            person.id.value,
            vedtaksperiode1.id.value,
            hendelseId,
        )
        repeat(3) {
            sessionContext.meldingDao.opprettAutomatiseringMedKorrigertSøknad(vedtaksperiode1.id.value, UUID.randomUUID())
        }
        blirManuellOppgaveMedFeilOgVarsel(
            gjeldendeBehandling = gjeldendeBehandling,
            problems = listOf("Antall ganger vedtaksperioden er automatisk godkjent med korrigert søknad er to eller mer"),
            varselkode = Varselkode.SB_SØ_1,
        )
    }

    @Test
    fun `vedtaksperiode som mottok første søknad for mer enn 6 måneder er ikke automatiserbar`() {
        sessionContext.meldingDao.registrerBehandlingOpprettetKorrigertSøknad(
            person.id.value,
            vedtaksperiode1.id.value,
            hendelseId,
        )
        sessionContext.legacyBehandlingDao.settFørsteLegacyBehandlingVedtakFattetTidspunkt(
            vedtaksperiode1.id.value,
            LocalDateTime.now().minusMonths(6),
        )
        blirManuellOppgaveMedFeil(problems = listOf("Mer enn 6 måneder siden vedtak på første mottatt søknad"))
    }

    @Test
    fun `Automatisering av korrigert søknad er allerede håndtert for tidligere sykefraværstilfelle`() {
        sessionContext.meldingDao.registrerBehandlingOpprettetKorrigertSøknad(
            person.id.value,
            vedtaksperiode1.id.value,
            hendelseId,
        )
        sessionContext.meldingDao.opprettAutomatiseringMedKorrigertSøknad(vedtaksperiode1.id.value, hendelseId)
        blirAutomatiskBehandlet()
    }

    @Test
    fun `vedtaksperiode uten ok risikovurdering er ikke automatiserbar`() {
        sessionContext.risikovurderingDao.lagre(vedtaksperiode1.id.value, false, JsonNodeFactory.instance.objectNode(), LocalDateTime.now())
        blirManuellOppgave()
    }

    @Test
    fun `vedtaksperiode med null risikovurdering er ikke automatiserbar`() {
        sessionContext.risikovurderingDao.slett(vedtaksperiode1.id.value)
        blirManuellOppgave()
    }

    @Test
    fun `vedtaksperiode med nådd maksdato og refusjon fra AG er ikke automatiserbar`() {
        blirManuellOppgaveMedFeilOgVarsel(
            gjeldendeBehandling = enBehandling(skjæringstidspunkt = 1 jan 2018),
            tags = listOf("ArbeidsgiverØnskerRefusjon"),
            maksdato = 1 des 2017,
            varselkode = Varselkode.RV_OV_5,
            problems = listOf("Nådd maksdato og har refusjon til arbeidsgiver"),
        )
    }

    @Test
    fun `vedtaksperiode med åpne oppgaver er ikke automatiserbar`() {
        sessionContext.åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(ÅpneGosysOppgaverDto(person.id.value, 1, false, LocalDateTime.now()))
        blirManuellOppgave()
    }

    @Test
    fun `vedtaksperiode med _null_ åpne oppgaver er ikke automatiserbar`() {
        sessionContext.åpneGosysOppgaverDao.slett(person.id.value)
        blirManuellOppgave()
    }

    @Test
    fun `vedtaksperiode plukket ut til stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveFullRefusjonEnArbeidsgiver = true
        blirStikkprøve()
    }

    @Test
    fun `person med flere arbeidsgivere skal automatisk godkjennes`() {
        sessionContext.vedtakDao.leggTilVedtaksperiodetype(vedtaksperiode1.id.value, FØRSTEGANGSBEHANDLING, Inntektskilde.FLERE_ARBEIDSGIVERE)
        blirAutomatiskBehandlet()
    }

    @Test
    fun `periode med positiv revurdering skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(enUtbetaling(personbeløp = 1, type = REVURDERING))
    }

    @Test
    fun `periode med negativ revurdering skal ikke automatisk godkjennes`() {
        blirManuellOppgave(enUtbetaling(personbeløp = -1, type = REVURDERING))
    }

    @Test
    fun `revurdering uten endringer i beløp kan automatisk godkjennes`() {
        blirAutomatiskBehandlet(
            enUtbetaling(
                arbeidsgiverbeløp = 0,
                personbeløp = 0,
                type = REVURDERING,
            ),
        )
    }

    @Test
    fun `periode med vergemål skal ikke automatisk godkjennes`() {
        sessionContext.vergemålDao.lagre(person.id.value, VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = false), false)
        blirManuellOppgave()
    }

    @Test
    fun `forlengelse med utbetaling til sykmeldt skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(enUtbetaling(personbeløp = 500))
    }

    @Test
    fun `forlengelse med utbetaling til sykmeldt som plukkes ut som stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveUtsEnArbeidsgiverForlengelse = true
        blirStikkprøve(enUtbetaling(personbeløp = 500), periodetype = FORLENGELSE)
    }

    @Test
    fun `selvstendig næringsdrivende forlengelse som plukkes ut som stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveSelvstendigNæringsdrivendeForlengelse = true
        blirStikkprøve(
            enUtbetaling(personbeløp = 500),
            periodetype = FORLENGELSE,
            yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG,
        )
    }

    @Test
    fun `periode med forlengelse av selvstendig næringsdrivende skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(
            enUtbetaling(personbeløp = 500, arbeidsgiverbeløp = 0),
            yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG,
        )
    }

    @Test
    fun `førstegangsbehandling med utbetaling til sykmeldt skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(enUtbetaling(personbeløp = 500))
    }

    @Test
    fun `førstegangsbehandling med utbetaling til sykmeldt som plukkes ut som stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveUtsEnArbeidsgiverFørstegangsbehandling = true
        val utbetaling = enUtbetaling(personbeløp = 500)
        blirStikkprøve(utbetaling, periodetype = FØRSTEGANGSBEHANDLING)
    }

    @Test
    fun `egenansatt går ikke til stikkprøve`() {
        stikkprøveFullRefusjonEnArbeidsgiver = true
        blirStikkprøve()
        sessionContext.personRepository.lagre(lagPerson(id = person.id, erEgenAnsatt = true))
        blirAutomatiskBehandlet()
    }

    @Test
    fun `tar ikke stikkprøve når det er gradert adresse`() {
        stikkprøveFullRefusjonEnArbeidsgiver = true
        blirStikkprøve()
        sessionContext.personRepository.lagre(lagPerson(id = person.id, adressebeskyttelse = Personinfo.Adressebeskyttelse.Fortrolig))
        blirAutomatiskBehandlet()
    }

    @Test
    fun `periode med delvis refusjon skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(enUtbetaling(personbeløp = 500, arbeidsgiverbeløp = 500))
    }

    @Test
    fun `periode med pågående overstyring skal ikke automatisk godkjennes`() {
        sessionContext.totrinnsvurderingRepository.lagre(Totrinnsvurdering.ny(person.id.value))
        blirManuellOppgave()
    }

    @Test
    fun `nullrevurdering grunnet saksbehandleroverstyring skal ikke automatisk godkjennes`() {
        val utbetaling = enUtbetaling(arbeidsgiverbeløp = 0, personbeløp = 0, type = REVURDERING)
        blirAutomatiskBehandlet(utbetaling)
        sessionContext.totrinnsvurderingRepository.lagre(Totrinnsvurdering.ny(person.id.value))
        blirManuellOppgave()
    }

    @Test
    fun `veileder har stanset automatisk behandling`() {
        sessionContext.veilederStansRepository.lagre(
            VeilederStans.ny(
                identitetsnummer = Identitetsnummer.fraString(person.id.value),
                årsaker = setOf(VeilederStans.StansÅrsak.MEDISINSK_VILKAR),
                opprettet = Instant.now(),
                originalMeldingId = UUID.randomUUID(),
            ),
        )
        blirManuellOppgave()
    }

    @Test
    fun `saksbehandler har stanset automatisk behandling`() {
        sessionContext.saksbehandlerStansRepository.lagre(
            SaksbehandlerStans.ny(
                utførtAvSaksbehandlerIdent = lagSaksbehandler().ident,
                begrunnelse = "Begrunnelse",
                identitetsnummer = Identitetsnummer.fraString(person.id.value),
            ),
        )
        blirManuellOppgave()
    }

    @Test
    fun `førstegangsbehandling av selvstendig næringsdrivende stanser automatisk behandling`() {
        blirManuellOppgave(yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG, periodetype = FØRSTEGANGSBEHANDLING)
    }

    private fun assertKanIkkeAutomatiseres(resultat: Automatiseringsresultat) {
        logg.info("Fikk resultat $resultat")
        assertTrue(
            resultat is Automatiseringsresultat.KanIkkeAutomatiseres,
            "Expected ${Automatiseringsresultat.KanIkkeAutomatiseres::class.simpleName}, got ${resultat::class.simpleName}",
        )
    }

    private fun assertStikkprøve(resultat: Automatiseringsresultat) {
        assertTrue(
            resultat is Automatiseringsresultat.Stikkprøve,
            "Expected ${Automatiseringsresultat.Stikkprøve::class.simpleName}, got ${resultat::class.simpleName}",
        )
    }

    private fun assertKanAutomatiseres(resultat: Automatiseringsresultat) {
        assertTrue(
            resultat is Automatiseringsresultat.KanAutomatiseres,
            "Expected ${Automatiseringsresultat.KanAutomatiseres::class.simpleName}, got ${resultat::class.simpleName}",
        )
    }

    private fun forsøkAutomatisering(
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        periodetype: Periodetype = FORLENGELSE,
        gjeldendeBehandling: Behandling = enBehandling(yrkesaktivitetstype = yrkesaktivitetstype),
        utbetaling: Utbetaling = enUtbetaling(),
        maksdato: LocalDate = 1 des 2018,
        tags: List<String> = emptyList(),
    ) = automatisering.utfør(
        fødselsnummer = person.id.value,
        vedtaksperiodeId = vedtaksperiode1.id,
        utbetaling = utbetaling,
        periodetype = periodetype,
        gjeldendeBehandling = gjeldendeBehandling,
        organisasjonsnummer = vedtaksperiode1.organisasjonsnummer,
        yrkesaktivitetstype = yrkesaktivitetstype,
        maksdato = maksdato,
        tags = tags,
    )

    private fun enUtbetaling(
        arbeidsgiverbeløp: Int = 500,
        personbeløp: Int = 0,
        type: Utbetalingtype = Utbetalingtype.UTBETALING,
    ) = Utbetaling(utbetalingId, arbeidsgiverbeløp, personbeløp, type)

    private fun enBehandling(
        fom: LocalDate = 1 jan 2018,
        tom: LocalDate = 31 jan 2018,
        skjæringstidspunkt: LocalDate = fom,
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
    ) = lagBehandling(
        vedtaksperiodeId = vedtaksperiode1.id,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        yrkesaktivitetstype = yrkesaktivitetstype,
    )

    private fun blirManuellOppgave(
        utbetaling: Utbetaling = enUtbetaling(),
        gjeldendeBehandling: Behandling = enBehandling(),
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        maksdato: LocalDate = 1 des 2018,
        tags: List<String> = emptyList(),
        periodetype: Periodetype = FORLENGELSE,
    ) = assertKanIkkeAutomatiseres(
        forsøkAutomatisering(
            yrkesaktivitetstype = yrkesaktivitetstype,
            utbetaling = utbetaling,
            gjeldendeBehandling = gjeldendeBehandling,
            maksdato = maksdato,
            tags = tags,
            periodetype = periodetype,
        ),
    )

    private fun blirStikkprøve(
        utbetaling: Utbetaling = enUtbetaling(),
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
    ) = assertStikkprøve(
        forsøkAutomatisering(
            utbetaling = utbetaling,
            periodetype = periodetype,
            yrkesaktivitetstype = yrkesaktivitetstype,
        ),
    )

    private fun blirManuellOppgaveMedFeil(
        utbetaling: Utbetaling = enUtbetaling(),
        problems: List<String>,
    ) {
        val resultat = forsøkAutomatisering(utbetaling = utbetaling)
        assertKanIkkeAutomatiseres(resultat)
        check(resultat is Automatiseringsresultat.KanIkkeAutomatiseres)
        assertEquals(problems.toSet(), resultat.problemer.toSet())
    }

    private fun blirAutomatiskBehandlet(
        utbetaling: Utbetaling = enUtbetaling(),
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
    ) = assertKanAutomatiseres(forsøkAutomatisering(utbetaling = utbetaling, yrkesaktivitetstype = yrkesaktivitetstype))

    private fun blirManuellOppgaveMedFeilOgVarsel(
        utbetaling: Utbetaling = enUtbetaling(),
        problems: List<String>,
        gjeldendeBehandling: Behandling = enBehandling(),
        varselkode: Varselkode,
        maksdato: LocalDate = 1 des 2018,
        tags: List<String> = emptyList(),
    ) {
        val resultat =
            forsøkAutomatisering(
                utbetaling = utbetaling,
                gjeldendeBehandling = gjeldendeBehandling,
                maksdato = maksdato,
                tags = tags,
            )
        assertKanIkkeAutomatiseres(resultat)
        check(resultat is Automatiseringsresultat.KanIkkeAutomatiseres)
        assertEquals(problems.toSet(), resultat.problemer.toSet())
        assertEquals(
            varselkode.name,
            sessionContext.varselRepository
                .finnVarslerFor(gjeldendeBehandling.id)
                .first()
                .kode,
        )
    }
}
