package no.nav.helse.modell.oppgave

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.OppgaveIkkeTildelt
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.GOSYS
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.RETUR
import no.nav.helse.modell.oppgave.Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.TILBAKEDATERT
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle.SELVSTSTENDIG_NÆRINGSDRIVENDE_BETA
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class Oppgave private constructor(
    val id: Long,
    val opprettet: LocalDateTime,
    val førsteOpprettet: LocalDateTime?,
    tilstand: Tilstand,
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val utbetalingId: UUID,
    val godkjenningsbehovId: UUID,
    val kanAvvises: Boolean,
    ferdigstiltAvIdent: NAVIdent?,
    ferdigstiltAvOid: UUID?,
    egenskaper: Set<Egenskap>,
    tildeltTil: SaksbehandlerOid?,
) {
    private val observers = mutableListOf<OppgaveObserver>()
    private val _egenskaper = egenskaper.toMutableSet()

    var ferdigstiltAvOid = ferdigstiltAvOid
        private set
    var ferdigstiltAvIdent: NAVIdent? = ferdigstiltAvIdent
        private set

    var tilstand: Tilstand = tilstand
        private set

    var tildeltTil: SaksbehandlerOid? = tildeltTil
        private set

    val egenskaper: Set<Egenskap> get() = _egenskaper.toSet()

    fun register(observer: OppgaveObserver) {
        observers.add(observer)
    }

    fun forsøkTildeling(
        saksbehandlerWrapper: SaksbehandlerWrapper,
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
        brukerroller: Set<Brukerrolle>,
    ) {
        logg.info("Oppgave med {} forsøkes tildelt av saksbehandler.", kv("oppgaveId", id))
        val tildelt = tildeltTil
        if (tildelt != null && tildelt != saksbehandlerWrapper.saksbehandler.id) {
            logg.warn("Oppgave med {} kan ikke tildeles fordi den er tildelt noen andre.", kv("oppgaveId", id))
            throw OppgaveTildeltNoenAndre(tildelt.value, false)
        }
        tilstand.tildel(
            oppgave = this,
            saksbehandlerWrapper = saksbehandlerWrapper,
            saksbehandlerTilgangsgrupper = saksbehandlerTilgangsgrupper,
            brukerroller = brukerroller,
        )
    }

    fun forsøkAvmelding(saksbehandlerWrapper: SaksbehandlerWrapper) {
        logg.info("Oppgave med {} forsøkes avmeldt av saksbehandler.", kv("oppgaveId", id))
        val tildelt =
            tildeltTil ?: run {
                logg.info("Kan ikke fjerne tildeling når oppgave ikke er tildelt, {}", kv("oppgaveId", id))
                throw OppgaveIkkeTildelt(this.id)
            }

        if (tildelt != saksbehandlerWrapper.saksbehandler.id) {
            logg.info("Oppgave med {} er tildelt noen andre, avmeldes", kv("oppgaveId", id))
            sikkerlogg.info(
                "Oppgave med {} er tildelt $tildelt, avmeldes av $saksbehandlerWrapper",
                kv("oppgaveId", id),
            )
        }
        tilstand.avmeld(this, saksbehandlerWrapper)
    }

    fun forsøkTildelingVedReservasjon(
        saksbehandlerWrapper: SaksbehandlerWrapper,
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
        brukerroller: Set<Brukerrolle>,
    ) {
        logg.info("Oppgave med {} forsøkes tildelt grunnet reservasjon.", kv("oppgaveId", id))
        sikkerlogg.info(
            "Oppgave med {} forsøkes tildelt $saksbehandlerWrapper grunnet reservasjon.",
            kv("oppgaveId", id),
        )
        if (_egenskaper.contains(STIKKPRØVE)) {
            logg.info("Oppgave med {} er stikkprøve og tildeles ikke på tross av reservasjon.", kv("oppgaveId", id))
            return
        }
        tilstand.tildel(
            oppgave = this,
            saksbehandlerWrapper = saksbehandlerWrapper,
            saksbehandlerTilgangsgrupper = saksbehandlerTilgangsgrupper,
            brukerroller = brukerroller,
        )
    }

    fun sendTilBeslutter(beslutter: SaksbehandlerWrapper?) {
        _egenskaper.remove(RETUR)
        _egenskaper.add(BESLUTTER)
        tildeltTil = beslutter?.saksbehandler?.id
        oppgaveEndret()
    }

    fun sendIRetur(opprinneligSaksbehandlerWrapper: SaksbehandlerWrapper) {
        _egenskaper.remove(BESLUTTER)
        _egenskaper.add(RETUR)
        tildeltTil = opprinneligSaksbehandlerWrapper.saksbehandler.id
        oppgaveEndret()
    }

    fun leggTilEgenAnsatt() {
        _egenskaper.add(EGEN_ANSATT)
        oppgaveEndret()
    }

    fun fjernEgenAnsatt() {
        _egenskaper.remove(EGEN_ANSATT)
        oppgaveEndret()
    }

    fun fjernTilbakedatert() {
        _egenskaper.remove(TILBAKEDATERT)
        oppgaveEndret()
    }

    fun fjernGosys() {
        if (_egenskaper.remove(GOSYS)) {
            logg.info(
                "Fjerner egenskap GOSYS på {} for {}",
                kv("oppgaveId", id),
                kv("vedtaksperiodeId", vedtaksperiodeId),
            )
            oppgaveEndret()
        }
    }

    fun leggTilGosys() {
        if (_egenskaper.add(GOSYS)) {
            logg.info(
                "Legger til egenskap GOSYS på {} for {}",
                kv("oppgaveId", id),
                kv("vedtaksperiodeId", vedtaksperiodeId),
            )
            oppgaveEndret()
        }
    }

    fun leggPåVent(
        skalTildeles: Boolean,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        if (this.tildeltTil != saksbehandlerWrapper.saksbehandler.id && skalTildeles) {
            tildel(saksbehandlerWrapper)
        }
        if (this.tildeltTil != null && !skalTildeles) {
            avmeld(saksbehandlerWrapper)
        }
        _egenskaper.add(PÅ_VENT)
        oppgaveEndret()
    }

    fun endrePåVent(
        skalVæreTildeltSaksbehandler: Boolean,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        if (tildeltTil == saksbehandlerWrapper.saksbehandler.id) {
            if (!skalVæreTildeltSaksbehandler) {
                avmeld(saksbehandlerWrapper)
            }
        } else if (skalVæreTildeltSaksbehandler) {
            tildel(saksbehandlerWrapper)
        }
    }

    fun fjernFraPåVent() {
        _egenskaper.remove(PÅ_VENT)
        oppgaveEndret()
    }

    fun ferdigstill() {
        tilstand.ferdigstill(this)
    }

    fun avventerSystem(
        ident: NAVIdent,
        oid: UUID,
    ) {
        tilstand.avventerSystem(this, ident, oid)
    }

    fun avbryt() {
        tilstand.invalider(this)
    }

    private fun tildel(saksbehandlerWrapper: SaksbehandlerWrapper) {
        this.tildeltTil = saksbehandlerWrapper.saksbehandler.id
        logg.info(
            "Oppgave med {} tildeles saksbehandler med {}",
            kv("oppgaveId", id),
            kv(
                "oid",
                saksbehandlerWrapper.saksbehandler.id.value,
            ),
        )
        sikkerlogg.info("Oppgave med {} tildeles $saksbehandlerWrapper", kv("oppgaveId", id))
        oppgaveEndret()
    }

    private fun avmeld(saksbehandlerWrapper: SaksbehandlerWrapper) {
        this.tildeltTil = null
        logg.info(
            "Oppgave med {} avmeldes saksbehandler med {}",
            kv("oppgaveId", id),
            kv(
                "oid",
                saksbehandlerWrapper.saksbehandler.id.value,
            ),
        )
        sikkerlogg.info("Oppgave med {} avmeldes $saksbehandlerWrapper", kv("oppgaveId", id))
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
                kv("oppgaveId", oppgave.id),
            )
        }

        fun avventerSystem(
            oppgave: Oppgave,
            ident: NAVIdent,
            oid: UUID,
        ) {
            logg.warn(
                "Forventer ikke avventer system i {} for oppgave med {}",
                kv("tilstand", this),
                kv("oppgaveId", oppgave.id),
            )
        }

        fun ferdigstill(oppgave: Oppgave) {
            logg.warn(
                "Forventer ikke ferdigstillelse i {} for oppgave med {}",
                kv("tilstand", this),
                kv("oppgaveId", oppgave.id),
            )
        }

        fun tildel(
            oppgave: Oppgave,
            saksbehandlerWrapper: SaksbehandlerWrapper,
            saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
            brukerroller: Set<Brukerrolle>,
        ) {
            logg.warn(
                "Forventer ikke forsøk på tildeling i {} for oppgave med {} av $saksbehandlerWrapper",
                kv("tilstand", this),
                kv("oppgaveId", oppgave.id),
            )
        }

        fun avmeld(
            oppgave: Oppgave,
            saksbehandlerWrapper: SaksbehandlerWrapper,
        ) {
            logg.warn(
                "Forventer ikke forsøk på avmelding i {} for oppgave med {} av $saksbehandlerWrapper",
                kv("tilstand", this),
                kv("oppgaveId", oppgave.id),
            )
        }
    }

    data object AvventerSaksbehandler : Tilstand {
        override fun avventerSystem(
            oppgave: Oppgave,
            ident: NAVIdent,
            oid: UUID,
        ) {
            oppgave.ferdigstiltAvIdent = ident
            oppgave.ferdigstiltAvOid = oid
            oppgave.nesteTilstand(AvventerSystem)
        }

        override fun invalider(oppgave: Oppgave) {
            oppgave.nesteTilstand(Invalidert)
        }

        override fun tildel(
            oppgave: Oppgave,
            saksbehandlerWrapper: SaksbehandlerWrapper,
            saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
            brukerroller: Set<Brukerrolle>,
        ) {
            if (!oppgave.kanTildelesTil(
                    saksbehandlerTilgangsgrupper = saksbehandlerTilgangsgrupper,
                    brukerroller = brukerroller,
                )
            ) {
                logg.info(
                    "Oppgave med {} har egenskaper som saksbehandler med {} ikke har tilgang til å behandle.",
                    kv("oppgaveId", oppgave.id),
                    kv("oid", saksbehandlerWrapper.saksbehandler.id.value),
                )
                throw ManglerTilgang(saksbehandlerWrapper.saksbehandler.id.value, oppgave.id)
            }
            oppgave.tildel(saksbehandlerWrapper)
        }

        override fun avmeld(
            oppgave: Oppgave,
            saksbehandlerWrapper: SaksbehandlerWrapper,
        ) {
            oppgave.avmeld(saksbehandlerWrapper)
        }
    }

    data object AvventerSystem : Tilstand {
        override fun ferdigstill(oppgave: Oppgave) {
            oppgave.nesteTilstand(Ferdigstilt)
        }

        override fun invalider(oppgave: Oppgave) {
            oppgave.nesteTilstand(Invalidert)
        }
    }

    data object Ferdigstilt : Tilstand

    data object Invalidert : Tilstand

    override fun equals(other: Any?): Boolean {
        if (other !is Oppgave) return false
        if (this.id != other.id) return false
        return this.egenskaper == other.egenskaper &&
            this.vedtaksperiodeId == other.vedtaksperiodeId
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + egenskaper.hashCode()
        return result
    }

    override fun toString(): String = "Oppgave(tilstand=$tilstand, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, id=$id)"

    fun kanSeesAv(
        brukerroller: Set<Brukerrolle>,
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean =
        egenskaper.all {
            harTilgangTilEgenskap(
                egenskap = it,
                saksbehandlerTilgangsgrupper = saksbehandlerTilgangsgrupper,
                brukerroller = brukerroller,
            )
        }

    fun kanTildelesTil(
        brukerroller: Set<Brukerrolle>,
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean =
        egenskaper.all {
            harTilgangTilEgenskap(
                egenskap = it,
                saksbehandlerTilgangsgrupper = saksbehandlerTilgangsgrupper,
                brukerroller = brukerroller,
            ) &&
                when (it) {
                    BESLUTTER -> Tilgangsgruppe.BESLUTTER in saksbehandlerTilgangsgrupper
                    STIKKPRØVE -> Brukerrolle.STIKKPRØVE in brukerroller
                    else -> true
                }
        }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java.declaringClass)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        fun ny(
            id: Long,
            førsteOpprettet: LocalDateTime?,
            vedtaksperiodeId: UUID,
            behandlingId: UUID,
            utbetalingId: UUID,
            hendelseId: UUID,
            kanAvvises: Boolean,
            egenskaper: Set<Egenskap>,
        ): Oppgave {
            val opprettet = LocalDateTime.now()
            return Oppgave(
                id = id,
                opprettet = opprettet,
                førsteOpprettet = førsteOpprettet ?: opprettet,
                tilstand = AvventerSaksbehandler,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = utbetalingId,
                godkjenningsbehovId = hendelseId,
                kanAvvises = kanAvvises,
                egenskaper = egenskaper,
                ferdigstiltAvIdent = null,
                ferdigstiltAvOid = null,
                tildeltTil = null,
            )
        }

        fun fraLagring(
            id: Long,
            opprettet: LocalDateTime,
            førsteOpprettet: LocalDateTime?,
            tilstand: Tilstand,
            behandlingId: UUID,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            godkjenningsbehovId: UUID,
            kanAvvises: Boolean,
            ferdigstiltAvOid: UUID?,
            ferdigstiltAvIdent: NAVIdent?,
            tildeltTil: SaksbehandlerOid?,
            egenskaper: Set<Egenskap>,
        ) = Oppgave(
            id = id,
            opprettet = opprettet,
            førsteOpprettet = førsteOpprettet,
            tilstand = tilstand,
            behandlingId = behandlingId,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            godkjenningsbehovId = godkjenningsbehovId,
            kanAvvises = kanAvvises,
            ferdigstiltAvOid = ferdigstiltAvOid,
            ferdigstiltAvIdent = ferdigstiltAvIdent,
            tildeltTil = tildeltTil,
            egenskaper = egenskaper,
        )

        fun harTilgangTilEgenskap(
            egenskap: Egenskap,
            saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>,
            brukerroller: Set<Brukerrolle>,
        ): Boolean =
            when (egenskap) {
                // Ingen skal ha tilgang til strengt fortrolig adresse i Speil foreløpig
                STRENGT_FORTROLIG_ADRESSE -> {
                    false
                }

                EGEN_ANSATT -> {
                    Tilgangsgruppe.EGEN_ANSATT in saksbehandlerTilgangsgrupper
                }

                FORTROLIG_ADRESSE -> {
                    Tilgangsgruppe.KODE_7 in saksbehandlerTilgangsgrupper
                }

                SELVSTENDIG_NÆRINGSDRIVENDE -> {
                    SELVSTSTENDIG_NÆRINGSDRIVENDE_BETA in brukerroller
                }

                else -> {
                    true
                }
            }
    }
}
