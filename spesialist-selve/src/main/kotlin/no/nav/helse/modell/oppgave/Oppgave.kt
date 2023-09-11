package no.nav.helse.modell.oppgave

import java.util.Objects
import java.util.UUID
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Gruppe
import no.nav.helse.Tilgangskontroll
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import org.slf4j.LoggerFactory

class Oppgave private constructor(
    private val id: Long,
    private val type: Oppgavetype,
    private var tilstand: Tilstand,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val hendelseId: UUID,
    private val totrinnsvurdering: Totrinnsvurdering?
) {

    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null
    private val egenskaper = mutableListOf<Oppgavetype>()
    private var tildeltTil: Saksbehandler? = null
    private var påVent: Boolean = false

    private val observers = mutableListOf<OppgaveObserver>()

    internal constructor(
        id: Long,
        type: Oppgavetype,
        tilstand: Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvIdent: String? = null,
        ferdigstiltAvOid: UUID? = null,
        tildelt: Saksbehandler? = null,
        påVent: Boolean = false,
        totrinnsvurdering: Totrinnsvurdering? = null
    ) : this(id, type, tilstand, vedtaksperiodeId, utbetalingId, hendelseId, totrinnsvurdering) {
        this.ferdigstiltAvIdent = ferdigstiltAvIdent
        this.ferdigstiltAvOid = ferdigstiltAvOid
        this.tildeltTil = tildelt
        this.påVent = påVent
    }

    fun accept(visitor: OppgaveVisitor) {
        visitor.visitOppgave(id, type, tilstand, vedtaksperiodeId, utbetalingId, hendelseId, ferdigstiltAvOid, ferdigstiltAvIdent, egenskaper, tildeltTil, påVent, totrinnsvurdering)
        totrinnsvurdering?.accept(visitor)
    }

    fun register(observer: OppgaveObserver) {
        observers.add(observer)
    }

    internal fun forsøkTildeling(
        saksbehandler: Saksbehandler,
        påVent: Boolean = false,
        harTilgangTil: Tilgangskontroll,
    ) {
        check(tilstand is AvventerSaksbehandler) { "Oppgave med id=$id i tilstand=$tilstand kan ikke tildeles" }
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

    internal fun sendTilBeslutter(behandlendeSaksbehandler: Saksbehandler) {
        val totrinnsvurdering = requireNotNull(totrinnsvurdering) {
            "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
        }

        totrinnsvurdering.sendTilBeslutter(id, behandlendeSaksbehandler)

        tildeltTil = totrinnsvurdering.tidligereBeslutter()
    }

    internal fun sendIRetur(besluttendeSaksbehandler: Saksbehandler) {
        val totrinnsvurdering = requireNotNull(totrinnsvurdering) {
            "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
        }

        totrinnsvurdering.sendIRetur(id, besluttendeSaksbehandler)

        val opprinneligSaksbehandler = requireNotNull(totrinnsvurdering.opprinneligSaksbehandler()) {
            "Opprinnelig saksbehandler kan ikke være null ved retur av beslutteroppgave"
        }

        tildeltTil = opprinneligSaksbehandler
    }

    fun ferdigstill() {
        tilstand.ferdigstill(this)
    }

    fun avventerSystem(ident: String, oid: UUID) {
        tilstand.avventerSystem(this, ident, oid)
    }

    fun lagMelding(eventName: String, fødselsnummer: String, hendelseId: UUID): JsonMessage {
        return lagMelding(fødselsnummer, hendelseId, eventName, this, false).second
    }

    fun avbryt() {
        tilstand.invalider(this)
    }

    private fun nesteTilstand(neste: Tilstand) {
        val forrige = tilstand
        tilstand = neste
        logg.info(
            "Oppgave med {} bytter tilstand fra {} til {}",
            kv("oppgaveId", id),
            kv("forrigeTilstand", forrige),
            kv("nesteTilstand", neste),
        )
        observers.forEach { it.tilstandEndret(forrige, tilstand, this) }
    }

    sealed interface Tilstand {
        fun invalider(oppgave: Oppgave) {
            logg.warn(
                "Forventer ikke invalidering i {} for oppgave med {}",
                kv("tilstand", this),
                kv("oppgaveId", oppgave.id)
            )
        }
        fun avventerSystem(oppgave: Oppgave, ident: String, oid: UUID) {
            logg.warn(
                "Forventer ikke avventer system i {} for oppgave med {}",
                kv("tilstand", this),
                kv("oppgaveId", oppgave.id)
            )
        }
        fun ferdigstill(oppgave: Oppgave) {
            logg.warn(
                "Forventer ikke ferdigstillelse i {} for oppgave med {}",
                kv("tilstand", this),
                kv("oppgaveId", oppgave.id)
            )
        }
    }

    data object AvventerSaksbehandler: Tilstand {
        override fun avventerSystem(oppgave: Oppgave, ident: String, oid: UUID) {
            oppgave.ferdigstiltAvIdent = ident
            oppgave.ferdigstiltAvOid = oid
            oppgave.totrinnsvurdering?.ferdigstill(oppgave.utbetalingId)
            oppgave.nesteTilstand(AvventerSystem)
        }

        override fun invalider(oppgave: Oppgave) {
            oppgave.nesteTilstand(Invalidert)
        }
    }

    data object AvventerSystem: Tilstand {
        override fun ferdigstill(oppgave: Oppgave) {
            oppgave.nesteTilstand(Ferdigstilt)
        }

        override fun invalider(oppgave: Oppgave) {
            oppgave.nesteTilstand(Invalidert)
        }
    }

    data object Ferdigstilt: Tilstand

    data object Invalidert: Tilstand

    override fun equals(other: Any?): Boolean {
        if (other !is Oppgave) return false
        if (this.id != other.id) return false
        return this.type == other.type && this.vedtaksperiodeId == other.vedtaksperiodeId
    }

    override fun hashCode(): Int {
        return Objects.hash(id, type, vedtaksperiodeId)
    }

    override fun toString(): String {
        return "Oppgave(type=$type, tilstand=$tilstand, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, id=$id)"
    }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)

        fun oppgaveMedEgenskaper(
            id: Long,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            hendelseId: UUID,
            egenskaper: List<Oppgavetype>,
            totrinnsvurdering: Totrinnsvurdering? = null
        ): Oppgave {
            val hovedegenskap = egenskaper.firstOrNull() ?: Oppgavetype.SØKNAD
            return Oppgave(id, hovedegenskap, AvventerSaksbehandler, vedtaksperiodeId, utbetalingId, hendelseId, totrinnsvurdering).also {
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
                tilstand = oppgave.tilstand.toString(),
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
                tilstand = oppgave.tilstand.toString(),
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
            tilstand: String,
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
                "status" to tilstand,
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
}
