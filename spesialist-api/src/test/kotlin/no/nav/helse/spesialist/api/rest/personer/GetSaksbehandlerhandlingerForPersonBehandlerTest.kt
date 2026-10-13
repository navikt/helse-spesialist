package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.rest.ApiHandlingType
import no.nav.helse.spesialist.api.rest.ApiSaksbehandlerhandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling
import no.nav.helse.spesialist.domain.saksbehandlerhandling.SaksbehandlerhandlingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSaksbehandlerhandlingerForPersonBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `returnerer alle handlinger for personen med nyeste først`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val annenPerson = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val behandlingId = UUID.randomUUID()

        val eldst =
            lagHandling(
                identitetsnummer = person.id,
                tidspunkt = Instant.parse("2026-10-01T10:00:00Z"),
                skjæringstidspunkt = LocalDate.parse("2026-09-01"),
                behandlingId = SpleisBehandlingId(behandlingId),
            )
        val nyest = lagHandling(identitetsnummer = person.id, tidspunkt = Instant.parse("2026-10-02T10:00:00Z"))
        listOf(eldst, nyest, lagHandling(identitetsnummer = annenPerson.id))
            .forEach(sessionContext.saksbehandlerhandlingRepository::lagre)

        // When:
        val response = hent(personPseudoId.value)

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        val body = response.body<List<ApiSaksbehandlerhandling>>()
        assertEquals(listOf(nyest.handlingId, eldst.handlingId), body.map { it.handlingId })
        assertEquals(
            ApiSaksbehandlerhandling(
                handlingId = eldst.handlingId,
                handlingType = ApiHandlingType.MANUELT_VURDERT_OPPTJENING,
                saksbehandlerIdent = "A123456",
                tidspunkt = LocalDateTime.parse("2026-10-01T12:00:00"),
                skjæringstidspunkt = LocalDate.parse("2026-09-01"),
                behandlingId = behandlingId,
                erDelAvAktivTotrinnsvurdering = false,
            ),
            body.last(),
        )
    }

    @Test
    fun `markerer handlinger som er del av aktiv totrinnsvurdering`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        val godkjent =
            Totrinnsvurdering
                .ny(fødselsnummer = person.id.value)
                .also { it.ferdigstill() }
                .also(sessionContext.totrinnsvurderingRepository::lagre)
        val aktiv =
            Totrinnsvurdering
                .ny(fødselsnummer = person.id.value)
                .also(sessionContext.totrinnsvurderingRepository::lagre)

        val iAktiv = lagHandling(identitetsnummer = person.id, totrinnsvurderingId = aktiv.id())
        val iGodkjent = lagHandling(identitetsnummer = person.id, totrinnsvurderingId = godkjent.id())
        val utenTotrinnsvurdering = lagHandling(identitetsnummer = person.id)
        listOf(iAktiv, iGodkjent, utenTotrinnsvurdering).forEach(sessionContext.saksbehandlerhandlingRepository::lagre)

        // When:
        val response = hent(personPseudoId.value)

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        val erDelAvAktiv =
            response
                .body<List<ApiSaksbehandlerhandling>>()
                .associate { it.handlingId to it.erDelAvAktivTotrinnsvurdering }
        assertEquals(true, erDelAvAktiv[iAktiv.handlingId])
        assertEquals(false, erDelAvAktiv[iGodkjent.handlingId])
        assertEquals(false, erDelAvAktiv[utenTotrinnsvurdering.handlingId])
    }

    @Test
    fun `returnerer tom liste når personen ikke har handlinger`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response = hent(personPseudoId.value)

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertEquals(emptyList(), response.body<List<ApiSaksbehandlerhandling>>())
    }

    @Test
    fun `returnerer 404 for ukjent PersonPseudoId`() {
        // When:
        val response = hent(UUID.randomUUID())

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
    }

    private fun hent(pseudoId: UUID) =
        integrationTestFixture.get(
            "/api/personer/$pseudoId/saksbehandlerhandlinger",
            saksbehandler = lagSaksbehandler(),
            tilganger = setOf(Tilgang.Les),
        )

    private fun lagHandling(
        identitetsnummer: Identitetsnummer,
        tidspunkt: Instant = Instant.now(),
        totrinnsvurderingId: TotrinnsvurderingId? = null,
        skjæringstidspunkt: LocalDate? = null,
        behandlingId: SpleisBehandlingId? = null,
    ) = Saksbehandlerhandling.ny(
        id = SaksbehandlerhandlingId(UUID.randomUUID()),
        identitetsnummer = identitetsnummer,
        handlingId = UUID.randomUUID(),
        handlingType = HandlingType.MANUELT_VURDERT_OPPTJENING,
        saksbehandlerIdent = "A123456",
        tidspunkt = tidspunkt,
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingId = behandlingId,
        totrinnsvurderingId = totrinnsvurderingId,
    )
}
