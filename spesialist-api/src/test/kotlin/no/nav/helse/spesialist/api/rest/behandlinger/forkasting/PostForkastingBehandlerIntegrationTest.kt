package no.nav.helse.spesialist.api.rest.behandlinger.forkasting

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Arbeidssituasjon
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostForkastingBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext
    private val saksbehandlerRepository = sessionContext.saksbehandlerRepository
    private val behandlingRepository = sessionContext.behandlingRepository
    private val vedtaksperiodeRepository = sessionContext.vedtaksperiodeRepository
    private val varselRepository = sessionContext.varselRepository
    private val varseldefinisjonRepository = sessionContext.varseldefinisjonRepository
    private val personRepository = sessionContext.personRepository
    private val meldingDao = sessionContext.meldingDao
    private val oppgaveRepository = sessionContext.oppgaveRepository
    private val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository

    @Test
    fun `happy path`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val varseldefinisjon = lagVarseldefinisjon()
        val varsel = lagVarsel(behandlingUnikId = behandling.id, spleisBehandlingId = behandling.spleisBehandlingId, kode = varseldefinisjon.kode)
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varselRepository.lagre(varsel)
        meldingDao.lagre(godkjenningsbehov)
        oppgaveRepository.lagre(oppgave)
        totrinnsvurderingRepository.lagre(totrinnsvurdering)
        varseldefinisjonRepository.lagre(varseldefinisjon)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/forkasting",
            body = """
                {
                  "årsak": "En årsak",
                  "begrunnelser": ["Begrunnelse 1", "Begrunnelse 2"],
                  "kommentar": "En kommentar"
                }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        val funnetVarsel = varselRepository.finn(varsel.id)
        val funnetTotrinnsvurdering = totrinnsvurderingRepository.finn(totrinnsvurdering.id())
        val funnetOppgave = oppgaveRepository.finn(oppgave.id)
        val reservasjon = integrationTestFixture.sessionFactory.sessionContext.reservasjonDao.hentReservasjonFor(person.id.value)
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        assertEquals(Varsel.Status.AVVIST, funnetVarsel?.status)
        assertEquals(TotrinnsvurderingTilstand.GODKJENT, funnetTotrinnsvurdering?.tilstand)
        assertEquals(Oppgave.AvventerSystem, funnetOppgave?.tilstand)
        assertEquals(false, funnetOppgave?.egenskaper?.contains(Egenskap.PÅ_VENT))
        assertNotNull(reservasjon)
        assertEquals(saksbehandler, reservasjon.reservertTil)
    }

    @Test
    fun `gir NotFound hvis behandlingen ikke finnes`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val saksbehandler = lagSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/behandlinger/$behandlingId/forkasting",
            body = """{
                  "årsak": "En årsak",
                    "begrunnelser": ["Begrunnelse 1", "Begrunnelse 2"],
                    "kommentar": "En kommentar"
                }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "Fant ikke behandling",
              "code": "BEHANDLING_IKKE_FUNNET" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir NotFound hvis vedtaksperioden ikke finnes`() {
        // Given:
        val saksbehandler = lagSaksbehandler()
        val behandlingId = lagSpleisBehandlingId()
        val behandling =
            lagBehandling(spleisBehandlingId = behandlingId)
        behandlingRepository.lagre(behandling)
        saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/behandlinger/${behandlingId.value}/forkasting",
            body = """
                {
                  "årsak": "En årsak",
                  "begrunnelser": ["Begrunnelse 1", "Begrunnelse 2"],
                  "kommentar": "En kommentar"
                }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "Fant ikke vedtaksperiode",
              "code": "VEDTAKSPERIODE_IKKE_FUNNET" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir forbidden hvis saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(erEgenAnsatt = true)
            .also(personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/forkasting",
            body = """
                {
                  "årsak": "En årsak",
                  "begrunnelser": ["Begrunnelse 1", "Begrunnelse 2"],
                  "kommentar": "En kommentar"
                }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }

    @Test
    fun `gir bad request hvis oppgaven ikke finnes`() {
        // Given:
        val person = lagPerson()
            .also(personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/forkasting",
            body = """
                {
                  "årsak": "En årsak",
                  "begrunnelser": ["Begrunnelse 1", "Begrunnelse 2"],
                  "kommentar": "En kommentar"
                }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 400,
              "title": "Fant ikke oppgave.",
              "code": "OPPGAVE_IKKE_FUNNET" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir bad request hvis oppgaven ikke avventer saksbehandler`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        meldingDao.lagre(godkjenningsbehov)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        oppgave.avventerSystem(saksbehandler.ident, UUID.randomUUID())
        oppgaveRepository.lagre(oppgave)
        // When:
        val response = integrationTestFixture.post(
            url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/forkasting",
            body = """
                {
                  "årsak": "En årsak",
                  "begrunnelser": ["Begrunnelse 1", "Begrunnelse 2"],
                  "kommentar": "En kommentar"
                }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 400,
              "title": "Oppgaven er i feil tilstand.",
              "code": "OPPGAVE_FEIL_TILSTAND"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir bad request hvis det finnes totrinnsvurdering og denne er sendt til beslutter - kun saksbehandler skal kunne kaste ut saken`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        meldingDao.lagre(godkjenningsbehov)

        oppgaveRepository.lagre(oppgave)

        totrinnsvurdering.sendTilBeslutter(oppgave.id, saksbehandler.id)
        totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/forkasting",
            body = """
                {
                  "årsak": "En årsak",
                  "begrunnelser": ["Begrunnelse 1", "Begrunnelse 2"],
                  "kommentar": "En kommentar"
                }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 400,
              "title": "Totrinnsvurdering er sendt til beslutter, saken må returneres til saksbehandler før den kan kastes ut",
              "code": "TOTRINNSVURDERING_SENDT_TIL_BESLUTTER"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir internal server error dersom det ikke finnes noen definisjon for varselet`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)
        val varsel = lagVarsel(behandlingUnikId = behandling.id, spleisBehandlingId = behandling.spleisBehandlingId)

        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        meldingDao.lagre(godkjenningsbehov)
        varselRepository.lagre(varsel)
        oppgaveRepository.lagre(oppgave)
        totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/forkasting",
            body = """
                {
                  "årsak": "En årsak",
                  "begrunnelser": ["Begrunnelse 1", "Begrunnelse 2"],
                  "kommentar": "En kommentar"
                }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )
    }

    private fun lagGodkjenningsbehov(behandling: Behandling, vedtaksperiode: Vedtaksperiode): Godkjenningsbehov {
        return Godkjenningsbehov(
            id = UUID.randomUUID(),
            opprettet = LocalDateTime.now(),
            fødselsnummer = vedtaksperiode.fødselsnummer,
            organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
            yrkesaktivitetstype = behandling.yrkesaktivitetstype,
            vedtaksperiodeId = vedtaksperiode.id.value,
            spleisVedtaksperioder = emptyList(),
            utbetalingId = behandling.utbetalingId?.value!!,
            spleisBehandlingId = behandling.spleisBehandlingId!!.value,
            vilkårsgrunnlagId = UUID.randomUUID(),
            tags = behandling.tags.toList(),
            periodeFom = behandling.fom,
            periodeTom = behandling.tom,
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            førstegangsbehandling = true,
            utbetalingtype = Utbetalingtype.UTBETALING,
            kanAvvises = true,
            inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
            orgnummereMedRelevanteArbeidsforhold = emptyList(),
            skjæringstidspunkt = behandling.skjæringstidspunkt,
            sykepengegrunnlagsfakta = Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker.EtterHovedregel(
                seksG = 6 * 118620.0,
                arbeidsgivere = listOf(
                    Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                        organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                        inntektskilde = Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver,
                        omregnetÅrsinntekt = 700_000.0,
                    )
                ),
                sykepengegrunnlag = BigDecimal("700000.0")
            ),
            foreløpigBeregnetSluttPåSykepenger = behandling.fom.plusYears(1),
            arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER,
            json = "{}",
        )
    }
}
