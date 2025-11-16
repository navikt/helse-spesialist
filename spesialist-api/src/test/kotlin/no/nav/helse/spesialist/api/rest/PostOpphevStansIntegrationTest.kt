package no.nav.helse.spesialist.api.rest

import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PostOpphevStansIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `Lagrer melding og notat når stans oppheves fra speil`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()

        val person = lagPerson(
            identitetsnummer = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)

        lagVedtaksperiode(
            id = VedtaksperiodeId(vedtaksperiodeId),
            identitetsnummer = person.identitetsnummer,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        sessionContext.oppgaveRepository.lagre(
            Oppgave.ny(
                id = 1,
                førsteOpprettet = null,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = UUID.randomUUID(),
                utbetalingId = UUID.randomUUID(),
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = emptySet(),
            )
        )
        val begrunnelse = "begrunnelse"

        // When:
        val response = integrationTestFixture.post(
            "/api/opphevstans",
            body = """{ "fodselsnummer": "$fødselsnummer", "begrunnelse": "$begrunnelse" }""",
            saksbehandler = saksbehandler,
        )
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        // Then:
        // Sjekk persistert data
        val lagredeStans = sessionContext.stansAutomatiskBehandlingDao.hentFor(fødselsnummer)
        assertEquals(1, lagredeStans.size)
        val lagretStans = lagredeStans.first()
        assertEquals(fødselsnummer, lagretStans.fødselsnummer)
        assertEquals("NORMAL", lagretStans.status)
        assertEquals(emptySet(), lagretStans.årsaker)
        assertEquals(null, lagretStans.meldingId)

        val notater = sessionContext.notatRepository.finnAlleForVedtaksperiode(vedtaksperiodeId)
        assertEquals(1, notater.size)
        val notat = notater.first()
        assertEquals(vedtaksperiodeId, notat.vedtaksperiodeId)
        assertEquals(NotatType.OpphevStans, notat.type)
        assertEquals(begrunnelse, notat.tekst)
        assertEquals(saksbehandler.id, notat.saksbehandlerOid)
        assertEquals(false, notat.feilregistrert)
        assertEquals(null, notat.feilregistrertTidspunkt)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser()
    }

}
