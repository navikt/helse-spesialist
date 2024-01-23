package no.nav.helse.mediator.oppgave

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilKafkaversjon
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveObserver
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import kotlin.properties.Delegates

internal class Oppgavemelder(
    private val hendelseDao: HendelseDao,
    private val rapidsConnection: RapidsConnection,
) : OppgaveObserver {

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

    private fun melding(eventName: String, oppgavemelding: OppgaveForKafkaBygger.Oppgavemelding): Pair<String, JsonMessage> {
        val fødselsnummer: String = hendelseDao.finnFødselsnummer(oppgavemelding.hendelseId)
        return fødselsnummer to JsonMessage.newMessage(eventName, mutableMapOf(
            "@forårsaket_av" to mapOf("id" to oppgavemelding.hendelseId),
            "hendelseId" to oppgavemelding.hendelseId,
            "oppgaveId" to oppgavemelding.oppgaveId,
            "tilstand" to oppgavemelding.tilstand,
            "type" to oppgavemelding.type,
            "fødselsnummer" to fødselsnummer,
            "påVent" to oppgavemelding.påVent,
            "egenskaper" to oppgavemelding.egenskaper
        ).apply {
            compute("beslutter") { _, _ -> oppgavemelding.beslutter }
            compute("saksbehandler") { _, _ -> oppgavemelding.saksbehandler }
        })
    }
}

private class OppgaveForKafkaBygger : OppgaveVisitor {
    private var beslutter: Map<String, Any>? = null
    private var saksbehandler: Map<String, Any>? = null
    private lateinit var hendelseId: UUID
    private var oppgaveId by Delegates.notNull<Long>()
    private lateinit var tilstand: String
    private lateinit var type: String
    private var påVent by Delegates.notNull<Boolean>()
    private lateinit var egenskaper: List<String>

    fun bygg(oppgave: Oppgave): Oppgavemelding {
        oppgave.accept(this)
        return Oppgavemelding(
            hendelseId = hendelseId,
            oppgaveId = oppgaveId,
            tilstand = tilstand,
            type = type,
            beslutter = beslutter,
            saksbehandler = saksbehandler,
            påVent = påVent,
            egenskaper = egenskaper
        )
    }

    data class Oppgavemelding(
        val hendelseId: UUID,
        val oppgaveId: Long,
        val tilstand: String,

        @Deprecated("Feltet skal fjernes når Risk bruker egenskaper i stedet")
        val type: String,

        val beslutter: Map<String, Any>?,
        val saksbehandler: Map<String, Any>?,

        @Deprecated("Feltet skal fjernes når Risk bruker egenskaper i stedet")
        val påVent: Boolean,

        val egenskaper: List<String>
    )

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
        kanAvvises: Boolean,
        totrinnsvurdering: Totrinnsvurdering?
    ) {
        this.hendelseId = hendelseId
        this.oppgaveId = id
        this.tilstand = mapTilstand(tilstand)
        this.type = egenskap.tilKafkaversjon()
        this.påVent = påVent
        this.saksbehandler = tildelt?.toMap()
        this.egenskaper = egenskaper.map { it.tilKafkaversjon() }
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
        this.beslutter = beslutter?.toMap()
    }

    private fun Saksbehandler.toMap() = mapOf(
        "epostadresse" to this.epostadresse(),
        "oid" to this.oid(),
    )

    private fun mapTilstand(tilstand: Oppgave.Tilstand): String {
        return when (tilstand) {
            Oppgave.AvventerSaksbehandler -> "AvventerSaksbehandler"
            Oppgave.AvventerSystem -> "AvventerSystem"
            Oppgave.Ferdigstilt -> "Ferdigstilt"
            Oppgave.Invalidert -> "Invalidert"
        }
    }
}
