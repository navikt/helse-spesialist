package no.nav.helse.spesialist.api.rest.totrinnsvurdering

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.modell.periodehistorikk.AvventerTotrinnsvurdering
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Varselvurdering
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @ParameterizedTest
    @CsvSource("Innvilget,INNVILGELSE", "DelvisInnvilget,DELVIS_INNVILGELSE", "Avslag,AVSLAG")
    fun `sender til godkjenning, oppdaterer totrinnsvurdering og lagrer vedtaksbegrunnelse med utfall basert på tags`(
        tag: String,
        forventetUtfall: VedtakBegrunnelseTypeFraDatabase,
    ) {
        // Given: en behandling med et vurdert varsel (skal ikke blokkere innsending).
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        val behandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                tags = setOf(tag),
            )
        sessionContext.behandlingRepository.lagre(behandling)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val varseldefinisjon = lagVarseldefinisjon()
        sessionContext.varseldefinisjonRepository.lagre(varseldefinisjon)
        sessionContext.varselRepository.lagre(
            lagVarsel(
                behandlingUnikId = behandling.id,
                spleisBehandlingId = behandling.spleisBehandlingId,
                status = Varsel.Status.VURDERT,
                kode = varseldefinisjon.kode,
                vurdering = Varselvurdering(saksbehandler.id, LocalDateTime.now(), varseldefinisjon.id),
            ),
        )
        val oppgave =
            lagOppgave(
                behandlingId = behandling.spleisBehandlingId!!,
                godkjenningsbehovId = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiode.id,
            ).also(sessionContext.oppgaveRepository::lagre)
        val totrinnsvurdering = Totrinnsvurdering.ny(person.id.value)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-til-godkjenning",
                body = """{"begrunnelse":"En begrunnelse"}""",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)

        val oppdatertOppgave = sessionContext.oppgaveRepository.finn(oppgave.id)!!
        assertTrue(Egenskap.BESLUTTER in oppdatertOppgave.egenskaper)

        val oppdatertTotrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)!!
        assertEquals(TotrinnsvurderingTilstand.AVVENTER_BESLUTTER, oppdatertTotrinnsvurdering.tilstand)
        assertEquals(saksbehandler.id, oppdatertTotrinnsvurdering.saksbehandler)

        val vedtakBegrunnelse = sessionContext.vedtakBegrunnelseDao.finnVedtakBegrunnelse(oppgaveId = oppgave.id.value)
        assertEquals(forventetUtfall, vedtakBegrunnelse?.type)
        assertEquals("En begrunnelse", vedtakBegrunnelse?.tekst)

        val historikk = sessionContext.periodehistorikkDao.finnForOppgave(oppgave.id.value)
        assertEquals(1, historikk.size)
        assertInstanceOf<AvventerTotrinnsvurdering>(historikk.single())
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
