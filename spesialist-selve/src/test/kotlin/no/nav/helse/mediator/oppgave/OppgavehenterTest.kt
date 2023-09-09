package no.nav.helse.mediator.oppgave

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates

class OppgavehenterTest {

    private companion object {
        private const val OPPGAVE_ID = 1L
        private const val TYPE = "SØKNAD"
        private const val STATUS = "AvventerSaksbehandler"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private const val SAKSBEHANDLER_IDENT = "S199999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLER_NAVN = "Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val BESLUTTER_OID = UUID.randomUUID()
        private val TILDELT_TIL = SaksbehandlerFraDatabase(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT)
        private const val PÅ_VENT = false
        private const val ER_RETUR = false
        private val TOTRINNSVURDERING_OPPRETTET = LocalDateTime.now()
        private val TOTRINNSVURDERING_OPPDATERT = LocalDateTime.now()
    }

    @Test
    fun `konverter fra OppgaveFraDatabase til Oppgave`() {
        val oppgavehenter = Oppgavehenter(oppgaveRepository, totrinnsvurderingRepository(), saksbehandlerRepository)
        val oppgave = oppgavehenter.oppgave(OPPGAVE_ID)
        oppgave.accept(inspektør)
        inspektør.assertOppgave(
            id = OPPGAVE_ID,
            type = Oppgavetype.SØKNAD,
            tilstand = Oppgave.AvventerSaksbehandler,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            ferdigstiltAvOid = SAKSBEHANDLER_OID,
            ferdigstiltAvIdent = SAKSBEHANDLER_IDENT,
            egenskaper = emptyList(),
            tildelt = Saksbehandler(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT),
            påVent = PÅ_VENT,
            null
        )
    }

    @Test
    fun `konverter fra OppgaveFraDatabase til Oppgave med totrinnsvurdering`() {
        val totrinnsvurdering = TotrinnsvurderingFraDatabase(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = ER_RETUR,
            saksbehandler = SAKSBEHANDLER_OID,
            beslutter = BESLUTTER_OID,
            utbetalingId = UTBETALING_ID,
            opprettet = LocalDateTime.now(),
            oppdatert = LocalDateTime.now()
        )

        val oppgavehenter = Oppgavehenter(oppgaveRepository, totrinnsvurderingRepository(totrinnsvurdering), saksbehandlerRepository)
        val oppgave = oppgavehenter.oppgave(OPPGAVE_ID)
        oppgave.accept(inspektør)
        inspektør.assertOppgave(
            id = OPPGAVE_ID,
            type = Oppgavetype.SØKNAD,
            tilstand = Oppgave.AvventerSaksbehandler,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            ferdigstiltAvOid = SAKSBEHANDLER_OID,
            ferdigstiltAvIdent = SAKSBEHANDLER_IDENT,
            egenskaper = emptyList(),
            tildelt = Saksbehandler(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT),
            påVent = PÅ_VENT,
            totrinnsvurdering = Totrinnsvurdering(
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                erRetur = ER_RETUR,
                saksbehandler = Saksbehandler(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT),
                beslutter = Saksbehandler(SAKSBEHANDLER_EPOST, BESLUTTER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT),
                utbetalingId = UTBETALING_ID,
                opprettet = TOTRINNSVURDERING_OPPRETTET,
                oppdatert = TOTRINNSVURDERING_OPPDATERT
            )
        )
    }

    private val inspektør = object : OppgaveVisitor {
        private var id by Delegates.notNull<Long>()
        private lateinit var type: Oppgavetype
        private lateinit var tilstand: Oppgave.Tilstand
        private lateinit var vedtaksperiodeId: UUID
        private lateinit var utbetalingId: UUID
        private var ferdigstiltAvOid: UUID? = null
        private var ferdigstiltAvIdent: String? = null
        private lateinit var egenskaper: List<Oppgavetype>
        private var tildelt: Saksbehandler? = null
        private var påVent by Delegates.notNull<Boolean>()
        private var totrinnsvurdering: Totrinnsvurdering? = null

        override fun visitOppgave(
            id: Long,
            type: Oppgavetype,
            tilstand: Oppgave.Tilstand,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            ferdigstiltAvOid: UUID?,
            ferdigstiltAvIdent: String?,
            egenskaper: List<Oppgavetype>,
            tildelt: Saksbehandler?,
            påVent: Boolean,
            totrinnsvurdering: Totrinnsvurdering?
        ) {
            this.id = id
            this.type = type
            this.tilstand = tilstand
            this.vedtaksperiodeId = vedtaksperiodeId
            this.utbetalingId = utbetalingId
            this.ferdigstiltAvOid = ferdigstiltAvOid
            this.ferdigstiltAvIdent = ferdigstiltAvIdent
            this.egenskaper = egenskaper
            this.tildelt = tildelt
            this.påVent = påVent
            this.totrinnsvurdering = totrinnsvurdering
        }

        fun assertOppgave(
            id: Long,
            type: Oppgavetype,
            tilstand: Oppgave.Tilstand,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            ferdigstiltAvOid: UUID?,
            ferdigstiltAvIdent: String?,
            egenskaper: List<Oppgavetype>,
            tildelt: Saksbehandler?,
            påVent: Boolean,
            totrinnsvurdering: Totrinnsvurdering?
        ) {
            assertEquals(id, this.id)
            assertEquals(type, this.type)
            assertEquals(tilstand, this.tilstand)
            assertEquals(vedtaksperiodeId, this.vedtaksperiodeId)
            assertEquals(utbetalingId, this.utbetalingId)
            assertEquals(ferdigstiltAvOid, this.ferdigstiltAvOid)
            assertEquals(ferdigstiltAvIdent, this.ferdigstiltAvIdent)
            assertEquals(egenskaper, this.egenskaper)
            assertEquals(tildelt, this.tildelt)
            assertEquals(påVent, this.påVent)
            assertEquals(totrinnsvurdering, this.totrinnsvurdering)
        }
    }

    private val oppgaveRepository = object : OppgaveRepository {
        override fun finnOppgave(id: Long): OppgaveFraDatabase {
            return OppgaveFraDatabase(
                id = OPPGAVE_ID,
                type = TYPE,
                status = STATUS,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
                ferdigstiltAvIdent = SAKSBEHANDLER_IDENT,
                ferdigstiltAvOid = SAKSBEHANDLER_OID,
                tildelt = TILDELT_TIL,
                påVent = PÅ_VENT
            )
        }

        override fun finnHendelseId(id: Long): UUID = UUID.randomUUID()
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
}