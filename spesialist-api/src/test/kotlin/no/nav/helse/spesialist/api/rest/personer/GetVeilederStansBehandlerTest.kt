package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.rest.ApiVeilederStans
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VeilederStans
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetVeilederStansBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `Returnerer aktiv veileder-stans for person`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id)

        val opprettet = Instant.now()
        val veilederStans =
            VeilederStans.ny(
                identitetsnummer = person.id,
                årsaker = setOf(VeilederStans.StansÅrsak.MEDISINSK_VILKAR, VeilederStans.StansÅrsak.AKTIVITETSKRAV),
                opprettet = opprettet,
                originalMeldingId = UUID.randomUUID(),
            )
        sessionContext.veilederStansRepository.lagre(veilederStans)

        // When:
        val response =
            integrationTestFixture.get(
                "/api/personer/${personPseudoId.value}/stans/veileder",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.body<ApiVeilederStans>()
        assertEquals(
            setOf(ApiVeilederStans.Årsak.MEDISINSK_VILKAR, ApiVeilederStans.Årsak.AKTIVITETSKRAV),
            body.årsaker,
        )
        val forventetTidspunkt = opprettet.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()
        assertEquals(forventetTidspunkt, body.tidspunkt)
    }

    @Test
    fun `Returnerer 404 når ingen aktiv stans finnes`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response =
            integrationTestFixture.get(
                "/api/personer/${personPseudoId.value}/stans/veileder",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(404, response.status)
    }

    @Test
    fun `Returnerer 404 når stansen er opphevet`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id)

        val veilederStans =
            VeilederStans.ny(
                identitetsnummer = person.id,
                årsaker = setOf(VeilederStans.StansÅrsak.MEDISINSK_VILKAR),
                opprettet = Instant.now(),
                originalMeldingId = UUID.randomUUID(),
            )
        veilederStans.opphevStans(
            opphevetAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Situasjonen er avklart",
        )
        sessionContext.veilederStansRepository.lagre(veilederStans)

        // When:
        val response =
            integrationTestFixture.get(
                "/api/personer/${personPseudoId.value}/stans/veileder",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(404, response.status)
    }

    @Test
    fun `Returnerer 404 for ugyldig PersonPseudoId`() {
        // Given:
        val saksbehandler = lagSaksbehandler()
        val ugyldigPseudoId = UUID.randomUUID()

        // When:
        val response =
            integrationTestFixture.get(
                "/api/personer/$ugyldigPseudoId/stans/veileder",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(404, response.status)
    }
}
