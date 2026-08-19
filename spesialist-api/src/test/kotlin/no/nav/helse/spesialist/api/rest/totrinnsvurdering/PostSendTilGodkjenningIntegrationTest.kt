package no.nav.helse.spesialist.api.rest.totrinnsvurdering

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class PostSendTilGodkjenningIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `gir Internal Server Error hvis behandlingen ikke finnes`() {
        // Given:
        val oppgave =
            lagOppgave(
                behandlingId = lagSpleisBehandlingId(),
                godkjenningsbehovId = UUID.randomUUID(),
            ).also(sessionContext.oppgaveRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-til-godkjenning",
                body = """{"begrunnelse":"Mangler behandling"}""",
            )

        // Then:
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
    fun `gir forbidden hvis saksbehandler ikke har tilgang til personen`() {
        // Given:
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(
                TilgangSomMangler.EgenAnsatt,
            )
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        sessionContext.behandlingRepository.lagre(behandling)
        val oppgave =
            lagOppgave(
                behandlingId = behandling.spleisBehandlingId!!,
                godkjenningsbehovId = UUID.randomUUID(),
            ).also(sessionContext.oppgaveRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-til-godkjenning",
                body = """{"begrunnelse":"En begrunnelse"}""",
            )

        // Then:
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
    fun `gir conflict hvis en tidligere, sammenhengende periode i samme sykefraværstilfelle har uvurderte varsler`() {
        // Given: to behandlinger i samme sykefraværstilfelle (samme skjæringstidspunkt) for samme person.
        val person = lagPerson().also(sessionContext.personRepository::lagre)

        val tidligereVedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        sessionContext.vedtaksperiodeRepository.lagre(tidligereVedtaksperiode)
        val tidligereBehandling =
            lagBehandling(
                vedtaksperiodeId = tidligereVedtaksperiode.id,
                fom = 1 jan 2021,
                tom = 15 jan 2021,
                skjæringstidspunkt = 1 jan 2021,
            )
        sessionContext.behandlingRepository.lagre(tidligereBehandling)
        sessionContext.varselRepository.lagre(
            lagVarsel(
                behandlingUnikId = tidligereBehandling.id,
                spleisBehandlingId = tidligereBehandling.spleisBehandlingId,
                status = Varsel.Status.AKTIV,
            ),
        )

        val gjeldendeVedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        sessionContext.vedtaksperiodeRepository.lagre(gjeldendeVedtaksperiode)
        val gjeldendeBehandling =
            lagBehandling(
                vedtaksperiodeId = gjeldendeVedtaksperiode.id,
                fom = 16 jan 2021,
                tom = 31 jan 2021,
                skjæringstidspunkt = 1 jan 2021,
            )
        sessionContext.behandlingRepository.lagre(gjeldendeBehandling)
        val oppgave =
            lagOppgave(
                behandlingId = gjeldendeBehandling.spleisBehandlingId!!,
                godkjenningsbehovId = UUID.randomUUID(),
            ).also(sessionContext.oppgaveRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-til-godkjenning",
                body = """{"begrunnelse":"En begrunnelse"}""",
            )

        // Then:
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 409,
              "title": "Det finnes aktive varsler som mangler vurdering",
              "code": "MANGLER_VURDERING_AV_VARSLER"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `sender til godkjenning selv om en urelatert periode med ulikt skjæringstidspunkt har uvurderte varsler`() {
        // Given: en urelatert periode (ulikt skjæringstidspunkt, altså et annet sykefraværstilfelle) med et aktivt varsel.
        val person = lagPerson().also(sessionContext.personRepository::lagre)

        val urelatertVedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        sessionContext.vedtaksperiodeRepository.lagre(urelatertVedtaksperiode)
        val urelatertBehandling =
            lagBehandling(
                vedtaksperiodeId = urelatertVedtaksperiode.id,
                fom = 1 jan 2021,
                tom = 15 jan 2021,
                skjæringstidspunkt = 1 jan 2021,
            )
        sessionContext.behandlingRepository.lagre(urelatertBehandling)
        sessionContext.varselRepository.lagre(
            lagVarsel(
                behandlingUnikId = urelatertBehandling.id,
                spleisBehandlingId = urelatertBehandling.spleisBehandlingId,
                status = Varsel.Status.AKTIV,
            ),
        )

        val gjeldendeVedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        sessionContext.vedtaksperiodeRepository.lagre(gjeldendeVedtaksperiode)
        val gjeldendeBehandling =
            lagBehandling(
                vedtaksperiodeId = gjeldendeVedtaksperiode.id,
                fom = (1 jan 2021).plusMonths(2),
                tom = (15 jan 2021).plusMonths(2),
                skjæringstidspunkt = (1 jan 2021).plusMonths(2),
            )
        sessionContext.behandlingRepository.lagre(gjeldendeBehandling)
        val oppgave =
            lagOppgave(
                behandlingId = gjeldendeBehandling.spleisBehandlingId!!,
                godkjenningsbehovId = UUID.randomUUID(),
            ).also(sessionContext.oppgaveRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-til-godkjenning",
                body = """{"begrunnelse":"En begrunnelse"}""",
            )

        // Then: dette feiler av en annen grunn (mangler aktiv totrinnsvurdering), ikke pga. varselsjekken.
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 409,
              "title": "Aktiv totrinnsvurdering mangler for oppgaven",
              "code": "TOTRINNSVURDERING_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }
}
