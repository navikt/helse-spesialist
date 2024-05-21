package no.nav.helse.mediator.oppgave

import no.nav.helse.mediator.oppgave.OppgaveMapper.tilKafkaversjon
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveObserver
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDateTime
import java.util.UUID
import kotlin.properties.Delegates

internal class Oppgavemelder(
    private val meldingDao: MeldingDao,
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

    private fun melding(
        eventName: String,
        oppgavemelding: OppgaveForKafkaBygger.Oppgavemelding,
    ): Pair<String, JsonMessage> {
        val fødselsnummer: String = meldingDao.finnFødselsnummer(oppgavemelding.hendelseId)
        return fødselsnummer to
            JsonMessage.newMessage(
                eventName,
                mutableMapOf(
                    "@forårsaket_av" to mapOf("id" to oppgavemelding.hendelseId),
                    "hendelseId" to oppgavemelding.hendelseId,
                    "oppgaveId" to oppgavemelding.oppgaveId,
                    "tilstand" to oppgavemelding.tilstand,
                    "fødselsnummer" to fødselsnummer,
                    "egenskaper" to oppgavemelding.egenskaper,
                ).apply {
                    compute("beslutter") { _, _ -> oppgavemelding.beslutter }
                    compute("saksbehandler") { _, _ -> oppgavemelding.saksbehandler }
                },
            )
    }
}

private class OppgaveForKafkaBygger : OppgaveVisitor {
    private var beslutter: Map<String, Any>? = null
    private var saksbehandler: Map<String, Any>? = null
    private lateinit var hendelseId: UUID
    private var oppgaveId by Delegates.notNull<Long>()
    private lateinit var tilstand: String
    private lateinit var egenskaper: List<String>

    fun bygg(oppgave: Oppgave): Oppgavemelding {
        oppgave.accept(this)
        return Oppgavemelding(
            hendelseId = hendelseId,
            oppgaveId = oppgaveId,
            tilstand = tilstand,
            beslutter = beslutter,
            saksbehandler = saksbehandler,
            egenskaper = egenskaper,
        )
    }

    data class Oppgavemelding(
        val hendelseId: UUID,
        val oppgaveId: Long,
        val tilstand: String,
        val beslutter: Map<String, Any>?,
        val saksbehandler: Map<String, Any>?,
        val egenskaper: List<String>,
    )

    override fun visitOppgave(
        id: Long,
        tilstand: Oppgave.Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Egenskap>,
        tildelt: Saksbehandler?,
        kanAvvises: Boolean,
        totrinnsvurdering: Totrinnsvurdering?,
    ) {
        this.hendelseId = hendelseId
        this.oppgaveId = id
        this.tilstand = mapTilstand(tilstand)
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
        oppdatert: LocalDateTime?,
    ) {
        this.beslutter = beslutter?.toMap()
    }

    private fun Saksbehandler.toMap() =
        mapOf(
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
