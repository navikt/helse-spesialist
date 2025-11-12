package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.VarselEndret
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagEnBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagEnOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagEnSaksbehandler
import no.nav.helse.spesialist.domain.testfixtures.lagEnVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class PostFattVedtakIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    private val saksbehandlerRepository = sessionContext.saksbehandlerRepository
    private val behandlingRepository = sessionContext.behandlingRepository
    private val egenansattDao = sessionContext.egenAnsattDao
    private val oppgaveRepository = sessionContext.oppgaveRepository
    private val personDao = sessionContext.personDao
    private val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository
    private val vedtaksperiodeRepository = sessionContext.vedtaksperiodeRepository
    private val varselRepository = sessionContext.varselRepository
    private val varseldefinisjonRepository = sessionContext.varseldefinisjonRepository

    @Test
    fun `gir NotFound hvis behandlingen ikke finnes`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val saksbehandler = lagEnSaksbehandler()
        val behandling =
            lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()))
        behandlingRepository.lagre(behandling)
        saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id())
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, true, LocalDateTime.now())

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
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer, lagOrganisasjonsnummer())
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id())
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id())
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        val oppgave = lagEnOppgave(behandlingId)
        oppgave.avventerSystem(lagSaksbehandlerident(), UUID.randomUUID())
        oppgaveRepository.lagre(oppgave)
        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id())
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        val oppgave = lagEnOppgave(behandlingId)
        oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = fødselsnummer)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, SaksbehandlerOid(UUID.randomUUID()))
        totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = emptySet()
        )


        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id())
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        val oppgave = lagEnOppgave(behandlingId)
        oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = fødselsnummer)
        totrinnsvurdering.sendTilBeslutter(oppgave.id, saksbehandler.id())
        totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
        )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val behandling = lagEnBehandling(spleisBehandlingId = behandlingId, vedtaksperiodeId = vedtaksperiode.id())
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        val oppgave = lagEnOppgave(behandlingId)
        oppgaveRepository.lagre(oppgave)

        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = fødselsnummer)
        totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
        )

        // Then:
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
            vedtaksperiodeId = vedtaksperiode.id(),
            tags = setOf("OverlapperMedInfotrygd")
        )
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        val oppgave = lagEnOppgave(behandlingId)
        oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
        )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
            vedtaksperiodeId = vedtaksperiode.id()
        )
        val saksbehandler = lagEnSaksbehandler()
        val kode = "RV_IV_2"
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        varseldefinisjonRepository.lagre(kode)
        varselRepository.lagre(
            Varsel.fraLagring(
                VarselId(UUID.randomUUID()),
                behandling.spleisBehandlingId!!,
                behandlingUnikId = behandling.id(),
                status = Varsel.Status.AKTIV,
                vurdering = null,
                kode = kode,
                opprettetTidspunkt = LocalDateTime.now(),
            )
        )
        val oppgave = lagEnOppgave(behandlingId)
        oppgaveRepository.lagre(oppgave)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER)
        )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
            vedtaksperiodeId = vedtaksperiode.id()
        )
        val saksbehandler = lagEnSaksbehandler()
        val kode = "RV_IV_2"
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        varseldefinisjonRepository.lagre(kode)
        varselRepository.lagre(
            Varsel.fraLagring(
                VarselId(UUID.randomUUID()),
                behandling.spleisBehandlingId!!,
                behandlingUnikId = behandling.id(),
                status = status,
                vurdering = null,
                kode = kode,
                opprettetTidspunkt = LocalDateTime.now(),
            )
        )
        val oppgave = lagEnOppgave(behandlingId)
        oppgaveRepository.lagre(oppgave)

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
            vedtaksperiodeId = vedtaksperiode.id()
        )
        val saksbehandler = lagEnSaksbehandler()
        val kode = "RV_IV_2"
        saksbehandlerRepository.lagre(saksbehandler)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        varseldefinisjonRepository.lagre(kode)
        val godkjentVarsel = Varsel.fraLagring(
            id = VarselId(UUID.randomUUID()),
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            behandlingUnikId = behandling.id(),
            status = Varsel.Status.GODKJENT,
            vurdering = null,
            kode = kode,
            opprettetTidspunkt = LocalDateTime.now(),
        )
        val vurdertVarsel = Varsel.fraLagring(
            id = VarselId(UUID.randomUUID()),
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            behandlingUnikId = behandling.id(),
            status = Varsel.Status.VURDERT,
            vurdering = null,
            kode = kode,
            opprettetTidspunkt = LocalDateTime.now(),
        )

        varselRepository.lagre(godkjentVarsel)
        varselRepository.lagre(vurdertVarsel)
        val oppgave = lagEnOppgave(behandlingId)
        oppgaveRepository.lagre(oppgave)

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
        assertEquals(listOf(vurdertVarsel.id().value), iderForPubliserteVarsler)
    }
}
