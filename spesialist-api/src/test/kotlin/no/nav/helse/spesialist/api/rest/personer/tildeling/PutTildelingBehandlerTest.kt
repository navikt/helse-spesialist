package no.nav.helse.spesialist.api.rest.personer.tildeling

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PutTildelingBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository
    private val oppgaveRepository = integrationTestFixture.sessionFactory.sessionContext.oppgaveRepository
    private val vedtaksperiodeRepository = integrationTestFixture.sessionFactory.sessionContext.vedtaksperiodeRepository
    private val behandlingRepository = integrationTestFixture.sessionFactory.sessionContext.behandlingRepository
    private val saksbehandlerRepository = integrationTestFixture.sessionFactory.sessionContext.saksbehandlerRepository

    @Test
    fun `happy path`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId =
            integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
                .nyPersonPseudoId(person.id)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val oppgave = lagOppgave(spleisBehandlingId, UUID.randomUUID(), vedtaksperiode.id).also(oppgaveRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)

        // when
        val response =
            integrationTestFixture.put(
                url = "/api/personer/${personPseudoId.value}/tildeling",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "navident": "${saksbehandler.ident.value}"
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        assertEquals(saksbehandler.id, oppgave.tildeltTil)
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = OppgaveOppdatert(oppgave),
                årsak = "Oppgave tildelt",
            ),
        )
    }

    @Test
    fun `forbidden hvis saksbehandler ikke har tilgang til oppgaven`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId =
            integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
                .nyPersonPseudoId(person.id)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val oppgave =
            lagOppgave(
                behandlingId = spleisBehandlingId,
                godkjenningsbehovId = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiode.id,
            ).also {
                it.leggTilEgenAnsatt()
                oppgaveRepository.lagre(it)
            }

        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)

        // when
        val response =
            integrationTestFixture.put(
                url = "/api/personer/${personPseudoId.value}/tildeling",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "navident": "${saksbehandler.ident.value}"
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        assertEquals(null, oppgave.tildeltTil)
    }

    @Test
    fun `no content hvis saksbehandler allerede er tildelt oppgaven`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId =
            integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
                .nyPersonPseudoId(person.id)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)
        val oppgave =
            lagOppgave(
                behandlingId = spleisBehandlingId,
                godkjenningsbehovId = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiode.id,
            ).also {
                it.forsøkTildeling(saksbehandler, emptySet())
                oppgaveRepository.lagre(it)
                it.konsumerHendelser() // nullstill hendelser som ikke er relevante for testen
            }

        // when
        val response =
            integrationTestFixture.put(
                url = "/api/personer/${personPseudoId.value}/tildeling",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "navident": "${saksbehandler.ident.value}"
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        assertEquals(saksbehandler.id, oppgave.tildeltTil)
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `conflict hvis oppgaven er tildelt en annen saksbehandler`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId =
            integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
                .nyPersonPseudoId(person.id)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(behandlingRepository::lagre)
        val spleisBehandlingId = behandling.spleisBehandlingId!!
        val saksbehandlerSomErTildeltOppgaven = lagSaksbehandler()
        val oppgave =
            lagOppgave(
                behandlingId = spleisBehandlingId,
                godkjenningsbehovId = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiode.id,
            ).also {
                it.forsøkTildeling(saksbehandlerSomErTildeltOppgaven, emptySet())
                oppgaveRepository.lagre(it)
                it.konsumerHendelser() // nullstill hendelser som ikke er relevante for testen
            }

        val saksbehandler = lagSaksbehandler().also(saksbehandlerRepository::lagre)

        // when
        val response =
            integrationTestFixture.put(
                url = "/api/personer/${personPseudoId.value}/tildeling",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "navident": "${saksbehandler.ident.value}"
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertEquals(saksbehandlerSomErTildeltOppgaven.id, oppgave.tildeltTil)
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }
}
