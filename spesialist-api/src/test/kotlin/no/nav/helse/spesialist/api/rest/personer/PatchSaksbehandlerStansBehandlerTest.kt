package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.modell.periodehistorikk.AutomatiskBehandlingStansetAvSaksbehandler
import no.nav.helse.modell.periodehistorikk.OpphevStansAvSaksbehandler
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertInstanceOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PatchSaksbehandlerStansBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `Lagrer historikk og stans når saksbehandler opphever saksbehandler-stans fra speil`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = lagVedtaksperiodeId()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        lagVedtaksperiode(
            id = vedtaksperiodeId,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        val behandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiodeId,
            ).also(sessionContext.behandlingRepository::lagre)

        val begrunnelse = "begrunnelse"

        sessionContext.saksbehandlerStansRepository.lagre(
            SaksbehandlerStans.ny(
                utførtAvSaksbehandlerIdent = NAVIdent("I123456"),
                begrunnelse = "Eksisterende stans",
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
            ),
        )

        // When:
        val response =
            integrationTestFixture.patch(
                "/api/personer/${personPseudoId.value}/stans/saksbehandler",
                body = """{ "begrunnelse": "$begrunnelse", "stans": false }""",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Skriv),
            )
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        // Then:
        val erStanset = sessionContext.saksbehandlerStansRepository.finnAktiv(Identitetsnummer.fraString(fødselsnummer))?.erStanset == false

        assertFalse(erStanset)

        val historikk = sessionContext.periodehistorikkDao.finnForBehandling(behandling.id)
        assertEquals(1, historikk.size)
        val historikkinnslag = historikk.first()
        assertInstanceOf<OpphevStansAvSaksbehandler>(historikkinnslag)
        assertEquals(saksbehandler, historikkinnslag.saksbehandler)
        assertEquals(begrunnelse, historikkinnslag.notattekst)
    }

    @Test
    fun `Lagrer historikk og stans når saksbehandler oppretter saksbehandler-stans fra speil`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = lagVedtaksperiodeId()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        lagVedtaksperiode(
            id = vedtaksperiodeId,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        val behandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiodeId,
            ).also(sessionContext.behandlingRepository::lagre)

        val begrunnelse = "begrunnelse"

        // When:
        val response =
            integrationTestFixture.patch(
                "/api/personer/${personPseudoId.value}/stans/saksbehandler",
                body = """{ "begrunnelse": "$begrunnelse", "stans": true }""",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Skriv),
            )
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        // Then:
        val erStanset = sessionContext.saksbehandlerStansRepository.finnAktiv(Identitetsnummer.fraString(fødselsnummer))?.erStanset == true
        assertTrue(erStanset)

        val historikk = sessionContext.periodehistorikkDao.finnForBehandling(behandling.id)
        assertEquals(1, historikk.size)
        val historikkinnslag = historikk.first()
        assertInstanceOf<AutomatiskBehandlingStansetAvSaksbehandler>(historikkinnslag)
        assertEquals(saksbehandler, historikkinnslag.saksbehandler)
        assertEquals(begrunnelse, historikkinnslag.notattekst)
    }
}
