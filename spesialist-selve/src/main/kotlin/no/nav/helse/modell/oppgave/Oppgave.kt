package no.nav.helse.modell.oppgave

import java.util.Objects
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Gruppe
import no.nav.helse.Tilgangskontroll
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.slf4j.LoggerFactory

class Oppgave private constructor(
    private val type: Oppgavetype,
    private var status: Oppgavestatus,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID
) {
    private var id: Long? = null
    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null

    constructor(
        id: Long,
        type: Oppgavetype,
        status: Oppgavestatus,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        ferdigstiltAvIdent: String? = null,
        ferdigstiltAvOid: UUID? = null
    ) : this(type, status, vedtaksperiodeId, utbetalingId) {
        this.id = id
        this.ferdigstiltAvIdent = ferdigstiltAvIdent
        this.ferdigstiltAvOid = ferdigstiltAvOid
    }

    private fun oppgaveId() = checkNotNull(id) { "Forventet at oppgave med id=$id skulle finnes?" }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)

        fun søknad(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.SØKNAD, vedtaksperiodeId, utbetalingId)
        fun stikkprøve(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.STIKKPRØVE, vedtaksperiodeId, utbetalingId)
        fun revurdering(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.REVURDERING, vedtaksperiodeId, utbetalingId)
        fun riskQA(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.RISK_QA, vedtaksperiodeId, utbetalingId)
        fun fortroligAdressebeskyttelse(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.FORTROLIG_ADRESSE, vedtaksperiodeId, utbetalingId)
        fun utbetalingTilSykmeldt(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.UTBETALING_TIL_SYKMELDT, vedtaksperiodeId, utbetalingId)
        fun delvisRefusjon(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.DELVIS_REFUSJON, vedtaksperiodeId, utbetalingId)

        private fun oppgave(type: Oppgavetype, vedtaksperiodeId: UUID, utbetalingId: UUID) =
            Oppgave(type, Oppgavestatus.AvventerSaksbehandler, vedtaksperiodeId, utbetalingId)

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

    fun ferdigstill(ident: String, oid: UUID) {
        status = Oppgavestatus.Ferdigstilt
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    fun ferdigstill() {
        status = Oppgavestatus.Ferdigstilt
    }

    private fun avventerSystem(ident: String, oid: UUID) {
        status = Oppgavestatus.AvventerSystem
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    fun lagMelding(eventName: String, oppgaveDao: OppgaveDao): JsonMessage {
        return lagMelding(oppgaveId(), eventName, false, oppgaveDao).second
    }

    fun loggOppgaverAvbrutt(vedtaksperiodeId: UUID) {
        logg.info("Har avbrutt oppgave $id for vedtaksperiode $vedtaksperiodeId")
    }

    fun lagreAvventerSystem(oppgaveDao: OppgaveDao, ident: String, oid: UUID) {
        avventerSystem(ident, oid)
        oppgaveDao.updateOppgave(oppgaveId(), status, ferdigstiltAvIdent, ferdigstiltAvOid)
    }

    fun lagre(oppgaveMediator: OppgaveMediator, contextId: UUID, hendelseId: UUID) {
        id?.also {
            oppgaveMediator.oppdater(it, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        } ?: oppgaveMediator.opprett(contextId, vedtaksperiodeId, utbetalingId, type, hendelseId).also { id = it } ?: return
    }

    fun avbryt() {
        status = Oppgavestatus.Invalidert
    }

    fun forsøkTildeling(
        oppgaveMediator: OppgaveMediator,
        saksbehandleroid: UUID,
        påVent: Boolean = false,
        harTilgangTil: Tilgangskontroll,
    ) {
        if (type == Oppgavetype.STIKKPRØVE) {
            logg.info("OppgaveId $id er stikkprøve og tildeles ikke på tross av reservasjon.")
            return
        }
        if (type == Oppgavetype.RISK_QA) {
            val harTilgangTilRisk = runBlocking { harTilgangTil(saksbehandleroid, Gruppe.RISK_QA) }
            if (!harTilgangTilRisk) logg.info("OppgaveId $id er RISK_QA og saksbehandler har ikke tilgang, tildeles ikke på tross av reservasjon.")
            return
        }
        oppgaveMediator.tildel(checkNotNull(id), saksbehandleroid, påVent)
        logg.info("Oppgave $id tildeles $saksbehandleroid grunnet reservasjon.")
    }

    fun lagrePeriodehistorikk(
        periodehistorikkDao: PeriodehistorikkDao,
        saksbehandleroid: UUID?,
        type: PeriodehistorikkType,
        notatId: Int?
    ) {
        periodehistorikkDao.lagre(type, saksbehandleroid, utbetalingId, notatId)
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
