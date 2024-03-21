package no.nav.helse.modell.oppgave

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.OppgaveIkkeTildelt
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.Companion.tilgangsstyrteEgenskaper
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.RETUR
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import org.slf4j.LoggerFactory

class Oppgave private constructor(
    private val id: Long,
    private var tilstand: Tilstand,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val hendelseId: UUID,
    private val kanAvvises: Boolean,
    private val totrinnsvurdering: Totrinnsvurdering?
) {

    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null
    private val egenskaper = mutableSetOf<Egenskap>()
    private var tildeltTil: Saksbehandler? = null

    private val observers = mutableListOf<OppgaveObserver>()

    constructor(
        id: Long,
        tilstand: Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        kanAvvises: Boolean,
        ferdigstiltAvIdent: String? = null,
        ferdigstiltAvOid: UUID? = null,
        tildelt: Saksbehandler? = null,
        totrinnsvurdering: Totrinnsvurdering? = null,
        egenskaper: List<Egenskap>
    ) : this(id, tilstand, vedtaksperiodeId, utbetalingId, hendelseId, kanAvvises, totrinnsvurdering) {
        this.ferdigstiltAvIdent = ferdigstiltAvIdent
        this.ferdigstiltAvOid = ferdigstiltAvOid
        this.tildeltTil = tildelt
        this.egenskaper.addAll(egenskaper)
    }

    fun accept(visitor: OppgaveVisitor) {
        visitor.visitOppgave(
            id,
            tilstand,
            vedtaksperiodeId,
            utbetalingId,
            hendelseId,
            ferdigstiltAvOid,
            ferdigstiltAvIdent,
            egenskaper.toList(),
            tildeltTil,
            kanAvvises,
            totrinnsvurdering
        )
        totrinnsvurdering?.accept(visitor)
    }

    fun register(observer: OppgaveObserver) {
        observers.add(observer)
    }

    internal fun forsøkTildeling(saksbehandler: Saksbehandler) {
        logg.info("Oppgave med {} forsøkes tildelt av saksbehandler.", kv("oppgaveId", id))
        val tildelt = tildeltTil
        if (tildelt != null && tildelt != saksbehandler) {
            logg.warn("Oppgave med {} kan ikke tildeles fordi den er tildelt noen andre.", kv("oppgaveId", id))
            throw OppgaveTildeltNoenAndre(tildelt, false)
        }
        tilstand.tildel(this, saksbehandler)
    }

    internal fun forsøkAvmelding(saksbehandler: Saksbehandler) {
        logg.info("Oppgave med {} forsøkes avmeldt av saksbehandler.", kv("oppgaveId", id))
        val tildelt = tildeltTil ?: throw OppgaveIkkeTildelt(this.id)

        if (tildelt != saksbehandler) {
            logg.info("Oppgave med {} er tildelt noen andre, avmeldes", kv("oppgaveId", id))
            sikkerlogg.info("Oppgave med {} er tildelt $tildelt, avmeldes av $saksbehandler", kv("oppgaveId", id))
        }
        tilstand.avmeld(this, saksbehandler)
    }

    fun forsøkTildelingVedReservasjon(
        saksbehandler: Saksbehandler,
    ) {
        logg.info("Oppgave med {} forsøkes tildelt grunnet reservasjon.", kv("oppgaveId", id))
        sikkerlogg.info("Oppgave med {} forsøkes tildelt $saksbehandler grunnet reservasjon.", kv("oppgaveId", id))
        if (egenskaper.contains(STIKKPRØVE)) {
            logg.info("Oppgave med {} er stikkprøve og tildeles ikke på tross av reservasjon.", kv("oppgaveId", id))
            return
        }
        tilstand.tildel(this, saksbehandler)
    }

    fun sendTilBeslutter(behandlendeSaksbehandler: Saksbehandler) {
        val totrinnsvurdering = requireNotNull(totrinnsvurdering) {
            "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
        }

        totrinnsvurdering.sendTilBeslutter(id, behandlendeSaksbehandler)

        egenskaper.remove(RETUR)
        egenskaper.add(BESLUTTER)
        tildeltTil = totrinnsvurdering.tidligereBeslutter()
        oppgaveEndret()
    }

    fun sendIRetur(besluttendeSaksbehandler: Saksbehandler) {
        val totrinnsvurdering = requireNotNull(totrinnsvurdering) {
            "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
        }

        totrinnsvurdering.sendIRetur(id, besluttendeSaksbehandler)

        val opprinneligSaksbehandler = requireNotNull(totrinnsvurdering.opprinneligSaksbehandler()) {
            "Opprinnelig saksbehandler kan ikke være null ved retur av beslutteroppgave"
        }

        egenskaper.remove(BESLUTTER)
        egenskaper.add(RETUR)

        tildeltTil = opprinneligSaksbehandler
        oppgaveEndret()
    }

    fun leggTilEgenAnsatt() {
        egenskaper.add(EGEN_ANSATT)
        oppgaveEndret()
    }

    fun fjernEgenAnsatt() {
        egenskaper.remove(EGEN_ANSATT)
        oppgaveEndret()
    }

    fun leggPåVent(skalTildeles: Boolean, saksbehandler: Saksbehandler) {
        if (this.tildeltTil != saksbehandler && skalTildeles) {
            this.tildeltTil = saksbehandler
        }
        if (this.tildeltTil != null && !skalTildeles) {
            this.tildeltTil = null
        }
        egenskaper.add(PÅ_VENT)
        oppgaveEndret()
    }

    fun fjernPåVent() {
        egenskaper.remove(PÅ_VENT)
        oppgaveEndret()
    }

    fun ferdigstill() {
        tilstand.ferdigstill(this)
    }

    fun avventerSystem(ident: String, oid: UUID) {
        tilstand.avventerSystem(this, ident, oid)
    }

    fun avbryt() {
        tilstand.invalider(this)
    }

    private fun tildel(saksbehandler: Saksbehandler) {
        this.tildeltTil = saksbehandler
        logg.info("Oppgave med {} tildeles saksbehandler med {}", kv("oppgaveId", id), kv("oid", saksbehandler.oid()))
        sikkerlogg.info("Oppgave med {} tildeles $saksbehandler", kv("oppgaveId", id))
        oppgaveEndret()
    }

    private fun avmeld(saksbehandler: Saksbehandler) {
        this.tildeltTil = null
        logg.info("Oppgave med {} avmeldes saksbehandler med {}", kv("oppgaveId", id), kv("oid", saksbehandler.oid()))
        sikkerlogg.info("Oppgave med {} avmeldes $saksbehandler", kv("oppgaveId", id))
        oppgaveEndret()
    }

    private fun oppgaveEndret() {
        observers.forEach { it.oppgaveEndret(this) }
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
        oppgaveEndret()
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

        fun tildel(oppgave: Oppgave, saksbehandler: Saksbehandler) {
            logg.warn(
                "Forventer ikke forsøk på tildeling i {} for oppgave med {} av $saksbehandler",
                kv("tilstand", this),
                kv("oppgaveId", oppgave.id)
            )
        }

        fun avmeld(oppgave: Oppgave, saksbehandler: Saksbehandler) {
            logg.warn(
                "Forventer ikke forsøk på avmelding i {} for oppgave med {} av $saksbehandler",
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

        override fun tildel(oppgave: Oppgave, saksbehandler: Saksbehandler) {
            val tilgangsstyrteEgenskaper = oppgave.egenskaper.tilgangsstyrteEgenskaper()
            if (tilgangsstyrteEgenskaper.isNotEmpty() && !saksbehandler.harTilgangTil(tilgangsstyrteEgenskaper)) {
                logg.info(
                    "Oppgave med {} har egenskaper som saksbehandler med {} ikke har tilgang til å behandle.",
                    kv("oppgaveId", oppgave.id),
                    kv("oid", saksbehandler.oid())
                )
                throw ManglerTilgang(saksbehandler.oid(), oppgave.id)
            }
            oppgave.tildel(saksbehandler)
        }

        override fun avmeld(oppgave: Oppgave, saksbehandler: Saksbehandler) {
            oppgave.avmeld(saksbehandler)
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
        return this.egenskaper.toSet() == other.egenskaper.toSet() &&
                this.vedtaksperiodeId == other.vedtaksperiodeId
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + egenskaper.hashCode()
        return result
    }

    override fun toString(): String {
        return "Oppgave(tilstand=$tilstand, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, id=$id)"
    }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        fun nyOppgave(
            id: Long,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            hendelseId: UUID,
            kanAvvises: Boolean,
            egenskaper: List<Egenskap>,
            totrinnsvurdering: Totrinnsvurdering? = null
        ): Oppgave {
            return Oppgave(id, AvventerSaksbehandler, vedtaksperiodeId, utbetalingId, hendelseId, kanAvvises, totrinnsvurdering).also {
                it.egenskaper.addAll(egenskaper)
            }
        }
    }
}
