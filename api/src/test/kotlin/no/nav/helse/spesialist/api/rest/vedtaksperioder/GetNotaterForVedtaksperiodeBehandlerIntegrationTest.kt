package no.nav.helse.spesialist.api.rest.vedtaksperioder

import com.github.navikt.tbd_libs.jackson.asLocalDateTimeOrNull
import io.ktor.http.HttpStatusCode
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.testfixtures.lagNotat
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetNotaterForVedtaksperiodeBehandlerIntegrationTest {
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
        val kommentartekst = "Kommentar"
        val dialogId = Dialog.Factory.ny()
            .also { it.leggTilKommentar(kommentartekst, saksbehandler.ident) }
            .also(sessionContext.dialogRepository::lagre).id()
        val notat = lagNotat(
            tekst = "jeg er en notattekst",
            type = NotatType.Generelt,
            dialogRef = dialogId,
            vedtaksperiodeId = vedtaksperiodeId.value,
            saksbehandlerOid = saksbehandler.id
        ).also(sessionContext.notatRepository::lagre)

        // when
        val response =
            integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiodeId.value}/notater")

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body) { "Body er tom eller ugyldig JSON" }
        assertEquals(notat.tekst, body.first()["tekst"].asText())
        assertEquals(notat.type.name, body.first()["type"].asText())
        assertEquals(notat.dialogRef.value, body.first()["dialogRef"].asLong())
        assertEquals(notat.vedtaksperiodeId, body.first()["vedtaksperiodeId"].asUUID())
        assertEquals(notat.saksbehandlerOid.value, body.first()["saksbehandlerOid"].asUUID())
        assertEquals(saksbehandler.navn, body.first()["saksbehandlerNavn"].asText())
        assertEquals(saksbehandler.ident.value, body.first()["saksbehandlerIdent"].asText())
        assertEquals(saksbehandler.epost, body.first()["saksbehandlerEpost"].asText())
        assertEquals(notat.feilregistrert, body.first()["feilregistrert"].asBoolean())
        assertEquals(notat.feilregistrertTidspunkt, body.first()["feilregistrert_tidspunkt"].asLocalDateTimeOrNull())
        assertEquals(notat.type.name, body.first()["type"].asText())
        val kommentarer = body.first()["kommentarer"].map { it["tekst"].asText() to it["saksbehandlerident"].asText() }
        assertEquals(1, kommentarer.size)
        assertEquals(kommentartekst, kommentarer.first().first)
        assertEquals(saksbehandler.ident.value, kommentarer.first().second)
    }
}