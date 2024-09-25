package no.nav.helse.mediator.oppgave

import no.nav.helse.mediator.oppgave.OppgaveMapper.mapTilString
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.oppgave.OppgaveDto
import no.nav.helse.modell.oppgave.OppgaveObserver
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

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

private class OppgaveForKafkaBygger {
    fun bygg(oppgave: Oppgave): Oppgavemelding {
        val dto = oppgave.toDto()
        return Oppgavemelding(
            hendelseId = dto.hendelseId,
            oppgaveId = dto.id,
            tilstand = mapTilstand(dto.tilstand),
            beslutter = dto.totrinnsvurdering?.beslutter?.toMap(),
            saksbehandler = dto.tildeltTil?.toMap(),
            egenskaper = dto.egenskaper.map { it.mapTilString() },
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

    private fun SaksbehandlerDto.toMap() =
        mapOf(
            "epostadresse" to this.epostadresse,
            "oid" to this.oid,
        )

    private fun mapTilstand(tilstand: OppgaveDto.TilstandDto): String {
        return when (tilstand) {
            OppgaveDto.TilstandDto.AvventerSaksbehandler -> "AvventerSaksbehandler"
            OppgaveDto.TilstandDto.AvventerSystem -> "AvventerSystem"
            OppgaveDto.TilstandDto.Ferdigstilt -> "Ferdigstilt"
            OppgaveDto.TilstandDto.Invalidert -> "Invalidert"
        }
    }
}
