package no.nav.helse.mediator.oppgave

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.oppgave.BESLUTTER
import no.nav.helse.modell.oppgave.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.FULLMAKT
import no.nav.helse.modell.oppgave.HASTER
import no.nav.helse.modell.oppgave.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveObserver
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.modell.oppgave.RETUR
import no.nav.helse.modell.oppgave.REVURDERING
import no.nav.helse.modell.oppgave.RISK_QA
import no.nav.helse.modell.oppgave.STIKKPRØVE
import no.nav.helse.modell.oppgave.SØKNAD
import no.nav.helse.modell.oppgave.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.tildeling.IOppgavemelder
import kotlin.properties.Delegates

internal class Oppgavemelder(private val hendelseDao: HendelseDao, private val oppgaveDao: OppgaveDao, private val rapidsConnection: RapidsConnection): OppgaveObserver, IOppgavemelder {

    override fun sendOppgaveOppdatertMelding(oppgaveId: Long) {
        lagOppgaveOppdatertMelding(oppgaveId).also { (key, message) ->
            rapidsConnection.publish(key, message.toJson())
        }
    }

    internal fun oppgaveOpprettet(oppgave: Oppgave) {
        val oppgavemelding = OppgaveForKafkaBygger().bygg(oppgave)
        val (fnr, melding) = melding("oppgave_opprettet", oppgavemelding)
        rapidsConnection.publish(fnr, melding.toJson())
    }

    override fun oppgaveEndret(oppgave: Oppgave) {
        val oppgavemelding = OppgaveForKafkaBygger().bygg(oppgave)
        val (fnr, melding) = melding("oppgave_oppdatert", oppgavemelding)
        rapidsConnection.publish(fnr, melding.toJson())
    }

    private fun lagOppgaveOppdatertMelding(oppgaveId: Long): Pair<String, JsonMessage> {
        val oppgavemelding: Oppgavemelding = requireNotNull(oppgaveDao.hentOppgavemelding(oppgaveId))
        return melding("oppgave_oppdatert", oppgavemelding)
    }

    private fun melding(eventName: String, oppgavemelding: Oppgavemelding): Pair<String, JsonMessage> {
        val fødselsnummer: String = hendelseDao.finnFødselsnummer(oppgavemelding.hendelseId)
        return fødselsnummer to JsonMessage.newMessage(eventName, mutableMapOf(
            "@forårsaket_av" to mapOf("id" to oppgavemelding.hendelseId),
            "hendelseId" to oppgavemelding.hendelseId,
            "oppgaveId" to oppgavemelding.oppgaveId,
            "status" to oppgavemelding.status,
            "type" to oppgavemelding.type,
            "fødselsnummer" to fødselsnummer,
            "erBeslutterOppgave" to (oppgavemelding.beslutter != null),
            "erReturOppgave" to oppgavemelding.erRetur,
            "påVent" to oppgavemelding.påVent
        ).apply {
            oppgavemelding.ferdigstiltAvIdent?.also { put("ferdigstiltAvIdent", it) }
            oppgavemelding.ferdigstiltAvOid?.also { put("ferdigstiltAvOid", it) }
        })
    }

    data class Oppgavemelding(
        val hendelseId: UUID,
        val oppgaveId: Long,
        val status: String,
        val type: String,
        val beslutter: UUID?,
        val erRetur: Boolean,
        val ferdigstiltAvIdent: String? = null,
        val ferdigstiltAvOid: UUID? = null,
        val påVent: Boolean,
    )
}

private class OppgaveForKafkaBygger : OppgaveVisitor {
    private var beslutterId: UUID? = null
    private var erRetur: Boolean = false
    private lateinit var hendelseId: UUID
    private var oppgaveId by Delegates.notNull<Long>()
    private lateinit var status: String
    private lateinit var type: String
    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null
    private var påVent by Delegates.notNull<Boolean>()

    fun bygg(oppgave: Oppgave): Oppgavemelder.Oppgavemelding {
        oppgave.accept(this)
        return Oppgavemelder.Oppgavemelding(
            hendelseId = hendelseId,
            oppgaveId = oppgaveId,
            status = status,
            type = type,
            beslutter = beslutterId,
            erRetur = erRetur,
            ferdigstiltAvIdent = ferdigstiltAvIdent,
            ferdigstiltAvOid = ferdigstiltAvOid,
            påVent = påVent
        )
    }

    override fun visitOppgave(
        id: Long,
        egenskap: Egenskap,
        tilstand: Oppgave.Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Egenskap>,
        tildelt: Saksbehandler?,
        påVent: Boolean,
        totrinnsvurdering: Totrinnsvurdering?
    ) {
        this.hendelseId = hendelseId
        this.oppgaveId = id
        this.status = mapTilstand(tilstand)
        this.type = mapEgenskap(egenskap)
        this.ferdigstiltAvIdent = ferdigstiltAvIdent
        this.ferdigstiltAvOid = ferdigstiltAvOid
        this.påVent = påVent
    }

    override fun visitTotrinnsvurdering(
        vedtaksperiodeId: UUID,
        erRetur: Boolean,
        saksbehandler: Saksbehandler?,
        beslutter: Saksbehandler?,
        utbetalingId: UUID?,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime?
    ) {
        beslutterId = beslutter?.oid()
        this.erRetur = erRetur
    }

    private fun mapTilstand(tilstand: Oppgave.Tilstand): String {
        return when (tilstand) {
            Oppgave.AvventerSaksbehandler -> "AvventerSaksbehandler"
            Oppgave.AvventerSystem -> "AvventerSystem"
            Oppgave.Ferdigstilt -> "Ferdigstilt"
            Oppgave.Invalidert -> "Invalidert"
        }
    }

    private fun mapEgenskap(egenskap: Egenskap): String {
        return when (egenskap) {
            SØKNAD -> "SØKNAD"
            STIKKPRØVE -> "STIKKPRØVE"
            RISK_QA -> "RISK_QA"
            REVURDERING -> "REVURDERING"
            FORTROLIG_ADRESSE -> "FORTROLIG_ADRESSE"
            UTBETALING_TIL_SYKMELDT -> "UTBETALING_TIL_SYKMELDT"
            DELVIS_REFUSJON -> "DELVIS_REFUSJON"
            UTBETALING_TIL_ARBEIDSGIVER -> "UTBETALING_TIL_ARBEIDSGIVER"
            INGEN_UTBETALING -> "INGEN_UTBETALING"
            EGEN_ANSATT -> "EGEN_ANSATT"
            HASTER -> "HASTER"
            BESLUTTER -> "BESLUTTER"
            RETUR -> "RETUR"
            FULLMAKT -> "FULLMAKT"
        }
    }
}