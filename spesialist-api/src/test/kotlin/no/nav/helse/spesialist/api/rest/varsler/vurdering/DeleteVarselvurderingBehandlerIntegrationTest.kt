package no.nav.helse.spesialist.api.rest.varsler.vurdering

import io.ktor.http.HttpStatusCode
import no.nav.helse.Varselvurdering
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.testfixtures.lagEnBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagEnBehandlingUnikId
import no.nav.helse.spesialist.domain.testfixtures.lagEnSaksbehandler
import no.nav.helse.spesialist.domain.testfixtures.lagEnSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagEnVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagEnVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagEnVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagEtVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavn
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
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
    private val personDao = sessionContext.personDao
    private val egenAnsattDao = sessionContext.egenAnsattDao

    @Test
    fun `slett vurdering av varsel - happy case`() {
        // given
        val saksbehandler = lagEnSaksbehandler()
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val varseldefinisjon = lagEnVarseldefinisjon()
        val varsel = lagEtVarsel(
            behandlingUnikId = behandling.id(),
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                saksbehandlerId = saksbehandler.id(),
                vurdertDefinisjonId = varseldefinisjon.id(),
                tidspunkt = LocalDateTime.now()
            )
        )
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(varseldefinisjon)
        varselRepository.lagre(varsel)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            saksbehandler = saksbehandler,
        )

        assertEquals(HttpStatusCode.OK.value, response.status)
    }

    @Test
    fun `Forbidden om saksbehandler ikke har tilgang til personen`() {
        // given
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val definisjon = lagEnVarseldefinisjon()
        val varsel = lagEtVarsel(behandlingUnikId = behandling.id(), spleisBehandlingId = behandling.spleisBehandlingId)
        val saksbehandler = lagEnSaksbehandler()
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Fortrolig
        )

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val definisjon = lagEnVarseldefinisjon()
        val varsel = lagEtVarsel(behandlingUnikId = lagEnBehandlingUnikId(), spleisBehandlingId = lagEnSpleisBehandlingId())
        val saksbehandler = lagEnSaksbehandler()
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val definisjon = lagEnVarseldefinisjon()
        val behandling = lagEnBehandling(vedtaksperiodeId = lagEnVedtaksperiodeId())
        val varsel = lagEtVarsel(behandlingUnikId = behandling.id(), spleisBehandlingId = behandling.spleisBehandlingId)
        val saksbehandler = lagEnSaksbehandler()
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        behandlingRepository.lagre(behandling)

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        integrationTestFixture.assertJsonEquals(
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
        val saksbehandler = lagEnSaksbehandler()
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val varseldefinisjon = lagEnVarseldefinisjon()
        val varsel = lagEtVarsel(
            behandlingUnikId = behandling.id(),
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = status,
        )
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(varseldefinisjon)
        varselRepository.lagre(varsel)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.Conflict.value, response.status)
    }

    @Test
    fun `får no content hvis status er AKTIV og det ikke finnes en vurdering når man forsøker å fjerne vurdering`() {
        // given
        val saksbehandler = lagEnSaksbehandler()
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val varseldefinisjon = lagEnVarseldefinisjon()
        val varsel = lagEtVarsel(
            behandlingUnikId = behandling.id(),
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.AKTIV,
            vurdering = null
        )
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(varseldefinisjon)
        varselRepository.lagre(varsel)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // when
        val response = integrationTestFixture.delete(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)
    }
}
