package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.rest.ApiSaksbehandlerStans
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSaksbehandlerStansBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `Returnerer aktiv saksbehandler-stans for person`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id)
        val begrunnelse = "Begrunnelse"

        val stans =
            SaksbehandlerStans.ny(
                utførtAvSaksbehandlerIdent = saksbehandler.ident,
                begrunnelse = begrunnelse,
                identitetsnummer = person.id,
            )
        sessionContext.saksbehandlerStansRepository.lagre(stans)

        // When:
        val response =
            integrationTestFixture.get(
                "/api/personer/${personPseudoId.value}/stans/saksbehandler",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.body<ApiSaksbehandlerStans>()
        assertEquals(true, body.erStanset)
        assertEquals(saksbehandler.ident.value, body.utførtAv)
        assertEquals(begrunnelse, body.begrunnelse)
        val forventetTidspunkt = stans.opprettet.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()
        assertEquals(forventetTidspunkt, body.opprettetTidspunkt)
    }

    @Test
    fun `Returnerer erStanset=false når ingen aktiv stans finnes`() {
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
                "/api/personer/${personPseudoId.value}/stans/saksbehandler",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.body<ApiSaksbehandlerStans>()
        assertEquals(false, body.erStanset)
        assertEquals(null, body.utførtAv)
        assertEquals(null, body.begrunnelse)
        assertEquals(null, body.opprettetTidspunkt)
    }

    @Test
    fun `Returnerer erStanset=false når stansen er opphevet`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id)

        val stans =
            SaksbehandlerStans.ny(
                utførtAvSaksbehandlerIdent = saksbehandler.ident,
                begrunnelse = "Begrunnelse",
                identitetsnummer = person.id,
            )
        stans.opphevStans(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Begrunnelse",
        )
        sessionContext.saksbehandlerStansRepository.lagre(stans)

        // When:
        val response =
            integrationTestFixture.get(
                "/api/personer/${personPseudoId.value}/stans/saksbehandler",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.body<ApiSaksbehandlerStans>()
        assertEquals(false, body.erStanset)
        assertEquals(null, body.utførtAv)
        assertEquals(null, body.begrunnelse)
        assertEquals(null, body.opprettetTidspunkt)
    }

    @Test
    fun `Returnerer 404 for ugyldig PersonPseudoId`() {
        // Given:
        val saksbehandler = lagSaksbehandler()
        val ugyldigPseudoId = UUID.randomUUID()

        // When:
        val response =
            integrationTestFixture.get(
                "/api/personer/$ugyldigPseudoId/stans/saksbehandler",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(404, response.status)
    }
}
