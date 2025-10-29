package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavn
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextLong

class PostFattVedtakIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val saksbehandlerRepository = integrationTestFixture.sessionFactory.sessionContext.saksbehandlerRepository
    private val behandlingRepository = integrationTestFixture.sessionFactory.sessionContext.behandlingRepository
    private val egenansattDao = integrationTestFixture.sessionFactory.sessionContext.egenAnsattDao
    private val oppgaveRepository = integrationTestFixture.sessionFactory.sessionContext.oppgaveRepository
    private val personDao = integrationTestFixture.sessionFactory.sessionContext.personDao
    private val totrinnsvurderingRepository = integrationTestFixture.sessionFactory.sessionContext.totrinnsvurderingRepository
    private val vedtaksperiodeRepository = integrationTestFixture.sessionFactory.sessionContext.vedtaksperiodeRepository
    private val varselRepository = integrationTestFixture.sessionFactory.sessionContext.varselRepository
    private val varseldefinisjonRepository = integrationTestFixture.sessionFactory.sessionContext.varseldefinisjonRepository

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
        response.assertResponseMessage(PostFattVedtakBehandler.BEHANDLING_IKKE_FUNNET)
    }

    @Test
    fun `gir NotFound hvis vedtaksperioden ikke finnes`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val saksbehandler = lagEnSaksbehandler()
        val behandling = lagEnBehandling(behandlingId, VedtaksperiodeId(UUID.randomUUID()))
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
        response.assertResponseMessage(PostFattVedtakBehandler.VEDTAKSPERIODE_IKKE_FUNNET)
    }

    @Test
    fun `gir forbidden hvis saksbehandler ikke har tilgang til personen`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer)
        val behandling = lagEnBehandling(behandlingId, vedtaksperiode.id())
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
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer)
        val behandling = lagEnBehandling(behandlingId, vedtaksperiode.id())
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
        response.assertResponseMessage(PostFattVedtakBehandler.OPPGAVE_IKKE_FUNNET)
    }

    @Test
    fun `gir bad request hvis oppgaven ikke avventer saksbehandler`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer)
        val behandling = lagEnBehandling(behandlingId, vedtaksperiode.id())
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
        oppgave.avventerSystem(lagSaksbehandlerident(),UUID.randomUUID())
        oppgaveRepository.lagre(oppgave)
        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        response.assertResponseMessage(PostFattVedtakBehandler.OPPGAVE_FEIL_TILSTAND)
    }

    @Test
    fun `gir forbidden hvis saksbehandler mangler besluttertilgang`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer)
        val behandling = lagEnBehandling(behandlingId, vedtaksperiode.id())
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
        response.assertResponseMessage(PostFattVedtakBehandler.SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG)
    }

    @Test
    fun `gir forbidden hvis saksbehandler prøver å beslutte egen oppgave`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer)
        val behandling = lagEnBehandling(behandlingId, vedtaksperiode.id())
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
        response.assertResponseMessage(PostFattVedtakBehandler.SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE)
    }

    @Test
    fun `gir conflict hvis totrinnsvurdering mangler beslutter`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer)
        val behandling = lagEnBehandling(behandlingId, vedtaksperiode.id())
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
        response.assertResponseMessage(PostFattVedtakBehandler.TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER)
    }

    @Test
    fun `gir bad request hvis behandlingen overlapper med Infotrygd`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer)
        val behandling = lagEnBehandling(behandlingId, vedtaksperiode.id(), tags = setOf("OverlapperMedInfotrygd"))
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
        response.assertResponseMessage(PostFattVedtakBehandler.OVERLAPPER_MED_INFOTRYGD)
    }

    @Test
    fun `gir bad request hvis det finnes relevante varsler som ikke er vurdert`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiode = lagEnVedtaksperiode(UUID.randomUUID(), fødselsnummer)
        val behandling = lagEnBehandling(behandlingId, vedtaksperiode.id())
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
        varselRepository.lagre(Varsel.fraLagring(
            VarselId(UUID.randomUUID()),
            behandling.id(),
            status = Varsel.Status.AKTIV,
            vurdering = null,
            kode = kode
        ))
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
        response.assertResponseMessage(PostFattVedtakBehandler.VARSLER_MANGLER_VURDERING)
    }

    private fun lagEnOppgave(behandlingId: UUID): Oppgave = Oppgave.ny(
        id = nextLong(),
        førsteOpprettet = LocalDateTime.now(),
        vedtaksperiodeId = UUID.randomUUID(),
        behandlingId = behandlingId,
        utbetalingId = UUID.randomUUID(),
        hendelseId = UUID.randomUUID(),
        kanAvvises = true,
        egenskaper = emptySet(),
    )

    private fun lagEnSaksbehandler(): Saksbehandler = Saksbehandler(
        id = SaksbehandlerOid(UUID.randomUUID()),
        navn = "Navn Navnesen",
        epost = "navn@navnesen.no",
        ident = "L112233"
    )

    private fun lagEnBehandling(
        behandlingId: UUID,
        vedtaksperiodeId: VedtaksperiodeId,
        tags: Set<String> = emptySet(),
    ): Behandling = Behandling.fraLagring(
        id = SpleisBehandlingId(behandlingId),
        tags = tags,
        søknadIder = emptySet(),
        fom = 1.jan(2018),
        tom = 31.jan(2018),
        skjæringstidspunkt = 1.jan(2018),
        vedtaksperiodeId = vedtaksperiodeId,
    )

    private fun lagEnVedtaksperiode(
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
    ): Vedtaksperiode = Vedtaksperiode(VedtaksperiodeId(vedtaksperiodeId), fødselsnummer)
}

private fun IntegrationTestFixture.Response.assertResponseMessage(melding: String) {
    assertEquals(melding, this.bodyAsJsonNode?.get("message")?.asText())
}
