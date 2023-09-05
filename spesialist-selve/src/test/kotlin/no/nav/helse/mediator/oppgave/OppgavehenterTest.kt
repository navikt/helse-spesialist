package no.nav.helse.mediator.oppgave

import java.util.UUID
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
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
        private const val FERDIGSTILT_AV_IDENT = "S199999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLER_NAVN = "Saksbehandler"
        private val FERDIGSTILT_AV_OID = UUID.randomUUID()
        private val TILDELT_TIL = SaksbehandlerFraDatabase(SAKSBEHANDLER_EPOST, FERDIGSTILT_AV_OID, SAKSBEHANDLER_NAVN, FERDIGSTILT_AV_IDENT)
        private const val PÅ_VENT = false
    }

    @Test
    fun `konverter fra OppgaveFraDatabase til Oppgave`() {
        val oppgavehenter = Oppgavehenter(repository)
        val oppgave = oppgavehenter.oppgave(OPPGAVE_ID)
        oppgave.accept(inspektør)
        inspektør.assertOppgave(
            id = OPPGAVE_ID,
            type = Oppgavetype.SØKNAD,
            status = Oppgavestatus.AvventerSaksbehandler,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID,
            ferdigstiltAvOid = FERDIGSTILT_AV_OID,
            ferdigstiltAvIdent = FERDIGSTILT_AV_IDENT,
            egenskaper = emptyList(),
            tildelt = Saksbehandler(SAKSBEHANDLER_EPOST, FERDIGSTILT_AV_OID, SAKSBEHANDLER_NAVN, FERDIGSTILT_AV_IDENT),
            påVent = PÅ_VENT
        )
    }

    private val inspektør = object : OppgaveVisitor {
        private var id by Delegates.notNull<Long>()
        private lateinit var type: Oppgavetype
        private lateinit var status: Oppgavestatus
        private lateinit var vedtaksperiodeId: UUID
        private lateinit var utbetalingId: UUID
        private var ferdigstiltAvOid: UUID? = null
        private var ferdigstiltAvIdent: String? = null
        private lateinit var egenskaper: List<Oppgavetype>
        private var tildelt: Saksbehandler? = null
        private var påVent by Delegates.notNull<Boolean>()

        override fun visitOppgave(
            id: Long,
            type: Oppgavetype,
            status: Oppgavestatus,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            ferdigstiltAvOid: UUID?,
            ferdigstiltAvIdent: String?,
            egenskaper: List<Oppgavetype>,
            tildelt: Saksbehandler?,
            påVent: Boolean
        ) {
            this.id = id
            this.type = type
            this.status = status
            this.vedtaksperiodeId = vedtaksperiodeId
            this.utbetalingId = utbetalingId
            this.ferdigstiltAvOid = ferdigstiltAvOid
            this.ferdigstiltAvIdent = ferdigstiltAvIdent
            this.egenskaper = egenskaper
            this.tildelt = tildelt
            this.påVent = påVent
        }

        fun assertOppgave(
            id: Long,
            type: Oppgavetype,
            status: Oppgavestatus,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            ferdigstiltAvOid: UUID?,
            ferdigstiltAvIdent: String?,
            egenskaper: List<Oppgavetype>,
            tildelt: Saksbehandler?,
            påVent: Boolean
        ) {
            assertEquals(id, this.id)
            assertEquals(type, this.type)
            assertEquals(status, this.status)
            assertEquals(vedtaksperiodeId, this.vedtaksperiodeId)
            assertEquals(utbetalingId, this.utbetalingId)
            assertEquals(ferdigstiltAvOid, this.ferdigstiltAvOid)
            assertEquals(ferdigstiltAvIdent, this.ferdigstiltAvIdent)
            assertEquals(egenskaper, this.egenskaper)
            assertEquals(tildelt, this.tildelt)
            assertEquals(påVent, this.påVent)
        }
    }

    private val repository = object : OppgaveRepository {
        override fun finnOppgave(id: Long): OppgaveFraDatabase {
            return OppgaveFraDatabase(
                id = OPPGAVE_ID,
                type = TYPE,
                status = STATUS,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
                ferdigstiltAvIdent = FERDIGSTILT_AV_IDENT,
                ferdigstiltAvOid = FERDIGSTILT_AV_OID,
                tildelt = TILDELT_TIL,
                påVent = PÅ_VENT
            )
        }
    }
}