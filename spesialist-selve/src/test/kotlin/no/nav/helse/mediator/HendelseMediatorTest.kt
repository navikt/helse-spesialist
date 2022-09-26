package no.nav.helse.mediator

import AbstractE2ETest
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.SAKSBEHANDLER_OID
import no.nav.helse.mediator.api.AnnulleringDto
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.mediator.api.modell.Saksbehandler
import no.nav.helse.modell.oppgave.OppgaveMediator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class HendelseMediatorTest : AbstractE2ETest() {

    private val mediator = HendelseMediator(
        dataSource = dataSource,
        rapidsConnection = testRapid,
        opptegnelseDao = opptegnelseDao,
        oppgaveMediator = oppgaveMediator,
        hendelsefabrikk = hendelsefabrikk
    )

    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    private val mediatorWithMock = HendelseMediator(
        dataSource = dataSource,
        rapidsConnection = testRapid,
        opptegnelseDao = opptegnelseDao,
        oppgaveMediator = oppgaveMediatorMock,
        hendelsefabrikk = hendelsefabrikk
    )

    @Test
    fun `oppgave avventer system når saksbehandlerløsning legges på rapid`() {
        val oid = UUID.randomUUID()
        val epost = "epost@nav.no"
        val saksbehandlerIdent = "saksbehandler"
        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!
        mediator.håndter(GodkjenningDTO(oppgavereferanse, true, saksbehandlerIdent, null, null, null), epost, oid)
        assertTrue(testRapid.inspektør.hendelser("saksbehandler_løsning").isNotEmpty())
        assertEquals("AvventerSystem", testRapid.inspektør.hendelser("oppgave_oppdatert").last()["status"].asText())
    }

    @Test
    fun `oppgave_oppdater inneholder påVent-flagg i noen tilfeller`() {
        fun påVentNode() = testRapid.inspektør.hendelser("oppgave_oppdatert").last()["påVent"]

        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!

        mediator.sendMeldingOppgaveOppdatert(oppgavereferanse)
        assertNull(påVentNode())

        mediator.sendMeldingOppgaveOppdatert(oppgavereferanse, påVent = true)
        assertEquals("true", påVentNode().asText())

        mediator.sendMeldingOppgaveOppdatert(oppgavereferanse, påVent = false)
        assertEquals("false", påVentNode().asText())
    }

    @Test
    fun `publiserer annullering på rapid`() {
        val oid = UUID.randomUUID()
        val navn = "Siri Saksbehandler"
        val ident = "Z999999"
        mediator.håndter(
            annulleringDto = AnnulleringDto(
                aktørId = "X999999",
                fødselsnummer = "12345612345",
                organisasjonsnummer = "12",
                fagsystemId = "foo",
                saksbehandlerIdent = "X999999",
                begrunnelser = listOf("En", "Toten", "Tre"),
                kommentar = "Nittedal Tistedal Elverum",
                gjelderSisteSkjæringstidspunkt = true
            ), Saksbehandler("siri.saksbehandler@nav.no", oid, navn, ident)
        )
        assertEquals("annullering", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(
            "siri.saksbehandler@nav.no",
            testRapid.inspektør.field(0, "saksbehandler")["epostaddresse"].asText()
        )
        assertEquals("X999999", testRapid.inspektør.field(0, "saksbehandler")["ident"].asText())
        assertEquals(true, testRapid.inspektør.field(0, "gjelderSisteSkjæringstidspunkt").asBoolean())
        assertEquals(oid.toString(), testRapid.inspektør.field(0, "saksbehandler")["oid"].asText())
        assertEquals(navn, testRapid.inspektør.field(0, "saksbehandler")["navn"].asText())
        assertEquals("Nittedal Tistedal Elverum", testRapid.inspektør.field(0, "kommentar").asText())
        assertEquals("En", testRapid.inspektør.field(0, "begrunnelser")[0].asText())
    }

    @Test
    fun `publiserer annullering på rapid tomme verdier`() {
        val oid = UUID.randomUUID()
        val navn = "Siri Saksbehandler"
        val ident = "Z999999"
        mediator.håndter(
            annulleringDto = AnnulleringDto(
                aktørId = "X999999",
                fødselsnummer = "12345612345",
                organisasjonsnummer = "12",
                fagsystemId = "foo",
                saksbehandlerIdent = "X999999",
                begrunnelser = listOf(),
                kommentar = null,
                gjelderSisteSkjæringstidspunkt = true
            ), Saksbehandler("siri.saksbehandler@nav.no", oid, navn, ident)
        )
        assertEquals("annullering", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(
            "siri.saksbehandler@nav.no",
            testRapid.inspektør.field(0, "saksbehandler")["epostaddresse"].asText()
        )
        assertEquals("X999999", testRapid.inspektør.field(0, "saksbehandler")["ident"].asText())
        assertEquals(oid.toString(), testRapid.inspektør.field(0, "saksbehandler")["oid"].asText())
        assertEquals(navn, testRapid.inspektør.field(0, "saksbehandler")["navn"].asText())
        assertThrows<IllegalArgumentException> { testRapid.inspektør.field(0, "kommentar") }
        assertEquals(emptyList<String>(), testRapid.inspektør.field(0, "begrunnelser").map { it.asText() })
    }

    @Test
    fun `sjekker at saksbehandler ikke har lov å attestere beslutteroppgaven hvis saksbehandler sendte saken til godkjenning`() {
        val oppgaveId = 1L

        every { oppgaveMediatorMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(oppgaveId) } returns SAKSBEHANDLER_OID

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(oppgaveId, SAKSBEHANDLER_OID)

        assertTrue(kanIkkeAttestere)
    }

    @Test
    fun `sjekker at saksbehandler har lov å attestere beslutteroppgaven hvis saksbehandler ikke sendte saken til godkjenning`() {
        val oppgaveId = 1L

        every { oppgaveMediatorMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(oppgaveId) } returns UUID.randomUUID()

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(oppgaveId, SAKSBEHANDLER_OID)

        assertFalse(kanIkkeAttestere)
    }

    @Test
    fun `sjekker at saksbehandler har lov å attestere beslutteroppgaven hvis tidligere saksbehandler ikke finnes`() {
        val oppgaveId = 1L

        every { oppgaveMediatorMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(oppgaveId) } returns null

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(oppgaveId, SAKSBEHANDLER_OID)

        assertFalse(kanIkkeAttestere)
    }
}
