package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavn
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
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

    @Test
    fun `gir 404 hvis behandlingen ikke finnes`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val saksbehandler = Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Navn Navnesen",
            epost = "navn@navnesen.no",
            ident = "L112233"
        )
        saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
    }

    @Test
    fun `gir forbidden hvis saksbehandler ikke har tilgang til personen`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val behandling = Behandling.fraLagring(
            id = SpleisBehandlingId(behandlingId),
            tags = emptySet(),
            fødselsnummer = fødselsnummer,
            søknadIder = emptySet()
        )
        val saksbehandler = Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Navn Navnesen",
            epost = "navn@navnesen.no",
            ident = "L112233"
        )
        saksbehandlerRepository.lagre(saksbehandler)
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
        val behandling = Behandling.fraLagring(
            id = SpleisBehandlingId(behandlingId),
            tags = emptySet(),
            fødselsnummer = fødselsnummer,
            søknadIder = emptySet()
        )
        val saksbehandler = Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Navn Navnesen",
            epost = "navn@navnesen.no",
            ident = "L112233"
        )
        saksbehandlerRepository.lagre(saksbehandler)
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
        val behandling = Behandling.fraLagring(
            id = SpleisBehandlingId(behandlingId),
            tags = emptySet(),
            fødselsnummer = fødselsnummer,
            søknadIder = emptySet()
        )
        val saksbehandler = Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Navn Navnesen",
            epost = "navn@navnesen.no",
            ident = "L112233"
        )
        saksbehandlerRepository.lagre(saksbehandler)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )
        val oppgave = Oppgave.ny(
            id = nextLong(),
            førsteOpprettet = LocalDateTime.now(),
            vedtaksperiodeId = UUID.randomUUID(),
            behandlingId = behandlingId,
            utbetalingId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            kanAvvises = true,
            egenskaper = emptySet(),
        )
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
}

private fun IntegrationTestFixture.Response.assertResponseMessage(melding: String) {
    assertEquals(melding, this.bodyAsJsonNode?.get("message")?.asText())
}