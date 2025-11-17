package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.Varselvurdering
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class PostFattVedtakIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `gir NotFound hvis behandlingen ikke finnes`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandlingId.value}/fatt",
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
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir forbidden hvis saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(erEgenAnsatt = true)
            .also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }

    @Test
    fun `gir bad request hvis oppgaven ikke finnes`() {
        // Given:
        val person = lagPerson()
            .also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
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
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!)
        oppgave.avventerSystem(saksbehandler.ident, UUID.randomUUID())
        sessionContext.oppgaveRepository.lagre(oppgave)
        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
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
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir forbidden hvis saksbehandler mangler besluttertilgang`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)

        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, SaksbehandlerOid(UUID.randomUUID()))
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = emptySet()
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
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir forbidden hvis saksbehandler prøver å beslutte egen oppgave`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, saksbehandler.id)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
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
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir conflict hvis totrinnsvurdering mangler beslutter`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(person.id.value)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
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
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `gir bad request hvis behandlingen overlapper med Infotrygd`() {
        // Given:
        val person =
            lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(
            vedtaksperiodeId = vedtaksperiode.id,
            tags = setOf("OverlapperMedInfotrygd")
        )
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
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
            response.bodyAsJsonNode!!
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
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.AKTIV,
            vurdering = null,
            kode = kode
        )
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.varseldefinisjonRepository.lagre(lagVarseldefinisjon(kode = kode))
        sessionContext.varselRepository.lagre(varsel)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
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
            response.bodyAsJsonNode!!
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
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = status,
            vurdering = Varselvurdering(
                saksbehandlerId = saksbehandler.id,
                tidspunkt = LocalDateTime.now(),
                vurdertDefinisjonId = lagVarseldefinisjon(kode = kode).id
            ),
            kode = kode
        )
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.varseldefinisjonRepository.lagre(lagVarseldefinisjon(kode = kode))
        sessionContext.varselRepository.lagre(varsel)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
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
        val godkjentVarsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.GODKJENT,
            vurdering = Varselvurdering(saksbehandler.id, LocalDateTime.now(), varseldefinisjon.id),
            kode = varseldefinisjon.kode
        )
        val vurdertVarsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(saksbehandler.id, LocalDateTime.now(), varseldefinisjon.id),
            kode = varseldefinisjon.kode
        )
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.varseldefinisjonRepository.lagre(varseldefinisjon)

        sessionContext.varselRepository.lagre(godkjentVarsel)
        sessionContext.varselRepository.lagre(vurdertVarsel)
        val oppgave = lagOppgave(behandling.spleisBehandlingId!!)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/${behandling.spleisBehandlingId?.value}/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
        )

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        val iderForPubliserteVarsler = integrationTestFixture.meldingPubliserer.publiserteUtgåendeHendelser
            .map { it.hendelse }
            .filterIsInstance<VarselEndret>()
            .map { it.varselId }
        assertEquals(listOf(vurdertVarsel.id.value), iderForPubliserteVarsler)
    }
}
