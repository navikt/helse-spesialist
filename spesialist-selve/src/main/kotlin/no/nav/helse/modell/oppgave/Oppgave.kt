package no.nav.helse.modell.oppgave

import java.util.Objects
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Gruppe
import no.nav.helse.Tilgangskontroll
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtBeslutter
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtIRetur
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveKreverTotrinnsvurdering
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import org.slf4j.LoggerFactory

class Oppgave private constructor(
    private val id: Long,
    private val type: Oppgavetype,
    private var status: Oppgavestatus,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val totrinnsvurdering: Totrinnsvurdering?
) {

    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null
    private val egenskaper = mutableListOf<Oppgavetype>()
    private var tildeltTil: Saksbehandler? = null
    private var påVent: Boolean = false

    constructor(
        id: Long,
        type: Oppgavetype,
        status: Oppgavestatus,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        ferdigstiltAvIdent: String? = null,
        ferdigstiltAvOid: UUID? = null,
        tildelt: Saksbehandler? = null,
        påVent: Boolean = false,
        totrinnsvurdering: Totrinnsvurdering? = null
    ) : this(id, type, status, vedtaksperiodeId, utbetalingId, totrinnsvurdering) {
        this.ferdigstiltAvIdent = ferdigstiltAvIdent
        this.ferdigstiltAvOid = ferdigstiltAvOid
        this.tildeltTil = tildelt
        this.påVent = påVent
    }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)

        fun oppgaveMedEgenskaper(
            id: Long,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            egenskaper: List<Oppgavetype>,
            totrinnsvurdering: Totrinnsvurdering? = null
        ): Oppgave {
            val hovedegenskap = egenskaper.firstOrNull() ?: Oppgavetype.SØKNAD
            return Oppgave(id, hovedegenskap, Oppgavestatus.AvventerSaksbehandler, vedtaksperiodeId, utbetalingId, totrinnsvurdering).also {
                it.egenskaper.addAll(egenskaper)
            }
        }

        fun lagMelding(
            oppgaveId: Long,
            eventName: String,
            påVent: Boolean? = null,
            oppgaveDao: OppgaveDao
        ): Pair<String, JsonMessage> {
            val hendelseId = oppgaveDao.finnHendelseId(oppgaveId)
            val oppgave = requireNotNull(oppgaveDao.finn(oppgaveId))
            val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)
            // @TODO bruke ny totrinnsvurderingtabell eller fjerne?
            val erBeslutterOppgave = false
            val erReturOppgave = false

            return fødselsnummer to lagMelding(
                eventName = eventName,
                hendelseId = hendelseId,
                oppgaveId = oppgaveId,
                status = oppgave.status,
                type = oppgave.type,
                fødselsnummer = fødselsnummer,
                erBeslutterOppgave = erBeslutterOppgave,
                erReturOppgave = erReturOppgave,
                ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
                ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
                påVent = påVent,
            )
        }

        fun lagMelding(
            fødselsnummer: String,
            hendelseId: UUID,
            eventName: String,
            oppgave: Oppgave,
            påVent: Boolean? = null
        ): Pair<String, JsonMessage> {
            // @TODO bruke ny totrinnsvurderingtabell eller fjerne?
            val erBeslutterOppgave = false
            val erReturOppgave = false

            return fødselsnummer to lagMelding(
                eventName = eventName,
                hendelseId = hendelseId,
                oppgaveId = oppgave.id,
                status = oppgave.status,
                type = oppgave.type,
                fødselsnummer = fødselsnummer,
                erBeslutterOppgave = erBeslutterOppgave,
                erReturOppgave = erReturOppgave,
                ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
                ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
                påVent = påVent,
            )
        }

        private fun lagMelding(
            eventName: String,
            hendelseId: UUID,
            oppgaveId: Long,
            status: Oppgavestatus,
            type: Oppgavetype,
            fødselsnummer: String,
            erBeslutterOppgave: Boolean,
            erReturOppgave: Boolean,
            ferdigstiltAvIdent: String? = null,
            ferdigstiltAvOid: UUID? = null,
            påVent: Boolean? = null,
        ): JsonMessage {
            return JsonMessage.newMessage(eventName, mutableMapOf(
                "@forårsaket_av" to mapOf(
                    "id" to hendelseId
                ),
                "hendelseId" to hendelseId,
                "oppgaveId" to oppgaveId,
                "status" to status.name,
                "type" to type.name,
                "fødselsnummer" to fødselsnummer,
                "erBeslutterOppgave" to erBeslutterOppgave,
                "erReturOppgave" to erReturOppgave,
            ).apply {
                ferdigstiltAvIdent?.also { put("ferdigstiltAvIdent", it) }
                ferdigstiltAvOid?.also { put("ferdigstiltAvOid", it) }
                påVent?.also { put("påVent", it) }
            })
        }
    }

    internal fun sendTilBeslutter(behandlendeSaksbehandler: Saksbehandler) {
        val totrinnsvurdering = requireNotNull(totrinnsvurdering) {
            "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
        }
        if (totrinnsvurdering.erBeslutteroppgave())
            throw OppgaveAlleredeSendtBeslutter(id)

        totrinnsvurdering.sendTilBeslutter(behandlendeSaksbehandler)

        if (totrinnsvurdering.tidligereBeslutter() == null) return
        if (behandlendeSaksbehandler == totrinnsvurdering.tidligereBeslutter())
            throw OppgaveKreverTotrinnsvurdering(id)

        tildeltTil = totrinnsvurdering.tidligereBeslutter()
    }

    internal fun sendIRetur(besluttendeSaksbehandler: Saksbehandler) {
        val totrinnsvurdering = requireNotNull(totrinnsvurdering) {
            "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
        }
        if (!totrinnsvurdering.erBeslutteroppgave())
            throw OppgaveAlleredeSendtIRetur(id)

        if (besluttendeSaksbehandler == totrinnsvurdering.opprinneligSaksbehandler())
            throw OppgaveKreverTotrinnsvurdering(id)

        totrinnsvurdering.sendIRetur(besluttendeSaksbehandler)

        val opprinneligSaksbehandler = requireNotNull(totrinnsvurdering.opprinneligSaksbehandler()) {
            "Opprinnelig saksbehandler kan ikke være null ved retur av beslutteroppgave"
        }

        tildeltTil = opprinneligSaksbehandler
    }

    fun accept(visitor: OppgaveVisitor) {
        visitor.visitOppgave(id, type, status, vedtaksperiodeId, utbetalingId, ferdigstiltAvOid, ferdigstiltAvIdent, egenskaper, tildeltTil, påVent, totrinnsvurdering)
        totrinnsvurdering?.accept(visitor)
    }

    fun ferdigstill(ident: String, oid: UUID) {
        status = Oppgavestatus.Ferdigstilt
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    fun ferdigstill() {
        status = Oppgavestatus.Ferdigstilt
    }

    fun avventerSystem(ident: String, oid: UUID) {
        status = Oppgavestatus.AvventerSystem
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    fun lagMelding(eventName: String, fødselsnummer: String, hendelseId: UUID): JsonMessage {
        return lagMelding(fødselsnummer, hendelseId, eventName, this, false).second
    }

    fun loggOppgaverAvbrutt(vedtaksperiodeId: UUID) {
        logg.info("Har avbrutt oppgave $id for vedtaksperiode $vedtaksperiodeId")
    }

    fun avbryt() {
        status = Oppgavestatus.Invalidert
    }

    fun forsøkTildeling(
        saksbehandler: Saksbehandler,
        påVent: Boolean = false,
        harTilgangTil: Tilgangskontroll,
    ) {
        if (type == Oppgavetype.STIKKPRØVE) {
            logg.info("OppgaveId $id er stikkprøve og tildeles ikke på tross av reservasjon.")
            return
        }
        if (type == Oppgavetype.RISK_QA) {
            val harTilgangTilRisk = runBlocking { harTilgangTil(saksbehandler.oid(), Gruppe.RISK_QA) }
            if (!harTilgangTilRisk) logg.info("OppgaveId $id er RISK_QA og saksbehandler har ikke tilgang, tildeles ikke på tross av reservasjon.")
            return
        }
        tildeltTil = saksbehandler
        this.påVent = påVent
        logg.info("Oppgave $id tildeles $saksbehandler grunnet reservasjon.")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Oppgave) return false
        if (this.id != other.id) return false
        return this.type == other.type && this.vedtaksperiodeId == other.vedtaksperiodeId
    }

    override fun hashCode(): Int {
        return Objects.hash(id, type, vedtaksperiodeId)
    }

    override fun toString(): String {
        return "Oppgave(type=$type, status=$status, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, id=$id)"
    }
}
