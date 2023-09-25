package no.nav.helse.modell.oppgave

import java.util.Objects
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import org.slf4j.LoggerFactory

class Oppgave private constructor(
    private val id: Long,
    private val egenskap: Egenskap,
    private var tilstand: Tilstand,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val hendelseId: UUID,
    private val totrinnsvurdering: Totrinnsvurdering?
) {

    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null
    private val egenskaper = mutableListOf<Egenskap>()
    private var tildeltTil: Saksbehandler? = null
    private var påVent: Boolean = false

    private val observers = mutableListOf<OppgaveObserver>()

    internal constructor(
        id: Long,
        egenskap: Egenskap,
        tilstand: Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvIdent: String? = null,
        ferdigstiltAvOid: UUID? = null,
        tildelt: Saksbehandler? = null,
        påVent: Boolean = false,
        totrinnsvurdering: Totrinnsvurdering? = null
    ) : this(id, egenskap, tilstand, vedtaksperiodeId, utbetalingId, hendelseId, totrinnsvurdering) {
        this.ferdigstiltAvIdent = ferdigstiltAvIdent
        this.ferdigstiltAvOid = ferdigstiltAvOid
        this.tildeltTil = tildelt
        this.påVent = påVent
    }

    fun accept(visitor: OppgaveVisitor) {
        visitor.visitOppgave(id, egenskap, tilstand, vedtaksperiodeId, utbetalingId, hendelseId, ferdigstiltAvOid, ferdigstiltAvIdent, egenskaper, tildeltTil, påVent, totrinnsvurdering)
        totrinnsvurdering?.accept(visitor)
    }

    fun register(observer: OppgaveObserver) {
        observers.add(observer)
    }

    internal fun forsøkTildelingVedReservasjon(
        saksbehandler: Saksbehandler,
        påVent: Boolean = false,
    ) {
        logg.info("Oppgave med {} forsøkes tildelt grunnet reservasjon.", kv("oppgaveId", id))
        sikkerlogg.info("Oppgave med {} forsøkes tildelt $saksbehandler grunnet reservasjon.", kv("oppgaveId", id))
        if (egenskap is STIKKPRØVE) {
            logg.info("Oppgave med {} er stikkprøve og tildeles ikke på tross av reservasjon.", kv("oppgaveId", id))
            return
        }
        tilstand.tildel(this, saksbehandler, påVent)
    }

    internal fun sendTilBeslutter(behandlendeSaksbehandler: Saksbehandler) {
        val totrinnsvurdering = requireNotNull(totrinnsvurdering) {
            "Forventer at det eksisterer en aktiv totrinnsvurdering når oppgave sendes til beslutter"
        }

        totrinnsvurdering.sendTilBeslutter(id, behandlendeSaksbehandler)

        egenskaper.remove(RETUR)
        egenskaper.add(BESLUTTER)
        tildeltTil = totrinnsvurdering.tidligereBeslutter()
        oppgaveEndret()
    }

    internal fun sendIRetur(besluttendeSaksbehandler: Saksbehandler) {
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

    internal fun leggPåVent(): Tildeling {
        val tildeltTil = this.tildeltTil ?: throw OppgaveIkkeTildelt(id)
        påVent = true
        oppgaveEndret()
        return tildeltTil.tildeling(påVent)
    }

    internal fun fjernPåVent(): Tildeling {
        val tildeltTil = this.tildeltTil ?: throw OppgaveIkkeTildelt(id)
        påVent = false
        oppgaveEndret()
        return tildeltTil.tildeling(påVent)
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

    private fun tildel(saksbehandler: Saksbehandler, påVent: Boolean) {
        this.tildeltTil = saksbehandler
        this.påVent = påVent
        logg.info("Oppgave med {} tildeles saksbehandler med {}", kv("oppgaveId", id), kv("oid", saksbehandler.oid()))
        sikkerlogg.info("Oppgave med {} tildeles $saksbehandler", kv("oppgaveId", id))
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

        fun tildel(oppgave: Oppgave, saksbehandler: Saksbehandler, påVent: Boolean) {
            logg.error(
                "Forventer ikke forsøk på tildeling i {} for oppgave med {} av $saksbehandler",
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

        override fun tildel(oppgave: Oppgave, saksbehandler: Saksbehandler, påVent: Boolean) {
            if (oppgave.egenskap is TilgangsstyrtEgenskap && !saksbehandler.harTilgangTil(oppgave.egenskap)) {
                logg.info(
                    "Oppgave med {} har egenskaper som saksbehandler med {} ikke har tilgang til å behandle.",
                    kv("oppgaveId", oppgave.id),
                    kv("oid", saksbehandler.oid())
                )
                return
            }
            oppgave.tildel(saksbehandler, påVent)
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
        return this.egenskap == other.egenskap && this.vedtaksperiodeId == other.vedtaksperiodeId
    }

    override fun hashCode(): Int {
        return Objects.hash(id, egenskap, vedtaksperiodeId)
    }

    override fun toString(): String {
        return "Oppgave(type=$egenskap, tilstand=$tilstand, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, id=$id)"
    }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        fun nyOppgave(
            id: Long,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            hendelseId: UUID,
            egenskaper: List<Egenskap>,
            totrinnsvurdering: Totrinnsvurdering? = null
        ): Oppgave {
            val hovedegenskap = egenskaper.firstOrNull { it in gyldigeOppgavetyper } ?: SØKNAD
            return Oppgave(id, hovedegenskap, AvventerSaksbehandler, vedtaksperiodeId, utbetalingId, hendelseId, totrinnsvurdering).also {
                it.egenskaper.addAll(egenskaper)
            }
        }

        // Brukes midlertidig mens vi står i en spagat mellom én oppgavetype og en liste av egenskaper.
        // Brukes slik at vi kan legge til nye egenskaper i kode som ikke finnes i oppgavetype-enumen i databasen og/eller er håndtert i Speil
        private val gyldigeOppgavetyper = listOf(FORTROLIG_ADRESSE, REVURDERING, STIKKPRØVE, RISK_QA, DELVIS_REFUSJON, UTBETALING_TIL_SYKMELDT)
    }
}
