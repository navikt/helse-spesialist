package no.nav.helse.spesialist.api.rest.totrinnsvurdering

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingRetur
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostSendIReturIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `gir NotFound hvis oppgaven ikke finnes`() {
        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/999999999/totrinnsvurdering/send-i-retur",
                body = """{"notatTekst":"Trenger ny vurdering"}""",
            )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "Oppgave ikke funnet",
              "code": "OPPGAVE_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

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
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-i-retur",
                body = """{"notatTekst":"Trenger ny vurdering"}""",
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
        val oppgave = lagOppgaveMedBehandling(person)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-i-retur",
                body = """{"notatTekst":"Trenger ny vurdering"}""",
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
    fun `gir conflict hvis aktiv totrinnsvurdering mangler for personen`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgaveMedBehandling(person)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-i-retur",
                body = """{"notatTekst":"Trenger ny vurdering"}""",
            )

        // Then:
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

    @Test
    fun `gir conflict hvis totrinnsvurderingen mangler opprinnelig saksbehandler`() {
        // Given: en totrinnsvurdering som aldri har blitt sendt til en beslutter (mangler saksbehandler).
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgaveMedBehandling(person)
        sessionContext.totrinnsvurderingRepository.lagre(Totrinnsvurdering.ny(person.id.value))

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-i-retur",
                body = """{"notatTekst":"Trenger ny vurdering"}""",
            )

        // Then:
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 409,
              "title": "Totrinnsvurdering mangler opprinnelig saksbehandler",
              "code": "TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir conflict hvis beslutter er samme saksbehandler som opprinnelig sendte oppgaven til beslutter`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgaveMedBehandling(person)
        val opprinneligSaksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val totrinnsvurdering = Totrinnsvurdering.ny(person.id.value)
        totrinnsvurdering.sendTilBeslutter(oppgave.id.value, opprinneligSaksbehandler.id)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When: samme saksbehandler som sendte oppgaven til beslutter, forsøker å sende den i retur.
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-i-retur",
                body = """{"notatTekst":"Trenger ny vurdering"}""",
                saksbehandler = opprinneligSaksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 409,
              "title": "Oppgaven krever totrinnsvurdering av annen saksbehandler",
              "code": "KREVER_TOTRINNSVURDERING_AV_ANNEN"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir conflict hvis oppgaven allerede er sendt i retur`() {
        // Given: en totrinnsvurdering som allerede er sendt i retur (tilstand er AVVENTER_SAKSBEHANDLER, ikke AVVENTER_BESLUTTER).
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgaveMedBehandling(person)
        val opprinneligSaksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val forrigeBeslutter = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val totrinnsvurdering = Totrinnsvurdering.ny(person.id.value)
        totrinnsvurdering.sendTilBeslutter(oppgave.id.value, opprinneligSaksbehandler.id)
        totrinnsvurdering.sendIRetur(oppgave.id.value, forrigeBeslutter.id)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-i-retur",
                body = """{"notatTekst":"Trenger ny vurdering"}""",
            )

        // Then:
        assertEquals(HttpStatusCode.Conflict.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 409,
              "title": "Oppgaven er allerede sendt i retur",
              "code": "OPPGAVE_ALLEREDE_SENDT_I_RETUR"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `sender oppgaven i retur, oppdaterer totrinnsvurderingen og oppretter historikkinnslag`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val oppgave = lagOppgaveMedBehandling(person)
        val opprinneligSaksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val totrinnsvurdering = Totrinnsvurdering.ny(person.id.value)
        totrinnsvurdering.sendTilBeslutter(oppgave.id.value, opprinneligSaksbehandler.id)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        val beslutter = lagSaksbehandler()

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-i-retur",
                body = """{"notatTekst":"Trenger ny vurdering"}""",
                saksbehandler = beslutter,
            )

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)
        assertTrue(response.bodyAsText.isEmpty())

        val oppdatertOppgave = sessionContext.oppgaveRepository.finn(oppgave.id)!!
        assertTrue(Egenskap.RETUR in oppdatertOppgave.egenskaper)
        assertFalse(Egenskap.BESLUTTER in oppdatertOppgave.egenskaper)

        val oppdatertTotrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)!!
        assertEquals(TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER, oppdatertTotrinnsvurdering.tilstand)
        assertEquals(opprinneligSaksbehandler.id, oppdatertTotrinnsvurdering.saksbehandler)
        assertEquals(beslutter.id, oppdatertTotrinnsvurdering.beslutter)

        val historikk = sessionContext.periodehistorikkDao.finnForOppgave(oppgave.id.value)
        assertEquals(1, historikk.size)
        val historikkinnslag = assertInstanceOf<TotrinnsvurderingRetur>(historikk.single())
        assertEquals("Trenger ny vurdering", historikkinnslag.notattekst)
        assertEquals(beslutter.ident, historikkinnslag.saksbehandler.ident)
    }

    private fun lagOppgaveMedBehandling(person: Person): Oppgave {
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        sessionContext.behandlingRepository.lagre(behandling)
        return lagOppgave(
            behandlingId = behandling.spleisBehandlingId!!,
            godkjenningsbehovId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiode.id,
        ).also(sessionContext.oppgaveRepository::lagre)
    }
}
