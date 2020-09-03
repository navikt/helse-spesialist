package no.nav.helse.modell.vedtak

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.rapids_rivers.JsonMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SaksbehandlerLøsningTest {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private const val OPPGAVETYPE = "EN_OPPGAVE"
        private const val GODKJENT = true
        private const val IDENT = "Z999999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private val objectMapper = jacksonObjectMapper()
    }

    private val dao = mockk<OppgaveDao>(relaxed = true)

    @Test
    fun `ferdigstiller oppgave`() {
        val godkjenningløsning = JsonMessage.newMessage()
        val saksbehandlerLøsning = SaksbehandlerLøsning(GODKJENT, IDENT, SAKSBEHANDLER_OID, SAKSBEHANDLER_EPOST, GODKJENTTIDSPUNKT, null, null, null)
        saksbehandlerLøsning.ferdigstillOppgave(dao, HENDELSE_ID, OPPGAVETYPE, godkjenningløsning)
        verify(exactly = 1) { dao.updateOppgave(HENDELSE_ID, OPPGAVETYPE, Oppgavestatus.Ferdigstilt, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID) }
        val json = objectMapper.readTree(godkjenningløsning.toJson()).path("@løsning").path("Godkjenning")
        assertEquals(GODKJENT, json.path("godkjent").booleanValue())
        assertEquals(IDENT, json.path("saksbehandlerIdent").textValue())
        assertEquals(GODKJENTTIDSPUNKT, LocalDateTime.parse(json.path("godkjenttidspunkt").textValue()))
        assertTrue(json.path("årsak").isNull)
        assertTrue(json.path("begrunnelser").isNull)
        assertTrue(json.path("kommentar").isNull)
    }
}
