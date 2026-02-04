package no.nav.helse.spesialist.api.rest.behandlinger.vedtak

import io.ktor.http.HttpStatusCode
import no.nav.helse.Varselvurdering
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentManuelt
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.application.testing.assertMindreEnnNSekunderSiden
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.Vedtak
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PostVedtakIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `happy case - ingen totrinnskontroll`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)
        sessionContext.meldingDao.lagre(godkjenningsbehov)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        val vedtak = sessionContext.vedtakRepository.finn(behandling.spleisBehandlingId!!)
        assertIs<Vedtak.ManueltUtenTotrinnskontroll>(vedtak)
        assertEquals(saksbehandler.ident, vedtak.saksbehandlerIdent)
    }

    @Test
    fun `happy case - med krav om totrinnskontroll`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val beslutter = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)
        val totrinnsvurdering = Totrinnsvurdering.ny(person.id.value)
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.saksbehandlerRepository.lagre(beslutter)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, saksbehandler.id)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        sessionContext.oppgaveRepository.lagre(oppgave)
        sessionContext.meldingDao.lagre(godkjenningsbehov)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = beslutter,
                tilganger = setOf(Tilgang.SAKSBEHANDLER),
                brukerroller = setOf(Brukerrolle.BESLUTTER),
            )

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        val vedtak = sessionContext.vedtakRepository.finn(behandling.spleisBehandlingId!!)
        assertIs<Vedtak.ManueltMedTotrinnskontroll>(vedtak)
        assertEquals(saksbehandler.ident, vedtak.saksbehandlerIdent)
        assertEquals(beslutter.ident, vedtak.beslutterIdent)
    }

    @Test
    fun `gir NotFound hvis behandlingen ikke finnes`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/$behandlingId/vedtak",
                body = "{}",
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
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir NotFound hvis vedtaksperioden ikke finnes`() {
        // Given:
        val saksbehandler = lagSaksbehandler()
        val behandlingId = lagSpleisBehandlingId()
        val behandling =
            lagBehandling(spleisBehandlingId = behandlingId)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandlingId.value}/vedtak",
                body = "{}",
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
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir forbidden hvis saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person =
            lagPerson(erEgenAnsatt = true)
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }

    @Test
    fun `gir bad request hvis oppgaven ikke finnes`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
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
            response.bodyAsJsonNode!!,
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
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.meldingDao.lagre(godkjenningsbehov)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        oppgave.avventerSystem(saksbehandler.ident, UUID.randomUUID())
        sessionContext.oppgaveRepository.lagre(oppgave)
        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
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
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir forbidden hvis saksbehandler mangler besluttertilgang`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val beslutter = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)

        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.saksbehandlerRepository.lagre(beslutter)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.meldingDao.lagre(godkjenningsbehov)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, saksbehandler.id)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = beslutter,
            )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 403,
              "title": "Mangler besluttertilgang",
              "code": "SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir forbidden hvis saksbehandler prøver å beslutte egen oppgave`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)

        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.meldingDao.lagre(godkjenningsbehov)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, saksbehandler.id)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.SAKSBEHANDLER),
                brukerroller = setOf(Brukerrolle.BESLUTTER),
            )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 403,
              "title": "Kan ikke beslutte egen oppgave",
              "code": "SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir conflict hvis totrinnsvurdering mangler beslutter`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)

        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.meldingDao.lagre(godkjenningsbehov)

        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(person.id.value)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.SAKSBEHANDLER),
            )

        // Then:
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 409,
              "title": "Behandlende saksbehandler mangler i totrinnsvurdering",
              "code": "TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir conflict hvis vedtaket er fattet allerede`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)

        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.meldingDao.lagre(godkjenningsbehov)
        sessionContext.vedtakRepository.lagre(
            Vedtak.manueltUtenTotrinnskontroll(
                behandling.spleisBehandlingId!!,
                saksbehandler.ident,
            ),
        )

        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 409,
              "title": "Vedtaket er allerede fattet",
              "code": "VEDTAK_ALLEREDE_FATTET" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir bad request hvis behandlingen overlapper med Infotrygd`() {
        // Given:
        val person =
            lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                tags = setOf("OverlapperMedInfotrygd"),
            )
        val saksbehandler = lagSaksbehandler()
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)

        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.meldingDao.lagre(godkjenningsbehov)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.SAKSBEHANDLER),
            )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 400,
              "title": "Kan ikke fatte vedtak fordi perioden overlapper med infotrygd",
              "code": "OVERLAPPER_MED_INFOTRYGD" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir bad request hvis det finnes relevante varsler som ikke er vurdert`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val kode = "RV_IV_2"
        val varsel =
            lagVarsel(
                behandlingUnikId = behandling.id,
                spleisBehandlingId = behandling.spleisBehandlingId,
                status = Varsel.Status.AKTIV,
                vurdering = null,
                kode = kode,
            )
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)

        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.varseldefinisjonRepository.lagre(lagVarseldefinisjon(kode = kode))
        sessionContext.varselRepository.lagre(varsel)
        sessionContext.meldingDao.lagre(godkjenningsbehov)

        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.SAKSBEHANDLER),
            )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 400,
              "title": "Kan ikke godkjenne varsler som ikke er vurdert av en saksbehandler",
              "code": "VARSLER_MANGLER_VURDERING" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsler som har status annet enn aktiv medfører ikke valideringsfeil`(status: Varsel.Status) {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val kode = "RV_IV_2"
        val varsel =
            lagVarsel(
                behandlingUnikId = behandling.id,
                spleisBehandlingId = behandling.spleisBehandlingId,
                status = status,
                vurdering =
                    Varselvurdering(
                        saksbehandlerId = saksbehandler.id,
                        tidspunkt = LocalDateTime.now(),
                        vurdertDefinisjonId = lagVarseldefinisjon(kode = kode).id,
                    ),
                kode = kode,
            )
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.varseldefinisjonRepository.lagre(lagVarseldefinisjon(kode = kode))
        sessionContext.varselRepository.lagre(varsel)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)
        sessionContext.meldingDao.lagre(godkjenningsbehov)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.SAKSBEHANDLER),
            )

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)
    }

    @Test
    fun `publiserer kun varsel_endret for varsler som har blitt godkjent`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val varseldefinisjon = lagVarseldefinisjon()
        val godkjentVarsel =
            lagVarsel(
                behandlingUnikId = behandling.id,
                spleisBehandlingId = behandling.spleisBehandlingId,
                status = Varsel.Status.GODKJENT,
                vurdering = Varselvurdering(saksbehandler.id, LocalDateTime.now(), varseldefinisjon.id),
                kode = varseldefinisjon.kode,
            )
        val vurdertVarsel = lagVurdertVarsel(varseldefinisjon, saksbehandler, behandling)
        val godkjenningsbehov = lagGodkjenningsbehov(behandling, vedtaksperiode)
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.varseldefinisjonRepository.lagre(varseldefinisjon)

        sessionContext.varselRepository.lagre(godkjentVarsel)
        sessionContext.varselRepository.lagre(vurdertVarsel)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!, godkjenningsbehov.id)
        sessionContext.oppgaveRepository.lagre(oppgave)
        sessionContext.meldingDao.lagre(godkjenningsbehov)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.SAKSBEHANDLER),
            )

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        val iderForPubliserteVarsler =
            integrationTestFixture.meldingPubliserer.publiserteUtgåendeHendelser
                .map { it.hendelse }
                .filterIsInstance<VarselEndret>()
                .map { it.varselId }
        assertEquals(listOf(vurdertVarsel.id.value), iderForPubliserteVarsler)
    }

    @Test
    fun `publiserer forventede meldinger`() {
        // Given:
        val person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id)
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling =
            lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
                .also(sessionContext.behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val varseldefinisjon =
            lagVarseldefinisjon()
                .also { sessionContext.varseldefinisjonRepository.lagre(it) }
        val saksbehandler = lagSaksbehandler()
        val vurdertVarsel =
            lagVurdertVarsel(varseldefinisjon, saksbehandler, behandling)
                .also(sessionContext.varselRepository::lagre)
        val godkjenningsbehov =
            lagGodkjenningsbehov(behandling, vedtaksperiode)
                .also(sessionContext.meldingDao::lagre)
        val oppgave =
            lagOppgave(spleisBehandlingId, godkjenningsbehov.id)
                .also(sessionContext.oppgaveRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/behandlinger/${spleisBehandlingId.value}/vedtak",
                body = "{}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            { actualUtgåendeHendelse ->
                assertEquals(
                    InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                        fødselsnummer = person.id.value,
                        hendelse =
                            VarselEndret(
                                vedtaksperiodeId = vedtaksperiode.id.value,
                                behandlingIdForBehandlingSomBleGodkjent = spleisBehandlingId.value,
                                varselId = vurdertVarsel.id.value,
                                varseltittel = varseldefinisjon.tittel,
                                varselkode = varseldefinisjon.kode,
                                forrigeStatus = "VURDERT",
                                gjeldendeStatus = "GODKJENT",
                            ),
                        årsak = "varsel godkjent",
                    ),
                    actualUtgåendeHendelse,
                )
            },
            { actualUtgåendeHendelse ->
                assertEquals(
                    InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                        fødselsnummer = person.id.value,
                        hendelse =
                            OppgaveOppdatert(
                                oppgave = oppgave,
                            ),
                        årsak = "oppgave avventer system",
                    ),
                    actualUtgåendeHendelse,
                )
            },
            { actualUtgåendeHendelse ->
                assertEquals(person.id.value, actualUtgåendeHendelse.fødselsnummer)
                val hendelse = actualUtgåendeHendelse.hendelse
                assertIs<Godkjenningsbehovløsning>(hendelse)
                assertEquals(true, hendelse.godkjent)
                assertEquals(saksbehandler.ident.value, hendelse.saksbehandlerIdent)
                assertEquals(saksbehandler.epost, hendelse.saksbehandlerEpost)
                assertMindreEnnNSekunderSiden(30, hendelse.godkjenttidspunkt)
                assertEquals(false, hendelse.automatiskBehandling)
                assertEquals(null, hendelse.årsak)
                assertEquals(null, hendelse.begrunnelser)
                assertEquals(null, hendelse.kommentar)
                assertEquals(listOf(), hendelse.saksbehandleroverstyringer)
                assertEquals("{}", hendelse.json)
                assertEquals("saksbehandlergodkjenning", actualUtgåendeHendelse.årsak)
            },
            { actualUtgåendeHendelse ->
                assertEquals(
                    InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                        fødselsnummer = person.id.value,
                        hendelse =
                            VedtaksperiodeGodkjentManuelt(
                                fødselsnummer = person.id.value,
                                vedtaksperiodeId = vedtaksperiode.id.value,
                                behandlingId = spleisBehandlingId.value,
                                yrkesaktivitetstype = behandling.yrkesaktivitetstype,
                                periodetype = godkjenningsbehov.periodetype.name,
                                saksbehandlerIdent = saksbehandler.ident,
                                saksbehandlerEpost = saksbehandler.epost,
                                beslutterIdent = null,
                                beslutterEpost = null,
                            ),
                        årsak = "saksbehandlergodkjenning",
                    ),
                    actualUtgåendeHendelse,
                )
            },
        )
    }

    private fun lagVurdertVarsel(
        varseldefinisjon: Varseldefinisjon,
        saksbehandler: Saksbehandler,
        behandling: Behandling,
    ): Varsel =
        lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(saksbehandler.id, LocalDateTime.now(), varseldefinisjon.id),
            kode = varseldefinisjon.kode,
        )

    private fun lagGodkjenningsbehov(
        behandling: Behandling,
        vedtaksperiode: Vedtaksperiode,
    ): Godkjenningsbehov =
        Godkjenningsbehov(
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
            sykepengegrunnlagsfakta =
                Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker.EtterHovedregel(
                    seksG = 6 * 118620.0,
                    arbeidsgivere =
                        listOf(
                            Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                                inntektskilde = Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver,
                                omregnetÅrsinntekt = 700_000.0,
                            ),
                        ),
                    sykepengegrunnlag = BigDecimal("700000.0"),
                ),
            foreløpigBeregnetSluttPåSykepenger = behandling.fom.plusYears(1),
            arbeidssituasjon = null,
            relevanteSøknader = listOf(UUID.randomUUID()),
            json = "{}",
        )
}
