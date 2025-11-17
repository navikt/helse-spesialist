package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.testfixtures.lagEnBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagEnVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
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
        val behandlingId = UUID.randomUUID()
        val saksbehandler = lagSaksbehandler()
        val behandling =
            lagEnBehandling(spleisBehandlingId = behandlingId)
        sessionContext.behandlingRepository.lagre(behandling)
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
        val behandlingId = UUID.randomUUID()
        val person = lagPerson(erEgenAnsatt = true)
            .also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }

    @Test
    fun `gir bad request hvis oppgaven ikke finnes`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val person = lagPerson()
            .also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgave(behandlingId)
        oppgave.avventerSystem(saksbehandler.ident, UUID.randomUUID())
        sessionContext.oppgaveRepository.lagre(oppgave)
        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgave(behandlingId)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = fødselsnummer)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, SaksbehandlerOid(UUID.randomUUID()))
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgave(behandlingId)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = fødselsnummer)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, saksbehandler.id)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgave(behandlingId)
        sessionContext.oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = fødselsnummer)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandling = lagEnBehandling(
            spleisBehandlingId = behandlingId,
            vedtaksperiodeId = vedtaksperiode.id,
            tags = setOf("OverlapperMedInfotrygd")
        )
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgave(behandlingId)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandlingUnikId = UUID.randomUUID()
        val behandling = lagEnBehandling(
            id = behandlingUnikId,
            spleisBehandlingId = behandlingId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        val saksbehandler = lagSaksbehandler()
        val kode = "RV_IV_2"
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)
        sessionContext.varseldefinisjonRepository.lagre(lagVarseldefinisjon(kode = kode))
        sessionContext.varselRepository.lagre(
            Varsel.fraLagring(
                VarselId(UUID.randomUUID()),
                behandling.spleisBehandlingId!!,
                behandlingUnikId = behandling.id,
                status = Varsel.Status.AKTIV,
                vurdering = null,
                kode = kode,
                opprettetTidspunkt = LocalDateTime.now(),
            )
        )
        val oppgave = lagOppgave(behandlingId)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandlingUnikId = UUID.randomUUID()
        val behandling = lagEnBehandling(
            id = behandlingUnikId,
            spleisBehandlingId = behandlingId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        val saksbehandler = lagSaksbehandler()
        val kode = "RV_IV_2"
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)
        sessionContext.varseldefinisjonRepository.lagre(lagVarseldefinisjon(kode = kode))
        sessionContext.varselRepository.lagre(
            Varsel.fraLagring(
                VarselId(UUID.randomUUID()),
                behandling.spleisBehandlingId!!,
                behandlingUnikId = behandling.id,
                status = status,
                vurdering = null,
                kode = kode,
                opprettetTidspunkt = LocalDateTime.now(),
            )
        )
        val oppgave = lagOppgave(behandlingId)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandlingUnikIdbehandling = UUID.randomUUID()
        val behandling = lagEnBehandling(
            id = behandlingUnikIdbehandling,
            spleisBehandlingId = behandlingId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        val saksbehandler = lagSaksbehandler()
        val kode = "RV_IV_2"
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)
        sessionContext.varseldefinisjonRepository.lagre(lagVarseldefinisjon(kode = kode))
        val godkjentVarsel = Varsel.fraLagring(
            id = VarselId(UUID.randomUUID()),
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            behandlingUnikId = behandling.id,
            status = Varsel.Status.GODKJENT,
            vurdering = null,
            kode = kode,
            opprettetTidspunkt = LocalDateTime.now(),
        )
        val vurdertVarsel = Varsel.fraLagring(
            id = VarselId(UUID.randomUUID()),
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            behandlingUnikId = behandling.id,
            status = Varsel.Status.VURDERT,
            vurdering = null,
            kode = kode,
            opprettetTidspunkt = LocalDateTime.now(),
        )

        sessionContext.varselRepository.lagre(godkjentVarsel)
        sessionContext.varselRepository.lagre(vurdertVarsel)
        val oppgave = lagOppgave(behandlingId)
        sessionContext.oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
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
