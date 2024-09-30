package no.nav.helse.mediator.oppgave

import TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.modell.oppgave.EgenskapDto
import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.oppgave.OppgaveDto
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.toDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import javax.naming.OperationNotSupportedException

class OppgavehenterTest {

    private companion object {
        private const val OPPGAVE_ID = 1L
        private val TYPE = EgenskapForDatabase.SØKNAD
        private const val STATUS = "AvventerSaksbehandler"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
        private const val SAKSBEHANDLER_IDENT = "S199999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLER_NAVN = "Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val BESLUTTER_OID = UUID.randomUUID()
        private val TILDELT_TIL = SaksbehandlerFraDatabase(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT)
        private const val PÅ_VENT = false
        private const val ER_RETUR = false
        private const val KAN_AVVISES = true
        private val TOTRINNSVURDERING_OPPRETTET = LocalDateTime.now()
        private val TOTRINNSVURDERING_OPPDATERT = LocalDateTime.now()
    }

    @Test
    fun `konverter fra OppgaveFraDatabase til Oppgave`() {
        val oppgavehenter = Oppgavehenter(oppgaveRepository(), totrinnsvurderingRepository(), saksbehandlerRepository, TilgangskontrollForTestHarIkkeTilgang)
        val oppgave = oppgavehenter.oppgave(OPPGAVE_ID).toDto()
        assertEquals(OPPGAVE_ID, oppgave.id)
        assertEquals(OppgaveDto.TilstandDto.AvventerSaksbehandler, oppgave.tilstand)
        assertEquals(VEDTAKSPERIODE_ID, oppgave.vedtaksperiodeId)
        assertEquals(UTBETALING_ID, oppgave.utbetalingId)
        assertEquals(SAKSBEHANDLER_OID, oppgave.ferdigstiltAvOid)
        assertEquals(SAKSBEHANDLER_IDENT, oppgave.ferdigstiltAvIdent)
        assertEquals(null, oppgave.totrinnsvurdering)
        assertTrue(oppgave.egenskaper.contains(EgenskapDto.SØKNAD))
        assertEquals(PÅ_VENT, oppgave.egenskaper.contains(EgenskapDto.PÅ_VENT))
    }

    @Test
    fun `konverter fra OppgaveFraDatabase til Oppgave med totrinnsvurdering`() {
        val totrinnsvurdering = TotrinnsvurderingFraDatabase(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = ER_RETUR,
            saksbehandler = SAKSBEHANDLER_OID,
            beslutter = BESLUTTER_OID,
            utbetalingId = UTBETALING_ID,
            opprettet = TOTRINNSVURDERING_OPPRETTET,
            oppdatert = TOTRINNSVURDERING_OPPDATERT,
        )

        val oppgavehenter = Oppgavehenter(oppgaveRepository(), totrinnsvurderingRepository(totrinnsvurdering), saksbehandlerRepository, TilgangskontrollForTestHarIkkeTilgang)
        val oppgave = oppgavehenter.oppgave(OPPGAVE_ID).toDto()
        assertEquals(OPPGAVE_ID, oppgave.id)
        assertEquals(OppgaveDto.TilstandDto.AvventerSaksbehandler, oppgave.tilstand)
        assertEquals(VEDTAKSPERIODE_ID, oppgave.vedtaksperiodeId)
        assertEquals(UTBETALING_ID, oppgave.utbetalingId)
        assertEquals(SAKSBEHANDLER_OID, oppgave.ferdigstiltAvOid)
        assertEquals(SAKSBEHANDLER_IDENT, oppgave.ferdigstiltAvIdent)
        assertTrue(oppgave.egenskaper.contains(EgenskapDto.SØKNAD))
        assertEquals(PÅ_VENT, oppgave.egenskaper.contains(EgenskapDto.PÅ_VENT))

        assertEquals(VEDTAKSPERIODE_ID, oppgave.totrinnsvurdering?.vedtaksperiodeId)
        assertEquals(ER_RETUR, oppgave.totrinnsvurdering?.erRetur)
        assertEquals(saksbehandler().toDto(), oppgave.totrinnsvurdering?.saksbehandler)
        assertEquals(saksbehandler(oid = BESLUTTER_OID).toDto(), oppgave.totrinnsvurdering?.beslutter)
        assertEquals(UTBETALING_ID, oppgave.totrinnsvurdering?.utbetalingId)
        assertEquals(TOTRINNSVURDERING_OPPRETTET, oppgave.totrinnsvurdering?.opprettet)
        assertEquals(TOTRINNSVURDERING_OPPDATERT, oppgave.totrinnsvurdering?.oppdatert)
    }

    private fun oppgaveRepository(oppgaveegenskaper: List<EgenskapForDatabase> = listOf(TYPE)) = object : OppgaveRepository {
        override fun finnOppgave(id: Long): OppgaveFraDatabase {
            return OppgaveFraDatabase(
                id = OPPGAVE_ID,
                egenskaper = oppgaveegenskaper,
                status = STATUS,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
                hendelseId = HENDELSE_ID,
                kanAvvises = KAN_AVVISES,
                ferdigstiltAvIdent = SAKSBEHANDLER_IDENT,
                ferdigstiltAvOid = SAKSBEHANDLER_OID,
                tildelt = TILDELT_TIL,
            )
        }

        override fun finnHendelseId(id: Long): UUID = UUID.randomUUID()

        override fun finnOppgaveId(fødselsnummer: String): Long = throw OperationNotSupportedException()

        override fun finnVedtaksperiodeId(fødselsnummer: String): UUID = throw OperationNotSupportedException()

        override fun harGyldigOppgave(utbetalingId: UUID): Boolean = throw OperationNotSupportedException()

        override fun invaliderOppgaveFor(fødselsnummer: String) {
            throw OperationNotSupportedException()
        }
    }

    private fun totrinnsvurderingRepository(
        totrinnsvurdering: TotrinnsvurderingFraDatabase? = null
    ) = object : TotrinnsvurderingRepository {
        override fun hentAktivTotrinnsvurdering(oppgaveId: Long): TotrinnsvurderingFraDatabase? = totrinnsvurdering
        override fun oppdater(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase) {}
    }

    private val saksbehandlerRepository = object : SaksbehandlerRepository {
        val saksbehandlere = mapOf(
            SAKSBEHANDLER_OID to SaksbehandlerFraDatabase(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT),
            BESLUTTER_OID to SaksbehandlerFraDatabase(SAKSBEHANDLER_EPOST, BESLUTTER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT),
        )
        override fun finnSaksbehandler(oid: UUID): SaksbehandlerFraDatabase? {
            return saksbehandlere[oid]
        }
    }

    private fun saksbehandler(
        epost: String = SAKSBEHANDLER_EPOST,
        oid: UUID = SAKSBEHANDLER_OID,
        navn: String = SAKSBEHANDLER_NAVN,
        ident: String = SAKSBEHANDLER_IDENT,
    ) = Saksbehandler(
        epostadresse = epost,
        oid = oid,
        navn = navn,
        ident = ident,
        tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
    )
}
