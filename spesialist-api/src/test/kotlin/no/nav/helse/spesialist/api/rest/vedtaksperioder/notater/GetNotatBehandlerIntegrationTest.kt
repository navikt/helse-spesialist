package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.ktor.http.HttpStatusCode
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.lagNotat
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetNotatBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `returnerer et notat i happy case`() {
        // given
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val identitetsnummer = lagPerson().also(sessionContext.personRepository::lagre).id
        val vedtaksperiodeId =
            lagVedtaksperiode(identitetsnummer = identitetsnummer)
                .also(sessionContext.vedtaksperiodeRepository::lagre).id
        val dialogId = Dialog.Factory.ny().also(sessionContext.dialogRepository::lagre).id()
        val notat = lagNotat(
            tekst = "jeg er en notattekst",
            type = NotatType.Generelt,
            dialogRef = dialogId,
            vedtaksperiodeId = vedtaksperiodeId.value,
            saksbehandlerOid = saksbehandler.id
        ).also(sessionContext.notatRepository::lagre)

        // when
        val response =
            integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiodeId.value}/notater/${notat.id().value}")

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body) { "Body er tom eller ugyldig JSON" }
        assertEquals(notat.tekst, body["tekst"].asText())
        assertEquals(notat.type.name, body["type"].asText())
        assertEquals(notat.dialogRef.value, body["dialogRef"].asLong())
        assertEquals(notat.vedtaksperiodeId, body["vedtaksperiodeId"].asUUID())
        assertEquals(notat.saksbehandlerOid.value, body["saksbehandlerOid"].asUUID())
    }

    @Test
    fun `gir 404 dersom varsel ikke finnes`() {
        val response = integrationTestFixture.get("/api/vedtaksperioder/${UUID.randomUUID()}/notater/99887766")
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "Fant ikke notat",
              "code": "NOTAT_IKKE_FUNNET" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }
    @Test
    fun `gir 403 dersom saksbehandler ikke har tilgang til den aktuelle personen`() {
        // given
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val identitetsnummer =
            lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Fortrolig)
                .also(sessionContext.personRepository::lagre).id
        val vedtaksperiodeId =
            lagVedtaksperiode(identitetsnummer = identitetsnummer)
                .also(sessionContext.vedtaksperiodeRepository::lagre).id
        val dialogId = Dialog.Factory.ny().also(sessionContext.dialogRepository::lagre).id()
        val notat = lagNotat(
            tekst = "jeg er en notattekst",
            type = NotatType.Generelt,
            dialogRef = dialogId,
            vedtaksperiodeId = vedtaksperiodeId.value,
            saksbehandlerOid = saksbehandler.id
        ).also(sessionContext.notatRepository::lagre)

        // when
        val response =
            integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiodeId.value}/notater/${notat.id().value}")

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
}
