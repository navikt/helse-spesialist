package no.nav.helse.spesialist.api.rest.varsler

import io.ktor.http.HttpStatusCode
import no.nav.helse.Varselvurdering
import no.nav.helse.mediator.asLocalDateTime
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
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

class GetVarselBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `gir OK og tilbake et varsel i happy case`() {
        // given
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiode = lagVedtaksperiode()
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varseldefinisjon = lagVarseldefinisjon(kode = "RV_IV_1")
        val opprettetTidspunkt = LocalDateTime.now()
        val vurdertTidspunkt = LocalDateTime.now()
        val varsel =
            lagVarsel(
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
                status = Varsel.Status.VURDERT,
                vurdering =
                    Varselvurdering(
                        saksbehandlerId = saksbehandler.id,
                        vurdertDefinisjonId = varseldefinisjon.id,
                        tidspunkt = vurdertTidspunkt,
                    ),
                kode = "RV_IV_1",
                opprettet = opprettetTidspunkt,
            )
        sessionContext.varseldefinisjonRepository.lagre(varseldefinisjon)
        sessionContext.varselRepository.lagre(varsel)
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(id = vedtaksperiode.identitetsnummer)
            .also(sessionContext.personRepository::lagre)

        // when
        val response = integrationTestFixture.get("/api/varsler/${varsel.id.value}")

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            {
              "id":  "${varsel.id.value}",
              "definisjonId": "${varseldefinisjon.id.value}",
              "tittel": "${varseldefinisjon.tittel}",
              "forklaring": "${varseldefinisjon.forklaring}",
              "handling": "${varseldefinisjon.handling}",
              "status": "VURDERT",
              "vurdering": {
                "ident": "${saksbehandler.ident.value}"
              }
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
            "opprettet",
            "vurdering.tidsstempel",
        )

        assertEquals(opprettetTidspunkt, response.bodyAsJsonNode.get("opprettet")?.asLocalDateTime())
        assertEquals(
            vurdertTidspunkt,
            response.bodyAsJsonNode
                .get("vurdering")
                ?.get("tidsstempel")
                ?.asLocalDateTime(),
        )
    }

    @Test
    fun `gir 404 dersom varsel ikke finnes`() {
        val varselId = VarselId(UUID.randomUUID())
        val response = integrationTestFixture.get("/api/varsler/${varselId.value}")
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "Fant ikke varsel",
              "code": "VARSEL_IKKE_FUNNET" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir 500 dersom behandlingen for det aktuelle varselet ikke finnes`() {
        // given
        val varsel =
            lagVarsel(
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                behandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
                vurdering = null,
            )
        sessionContext.varselRepository.lagre(varsel)

        // when
        val response = integrationTestFixture.get("/api/varsler/${varsel.id.value}")

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir 500 dersom vedtaksperioden for den aktuelle behandlingen ikke finnes`() {
        // given
        val vedtaksperiode = lagVedtaksperiode(id = lagVedtaksperiodeId())
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varsel =
            lagVarsel(
                behandlingUnikId = behandling.id,
                spleisBehandlingId = behandling.spleisBehandlingId,
                vurdering = null,
            )
        sessionContext.varselRepository.lagre(varsel)
        sessionContext.behandlingRepository.lagre(behandling)

        // when
        val response = integrationTestFixture.get("/api/varsler/${varsel.id.value}")

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir 403 dersom saksbehandler ikke har tilgang til den aktuelle personen`() {
        // given
        val vedtaksperiode = lagVedtaksperiode(id = lagVedtaksperiodeId())
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varsel =
            lagVarsel(
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
            )
        sessionContext.varselRepository.lagre(varsel)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        lagPerson(
            id = vedtaksperiode.identitetsnummer,
            adressebeskyttelse = Personinfo.Adressebeskyttelse.Fortrolig,
        ).also(sessionContext.personRepository::lagre)

        // when
        val response = integrationTestFixture.get("/api/varsler/${varsel.id.value}")

        // then
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 403,
              "title": "Mangler tilgang til person",
              "code": "MANGLER_TILGANG_TIL_PERSON" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir 500 dersom varseldefinisjon ikke finnes for det aktuelle varselet når varselet ikke er vurdert enda`() {
        // given
        val vedtaksperiode = lagVedtaksperiode(id = lagVedtaksperiodeId())
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varsel =
            lagVarsel(
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
            )
        sessionContext.varselRepository.lagre(varsel)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(id = vedtaksperiode.identitetsnummer)
            .also(sessionContext.personRepository::lagre)

        // when
        val response = integrationTestFixture.get("/api/varsler/${varsel.id.value}")

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir 500 dersom saksbehandler som vurderte varselet ikke finnes`() {
        // given
        val varseldefinisjon = lagVarseldefinisjon()
        val vedtaksperiode = lagVedtaksperiode()
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varsel =
            lagVarsel(
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
                status = Varsel.Status.VURDERT,
                vurdering =
                    Varselvurdering(
                        saksbehandlerId = SaksbehandlerOid(UUID.randomUUID()),
                        vurdertDefinisjonId = varseldefinisjon.id,
                        tidspunkt = LocalDateTime.now(),
                    ),
            )
        sessionContext.varseldefinisjonRepository.lagre(varseldefinisjon)
        sessionContext.varselRepository.lagre(varsel)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(id = vedtaksperiode.identitetsnummer)
            .also(sessionContext.personRepository::lagre)

        // when
        val response = integrationTestFixture.get("/api/varsler/${varsel.id.value}")

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir 500 dersom varseldefinisjon ikke finnes for det aktuelle varselet når varselet er vurdert`() {
        // given
        val vedtaksperiode = lagVedtaksperiode(id = lagVedtaksperiodeId())
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        val varsel =
            lagVarsel(
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
                status = Varsel.Status.VURDERT,
                vurdering =
                    Varselvurdering(
                        saksbehandlerId = saksbehandler.id,
                        vurdertDefinisjonId = VarseldefinisjonId(UUID.randomUUID()),
                        tidspunkt = LocalDateTime.now(),
                    ),
            )
        sessionContext.varselRepository.lagre(varsel)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        lagPerson(id = vedtaksperiode.identitetsnummer)
            .also(sessionContext.personRepository::lagre)

        // when
        val response = integrationTestFixture.get("/api/varsler/${varsel.id.value}")

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @ParameterizedTest
    @EnumSource(Varsel.Status::class, names = ["INAKTIV", "AVVIKLET", "AVVIST"], mode = EnumSource.Mode.INCLUDE)
    fun `gir 500 dersom varselet har en status som medfører at varselet ikke skal vises i Speil`(status: Varsel.Status) {
        // given
        val varseldefinisjon = lagVarseldefinisjon()
        val vedtaksperiode = lagVedtaksperiode(id = lagVedtaksperiodeId())
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val varsel =
            lagVarsel(
                spleisBehandlingId = behandling.spleisBehandlingId,
                behandlingUnikId = behandling.id,
                status = status,
            )
        sessionContext.varseldefinisjonRepository.lagre(varseldefinisjon)
        sessionContext.varselRepository.lagre(varsel)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)
        lagPerson(id = vedtaksperiode.identitetsnummer)
            .also(sessionContext.personRepository::lagre)

        // when
        val response = integrationTestFixture.get("/api/varsler/${varsel.id.value}")

        // then
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }
}
