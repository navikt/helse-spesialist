package no.nav.helse.spesialist.api.rest.varsler.vurdering

import io.ktor.http.HttpStatusCode
import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagBehandlingUnikId
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteVarselvurderingBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext
    private val vedtaksperiodeRepository = sessionContext.vedtaksperiodeRepository
    private val behandlingRepository = sessionContext.behandlingRepository
    private val varseldefinisjonRepository = sessionContext.varseldefinisjonRepository
    private val varselRepository = sessionContext.varselRepository
    private val personRepository = sessionContext.personRepository

    @Test
    fun `slett vurdering av varsel - happy case`() {
        // given
        val saksbehandler = lagSaksbehandler()
        val person = lagPerson()
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varseldefinisjon = lagVarseldefinisjon()
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                saksbehandlerId = saksbehandler.id,
                vurdertDefinisjonId = varseldefinisjon.id,
                tidspunkt = LocalDateTime.now()
            )
        )
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(varseldefinisjon)
        varselRepository.lagre(varsel)
        personRepository.lagre(person)

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            saksbehandler = saksbehandler,
        )

        assertEquals(HttpStatusCode.OK.value, response.status)
    }

    @Test
    fun `Forbidden om saksbehandler ikke har tilgang til personen`() {
        // given
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Fortrolig)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val definisjon = lagVarseldefinisjon()
        val varsel = lagVarsel(behandlingUnikId = behandling.id, spleisBehandlingId = behandling.spleisBehandlingId)
        val saksbehandler = lagSaksbehandler()
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        personRepository.lagre(person)

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 403,
                "title": "Mangler tilgang til person",
                "code": "MANGLER_TILGANG_TIL_PERSON"
            }""",
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `Internal Server Error om behandlingen for det aktuelle varselet ikke finnes`() {
        // given
        val definisjon = lagVarseldefinisjon()
        val varsel = lagVarsel(behandlingUnikId = lagBehandlingUnikId(), spleisBehandlingId = lagSpleisBehandlingId())
        val saksbehandler = lagSaksbehandler()
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 500,
                "title": "Internal Server Error"
            }""",
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `Internal Server Error om vedtaksperioden for det aktuelle varselet ikke finnes`() {
        // given
        val definisjon = lagVarseldefinisjon()
        val behandling = lagBehandling(vedtaksperiodeId = lagVedtaksperiodeId())
        val varsel = lagVarsel(behandlingUnikId = behandling.id, spleisBehandlingId = behandling.spleisBehandlingId)
        val saksbehandler = lagSaksbehandler()
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        behandlingRepository.lagre(behandling)

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 500,
                "title": "Internal Server Error"
            }""",
            response.bodyAsJsonNode!!
        )
    }

    @ParameterizedTest
    @EnumSource(Varsel.Status::class, names = ["VURDERT", "AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `får conflict hvis status ikke er AKTIV eller VURDERT når man forsøker å fjerne vurdering`(status: Varsel.Status) {
        // given
        val saksbehandler = lagSaksbehandler()
        val person = lagPerson()
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varseldefinisjon = lagVarseldefinisjon()
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = status,
        )
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(varseldefinisjon)
        varselRepository.lagre(varsel)
        personRepository.lagre(person)

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.Conflict.value, response.status)
    }

    @Test
    fun `får no content hvis status er AKTIV og det ikke finnes en vurdering når man forsøker å fjerne vurdering`() {
        // given
        val saksbehandler = lagSaksbehandler()
        val person = lagPerson()
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varseldefinisjon = lagVarseldefinisjon()
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.AKTIV,
            vurdering = null
        )
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(varseldefinisjon)
        varselRepository.lagre(varsel)
        personRepository.lagre(person)

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)
    }
}
