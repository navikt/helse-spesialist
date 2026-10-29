package no.nav.helse.spesialist.api.rest.personer

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagPåVent
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetPaVentForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider

    @Test
    fun `gir påVent for vedtaksperioden med aktiv oppgave`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        val vedtaksperiode = lagVedtaksperiodeMedOppgave(person)
        val saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID())
        lagPåVent(
            vedtaksperiodeId = vedtaksperiode.id,
            saksbehandlerOid = saksbehandlerOid,
            frist = LocalDate.of(2026, 11, 1),
            dialogId = DialogId(1),
        ).also(sessionContext.påVentRepository::lagre)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/pa-vent")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            {
              "paVent": {
                "frist": "2026-11-01",
                "oid": "${saksbehandlerOid.value}"
              }
            }
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir null når den aktive oppgaven ikke er på vent`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        lagVedtaksperiodeMedOppgave(person)

        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/pa-vent")

        assertEquals(200, response.status)
        assertJsonEquals("""{ "paVent": null }""", response.bodyAsText)
    }

    @Test
    fun `gir null når påVent ligger på en vedtaksperiode uten aktiv oppgave`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        val vedtaksperiodeUtenOppgave =
            lagVedtaksperiode(identitetsnummer = person.id).also(sessionContext.vedtaksperiodeRepository::lagre)
        lagPåVent(
            vedtaksperiodeId = vedtaksperiodeUtenOppgave.id,
            saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
            dialogId = DialogId(1),
        ).also(sessionContext.påVentRepository::lagre)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/pa-vent")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("""{ "paVent": null }""", response.bodyAsText)
    }

    @Test
    fun `gir 404 når pseudoId ikke finnes`() {
        val response = integrationTestFixture.get("/api/personer/${PersonPseudoId.ny().value}/pa-vent")

        assertEquals(404, response.status)
    }

    @ParameterizedTest
    @EnumSource(TilgangSomMangler::class)
    fun `gir 403 når saksbehandler mangler tilgang til personen`(tilgangSomMangler: TilgangSomMangler) {
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(tilgangSomMangler)
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/pa-vent")

        assertEquals(403, response.status)
    }

    @Test
    fun `krever lesetilgang`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/pa-vent",
                tilganger = emptySet<Tilgang>(),
            )

        assertEquals(403, response.status)
    }

    private fun lagVedtaksperiodeMedOppgave(person: Person) =
        lagVedtaksperiode(identitetsnummer = person.id).also { vedtaksperiode ->
            sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
            val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(sessionContext.behandlingRepository::lagre)
            lagOppgave(behandling.spleisBehandlingId!!, UUID.randomUUID(), vedtaksperiode.id)
                .also(sessionContext.oppgaveRepository::lagre)
        }
}
