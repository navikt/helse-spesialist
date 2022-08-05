package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.oppgave.Oppgavestatus
import no.nav.helse.rapids_rivers.testsupport.TestRapid

object TestRapidHelpers {
    fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    fun TestRapid.RapidInspector.siste(type: String) =
        hendelser(type).last()

    fun TestRapid.RapidInspector.behov() =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").map(JsonNode::asText) }

    fun TestRapid.RapidInspector.løsninger() =
        hendelser("behov")
            .filter { it.hasNonNull("@løsning") }

    fun TestRapid.RapidInspector.løsning(behov: String): JsonNode =
        løsninger()
            .last { it.path("@behov").map(JsonNode::asText).contains(behov) }
            .path("@løsning").path(behov)

    fun TestRapid.RapidInspector.contextId(): UUID =
        (hendelser("behov")
            .lastOrNull { it.hasNonNull("contextId") }
            ?: error("Prøver å finne contextId fra siste behov, men ingen behov er sendt ut"))
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    fun TestRapid.RapidInspector.oppgaveId() =
        hendelser("oppgave_opprettet")
            .last()
            .path("oppgaveId")
            .asLong()

    fun TestRapid.RapidInspector.contextId(hendelseId: UUID): UUID =
        hendelser("behov")
            .last { it.hasNonNull("contextId") && it.path("hendelseId").asText() == hendelseId.toString() }
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    fun TestRapid.RapidInspector.oppgaveId(hendelseId: UUID): String =
        hendelser("oppgave_opprettet")
            .last { it.path("hendelseId").asText() == hendelseId.toString() }
            .path("oppgaveId")
            .asText()

    fun TestRapid.RapidInspector.oppgaver(): Map<Int, OppgaveSnapshot> {
        val oppgaveindekser = mutableListOf<Long>()
        val oppgaver = mutableMapOf<Int, MutableList<JsonNode>>()
        hendelser("oppgave_opprettet")
            .forEach {
                oppgaveindekser.add(it.path("oppgaveId").asLong())
                oppgaver[oppgaveindekser.size - 1] = mutableListOf(it)
            }
        hendelser("oppgave_oppdatert")
            .forEach { oppgave ->
                val indeks = oppgaveindekser.indexOf(oppgave.path("oppgaveId").asLong())
                oppgaver[indeks]?.add(oppgave)
            }
        return oppgaver
            .mapValues { (_, oppgaver) ->
                OppgaveSnapshot(
                    type = oppgaver.first().path("type").asText(),
                    statuser = oppgaver.map { Oppgavestatus.valueOf(it.path("status").asText()) }
                )
            }
    }

    data class OppgaveSnapshot(
        val statuser: List<Oppgavestatus>,
        val type: String
    )

}
