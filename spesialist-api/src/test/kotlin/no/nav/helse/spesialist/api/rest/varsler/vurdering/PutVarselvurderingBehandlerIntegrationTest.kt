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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PutVarselvurderingBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext
    private val saksbehandlerRepository = sessionContext.saksbehandlerRepository
    private val vedtaksperiodeRepository = sessionContext.vedtaksperiodeRepository
    private val behandlingRepository = sessionContext.behandlingRepository
    private val varseldefinisjonRepository = sessionContext.varseldefinisjonRepository
    private val varselRepository = sessionContext.varselRepository
    private val personRepository = sessionContext.personRepository

    @Test
    fun `vurder varsel - happy case`() {
        // given
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert, erEgenAnsatt = false)
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
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
    }

    @Test
    fun `Forbidden om saksbehandler ikke har tilgang til personen`() {
        // given
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Fortrolig, erEgenAnsatt = false)
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
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
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
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
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
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
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
    fun `Internal Server Error om varselet har status VURDERT men mangler vurdering`() {
        // given
        val definisjon = lagVarseldefinisjon()
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert, erEgenAnsatt = false)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varsel = lagVarsel(behandlingUnikId = behandling.id, spleisBehandlingId = behandling.spleisBehandlingId, status = Varsel.Status.VURDERT, vurdering = null)
        val saksbehandler = lagSaksbehandler()
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        personRepository.lagre(person)

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
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
    fun `Conflict om varselet er vurdert, men av en annen saksbehandler`() {
        // given
        val definisjon = lagVarseldefinisjon()
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert, erEgenAnsatt = false)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val enAnnenSaksbehandler = lagSaksbehandler()
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                enAnnenSaksbehandler.id,
                tidspunkt = LocalDateTime.now(),
                definisjon.id
            )
        )
        val saksbehandler = lagSaksbehandler()
        saksbehandlerRepository.lagre(enAnnenSaksbehandler)
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        behandlingRepository.lagre(behandling)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        personRepository.lagre(person)

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
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
        val definisjon = lagVarseldefinisjon()
        val enAnnenDefinisjonId = lagVarseldefinisjon()
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert, erEgenAnsatt = false)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                saksbehandler.id,
                tidspunkt = LocalDateTime.now(),
                enAnnenDefinisjonId.id
            )
        )
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        behandlingRepository.lagre(behandling)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        personRepository.lagre(person)

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
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
        val definisjon = lagVarseldefinisjon()
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert, erEgenAnsatt = false)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = Varsel.Status.VURDERT,
            vurdering = Varselvurdering(
                saksbehandler.id,
                tidspunkt = LocalDateTime.now(),
                definisjon.id
            )
        )
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        behandlingRepository.lagre(behandling)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        personRepository.lagre(person)

        // when
        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV", "VURDERT"], mode = EnumSource.Mode.EXCLUDE)
    fun `får 409-feil hvis status på varselet er noe annet enn AKTIV eller VURDERT når man vurderer varsel`(status: Varsel.Status) {
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert, erEgenAnsatt = false)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val definisjon = lagVarseldefinisjon()
        val saksbehandler = lagSaksbehandler()
        val varsel = lagVarsel(
            behandlingUnikId = behandling.id,
            spleisBehandlingId = behandling.spleisBehandlingId,
            status = status,
        )
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)
        varseldefinisjonRepository.lagre(definisjon)
        varselRepository.lagre(varsel)
        personRepository.lagre(person)

        val response = integrationTestFixture.put(
            url = "/api/varsler/${varsel.id.value}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        assertEquals(HttpStatusCode.Conflict.value, response.status)
    }

    @Test
    fun `404 hvis varselet ikke finnes for settVarselstatus`() {
        val definisjon = lagVarseldefinisjon()
        val saksbehandler = lagSaksbehandler()

        val response = integrationTestFixture.put(
            url = "/api/varsler/${UUID.randomUUID()}/vurdering",
            body = """{
                          "definisjonId": "${definisjon.id.value}"
                        }""",
            saksbehandler = saksbehandler,
        )

        assertEquals(HttpStatusCode.NotFound.value, response.status)
    }
}
