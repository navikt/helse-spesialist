package no.nav.helse.spesialist.api.oppgave

import java.util.UUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

class Oppgavemelder(private val oppgaveApiDao: OppgaveApiDao, private val rapidsConnection: RapidsConnection) {

    fun sendOppgaveOppdatertMelding(oppgaveId: Long) = lagOppgaveOppdatertMelding(oppgaveId).also { (key, message) ->
        rapidsConnection.publish(key, message.toJson())
    }

    private fun lagOppgaveOppdatertMelding(oppgaveId: Long): Pair<String, JsonMessage> {
        val oppgavemelding: Oppgavemelding = requireNotNull(oppgaveApiDao.hentOppgavemelding(oppgaveId))
        val fødselsnummer: String = oppgaveApiDao.finnFødselsnummer(oppgaveId)

        return fødselsnummer to JsonMessage.newMessage("oppgave_oppdatert", mutableMapOf(
            "@forårsaket_av" to mapOf("id" to oppgavemelding.hendelseId),
            "hendelseId" to oppgavemelding.hendelseId,
            "oppgaveId" to oppgavemelding.oppgaveId,
            "status" to oppgavemelding.status.name,
            "type" to oppgavemelding.type.name,
            "fødselsnummer" to fødselsnummer,
            "erBeslutterOppgave" to (oppgavemelding.beslutter != null),
            "erReturOppgave" to oppgavemelding.erRetur,
        ).apply {
            oppgavemelding.ferdigstiltAvIdent?.also { put("ferdigstiltAvIdent", it) }
            oppgavemelding.ferdigstiltAvOid?.also { put("ferdigstiltAvOid", it) }
        })
    }

    data class Oppgavemelding(
        val hendelseId: UUID,
        val oppgaveId: Long,
        val status: Oppgavestatus,
        val type: Oppgavetype,
        val beslutter: UUID?,
        val erRetur: Boolean,
        val ferdigstiltAvIdent: String? = null,
        val ferdigstiltAvOid: UUID? = null,
    )
}
