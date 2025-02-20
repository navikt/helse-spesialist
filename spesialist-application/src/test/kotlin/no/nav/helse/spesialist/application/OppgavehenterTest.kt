package no.nav.helse.spesialist.application

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.mediator.oppgave.Oppgavehenter
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class OppgavehenterTest {

    private companion object {
        private const val OPPGAVE_ID = 1L
        private val TYPE = EgenskapForDatabase.SØKNAD
        private const val STATUS = "AvventerSaksbehandler"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val BEHANDLING_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
        private const val SAKSBEHANDLER_IDENT = "S199999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLER_NAVN = "Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val TILDELT_TIL = SaksbehandlerFraDatabase(
            epostadresse = SAKSBEHANDLER_EPOST,
            oid = SAKSBEHANDLER_OID,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT
        )
        private const val PÅ_VENT = false
        private const val KAN_AVVISES = true
    }

    @Test
    fun `konverter fra OppgaveFraDatabase til Oppgave`() {
        val oppgavehenter = Oppgavehenter(
            oppgaveDao = oppgaveRepository(),
            tilgangskontroll = { _, _ -> false }
        )
        val oppgave = oppgavehenter.oppgave(OPPGAVE_ID)
        assertEquals(OPPGAVE_ID, oppgave.id)
        assertEquals(Oppgave.AvventerSaksbehandler, oppgave.tilstand)
        assertEquals(VEDTAKSPERIODE_ID, oppgave.vedtaksperiodeId)
        assertEquals(UTBETALING_ID, oppgave.utbetalingId)
        assertEquals(SAKSBEHANDLER_OID, oppgave.ferdigstiltAvOid)
        assertEquals(SAKSBEHANDLER_IDENT, oppgave.ferdigstiltAvIdent)
        assertTrue(oppgave.egenskaper.contains(Egenskap.SØKNAD))
        assertEquals(PÅ_VENT, oppgave.egenskaper.contains(Egenskap.PÅ_VENT))
    }

    private fun oppgaveRepository(oppgaveegenskaper: List<EgenskapForDatabase> = listOf(TYPE)) = object : OppgaveDao {
        override fun finnOppgave(id: Long) = OppgaveFraDatabase(
            id = OPPGAVE_ID,
            egenskaper = oppgaveegenskaper,
            status = STATUS,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            behandlingId = BEHANDLING_ID,
            utbetalingId = UTBETALING_ID,
            godkjenningsbehovId = HENDELSE_ID,
            kanAvvises = KAN_AVVISES,
            ferdigstiltAvIdent = SAKSBEHANDLER_IDENT,
            ferdigstiltAvOid = SAKSBEHANDLER_OID,
            tildelt = TILDELT_TIL,
        )

        override fun finnOppgaveIdUansettStatus(fødselsnummer: String) = error("Not implemented in test")
        override fun finnGenerasjonId(oppgaveId: Long) = error("Not implemented in test")
        override fun oppgaveDataForAutomatisering(oppgaveId: Long) = error("Not implemented in test")
        override fun finnHendelseId(id: Long) = UUID.randomUUID()
        override fun finnOppgaveId(fødselsnummer: String) = error("Not implemented in test")
        override fun finnVedtaksperiodeId(fødselsnummer: String) = error("Not implemented in test")
        override fun finnVedtaksperiodeId(oppgaveId: Long) = error("Not implemented in test")
        override fun harGyldigOppgave(utbetalingId: UUID) = error("Not implemented in test")
        override fun invaliderOppgaveFor(fødselsnummer: String) = error("Not implemented in test")
        override fun finnOppgaveId(utbetalingId: UUID) = error("Not implemented in test")
        override fun reserverNesteId() = error("Not implemented in test")
        override fun venterPåSaksbehandler(oppgaveId: Long) = error("Not implemented in test")
        override fun finnSpleisBehandlingId(oppgaveId: Long) = error("Not implemented in test")
        override fun finnOppgaverForVisning(
            ekskluderEgenskaper: List<String>,
            saksbehandlerOid: UUID,
            offset: Int,
            limit: Int,
            sortering: List<OppgavesorteringForDatabase>,
            egneSakerPåVent: Boolean,
            egneSaker: Boolean,
            tildelt: Boolean?,
            grupperteFiltrerteEgenskaper: Map<Egenskap.Kategori, List<EgenskapForDatabase>>?
        ) = error("Not implemented in test")

        override fun finnAntallOppgaver(saksbehandlerOid: UUID) = error("Not implemented in test")
        override fun finnBehandledeOppgaver(behandletAvOid: UUID, offset: Int, limit: Int) =
            error("Not implemented in test")

        override fun finnEgenskaper(vedtaksperiodeId: UUID, utbetalingId: UUID) = error("Not implemented in test")
        override fun finnIdForAktivOppgave(vedtaksperiodeId: UUID) = error("Not implemented in test")
        override fun opprettOppgave(
            id: Long,
            godkjenningsbehovId: UUID,
            egenskaper: List<EgenskapForDatabase>,
            vedtaksperiodeId: UUID,
            behandlingId: UUID,
            utbetalingId: UUID,
            kanAvvises: Boolean
        ) = error("Not implemented in test")

        override fun finnFødselsnummer(oppgaveId: Long) = error("Not implemented in test")
        override fun updateOppgave(
            oppgaveId: Long,
            oppgavestatus: String,
            ferdigstiltAv: String?,
            oid: UUID?,
            egenskaper: List<EgenskapForDatabase>
        ) = error("Not implemented in test")

        override fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = error("Not implemented in test")
        override fun oppdaterPekerTilGodkjenningsbehov(godkjenningsbehovId: UUID, utbetalingId: UUID) =
            error("Not implemented in test")
    }
}
