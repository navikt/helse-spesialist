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
import no.nav.helse.spesialist.domain.testfixtures.lagEnVarseldefinisjonId
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PutVarselvurderingBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext
    private val saksbehandlerRepository = sessionContext.saksbehandlerRepository
    private val vedtaksperiodeRepository = sessionContext.vedtaksperiodeRepository
    private val behandlingRepository = sessionContext.behandlingRepository
    private val varseldefinisjonRepository = sessionContext.varseldefinisjonRepository
    private val varselRepository = sessionContext.varselRepository
    private val personDao = sessionContext.personDao
    private val egenAnsattDao = sessionContext.egenAnsattDao

    @Test
    fun `vurder varsel - happy case`() {
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
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        // then
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
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
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
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
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
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
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
    fun `Internal Server Error om varselet har status VURDERT men mangler vurdering`() {
        // given
        val definisjon = lagEnVarseldefinisjon()
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val varsel = lagEtVarsel(behandlingUnikId = behandling.id(), spleisBehandlingId = behandling.spleisBehandlingId, status = Varsel.Status.VURDERT, vurdering = null)
        val saksbehandler = lagEnSaksbehandler()
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
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
    fun `Conflict om varselet er vurdert, men av en annen saksbehandler`() {
        // given
        val definisjon = lagEnVarseldefinisjon()
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val enAnnenSaksbehandler = lagEnSaksbehandler()
        val varsel = lagEtVarsel(
            behandlingUnikId = behandling.id(),
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                enAnnenSaksbehandler.id(),
                tidspunkt = LocalDateTime.now(),
                definisjon.id()
            )
        )
        val saksbehandler = lagEnSaksbehandler()
        saksbehandlerRepository.lagre(enAnnenSaksbehandler)
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        behandlingRepository.lagre(behandling)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        integrationTestFixture.assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 409,
                "title": "Varsel har blitt vurdert av en annen saksbehandler",
                "code": "VARSEL_VURDERT_AV_ANNEN_SAKSBEHANDLER"
            }""",
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `Conflict om varselet er vurdert, men basert på en annen definisjon`() {
        // given
        val definisjon = lagEnVarseldefinisjon()
        val enAnnenDefinisjonId = lagEnVarseldefinisjonId()
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val saksbehandler = lagEnSaksbehandler()
        val varsel = lagEtVarsel(
            behandlingUnikId = behandling.id(),
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                saksbehandler.id(),
                tidspunkt = LocalDateTime.now(),
                enAnnenDefinisjonId
            )
        )
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        behandlingRepository.lagre(behandling)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        integrationTestFixture.assertJsonEquals(
            """{
                "type": "about:blank",
                "status": 409,
                "title": "Varsel har blitt vurdert basert på en annen definisjon",
                "code": "VARSEL_VURDERT_MED_ANNEN_DEFINISJON"
            }""",
            response.bodyAsJsonNode!!
        )
    }

    @Test
    fun `NoContent om varsel er vurdert men ingen endringer`() {
        // given
        val definisjon = lagEnVarseldefinisjon()
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val saksbehandler = lagEnSaksbehandler()
        val varsel = lagEtVarsel(
            behandlingUnikId = behandling.id(),
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                saksbehandler.id(),
                tidspunkt = LocalDateTime.now(),
                definisjon.id()
            )
        )
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        behandlingRepository.lagre(behandling)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV", "VURDERT"], mode = EnumSource.Mode.EXCLUDE)
    fun `får 409-feil hvis status på varselet er noe annet enn AKTIV eller VURDERT når man vurderer varsel`(status: Varsel.Status) {
        val vedtaksperiode = lagEnVedtaksperiode()
        val behandling = lagEnBehandling(vedtaksperiodeId = vedtaksperiode.id())
        val definisjon = lagEnVarseldefinisjon()
        val saksbehandler = lagEnSaksbehandler()
        val varsel = lagEtVarsel(
            behandlingUnikId = behandling.id(),
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = status,
        )
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        egenAnsattDao.lagre(vedtaksperiode.fødselsnummer, false, LocalDateTime.now())
        personDao.upsertPersoninfo(
            vedtaksperiode.fødselsnummer, lagFornavn(), lagMellomnavn(), lagEtternavn(), LocalDate.now(),
            Kjønn.Ukjent, Adressebeskyttelse.Ugradert
        )

        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id().value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        assertEquals(HttpStatusCode.Conflict.value, response.status)
    }

    @Test
    fun `404 hvis varselet ikke finnes for settVarselstatus`() {
        val definisjon = lagEnVarseldefinisjon()
        val saksbehandler = lagEnSaksbehandler()

        val response = integrationTestFixture.put(
            url = "/api/varsler/${UUID.randomUUID()}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id().value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        assertEquals(HttpStatusCode.NotFound.value, response.status)
    }
}
