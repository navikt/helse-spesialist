package no.nav.helse.spesialist.api.rest.oppgaver.påVent

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.periodehistorikk.FjernetFraPåVent
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagDialog
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagPåVent
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DeletePåVentBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository
    private val oppgaveRepository = integrationTestFixture.sessionFactory.sessionContext.oppgaveRepository
    private val vedtaksperiodeRepository = integrationTestFixture.sessionFactory.sessionContext.vedtaksperiodeRepository
    private val behandlingRepository = integrationTestFixture.sessionFactory.sessionContext.behandlingRepository
    private val saksbehandlerRepository = integrationTestFixture.sessionFactory.sessionContext.saksbehandlerRepository
    private val periodehistorikkDao = integrationTestFixture.sessionFactory.sessionContext.periodehistorikkDao
    private val påVentRepository = integrationTestFixture.sessionFactory.sessionContext.påVentRepository
    private val dialogRepository = integrationTestFixture.sessionFactory.sessionContext.dialogRepository

    @Test
    fun `happy path`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val oppgave = lagOppgave(spleisBehandlingId, UUID.randomUUID(), vedtaksperiode.id).also(oppgaveRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)
        val dialog = lagDialog().also(dialogRepository::lagre)
        val påVent = lagPåVent(vedtaksperiode.id, saksbehandler.id, dialogId = dialog.id()).also(påVentRepository::lagre)

        // when
        val response =
            integrationTestFixture.delete(
                url = "/api/oppgaver/${oppgave.id.value}/pa-vent",
                saksbehandler = saksbehandler,
            )

        assertEquals(HttpStatusCode.NoContent.value, response.status)
        val funnedeHistorikkinnslag = periodehistorikkDao.behandlingData[behandling.id.value] ?: emptyList()
        assertEquals(1, funnedeHistorikkinnslag.size)
        assertIs<FjernetFraPåVent>(funnedeHistorikkinnslag.single())
        assertEquals(oppgaveRepository.finn(oppgave.id)?.egenskaper?.contains(Egenskap.PÅ_VENT), false)
        assertNull(påVentRepository.finn(påVent.id()))
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = OppgaveOppdatert(oppgave),
                årsak = "Oppgave fjernet fra på vent",
            ),
        )
    }

    @Test
    fun `error hvis oppgave ikke finnes`() {
        val response = integrationTestFixture.delete(url = "/api/oppgaver/1/pa-vent")
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 404,
                "title": "Oppgave ikke funnet",
                "code": "OPPGAVE_IKKE_FUNNET"
            }""",
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `error hvis saksbehandler ikke har tilgang til personen`() {
        // given
        val person = lagPerson(erEgenAnsatt = true).also(personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val oppgave = lagOppgave(spleisBehandlingId, UUID.randomUUID(), vedtaksperiode.id).also(oppgaveRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)

        // when
        val response =
            integrationTestFixture.delete(
                url = "/api/oppgaver/${oppgave.id.value}/pa-vent",
                saksbehandler = saksbehandler,
                brukerroller = emptySet(),
            )
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 403,
                "title": "Mangler tilgang til person",
                "code": "MANGLER_TILGANG_TIL_PERSON"
            }""",
            response.bodyAsJsonNode!!,
        )
    }
}
