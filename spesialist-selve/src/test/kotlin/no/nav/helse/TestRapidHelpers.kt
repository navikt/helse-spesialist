package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import java.util.UUID

object TestRapidHelpers {
    fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    fun TestRapid.RapidInspector.hendelser() =
        meldinger().map { it.path("@event_name").asText() }

    fun TestRapid.RapidInspector.hendelser(forårsaketAv: UUID) =
        meldinger()
            .filterNot { it.path("@event_name").isMissingOrNull() }
            .filter { it.path("@forårsaket_av").path("id").asText() == forårsaketAv.toString() }

    fun TestRapid.RapidInspector.hendelser(type: String, forårsaketAv: UUID) =
        meldinger()
            .filter { it.path("@event_name").asText() == type }
            .filter { it.path("@forårsaket_av").path("id").asText() == forårsaketAv.toString() }

    fun TestRapid.RapidInspector.siste(type: String) =
        hendelser(type).last()

    fun TestRapid.RapidInspector.behov() =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").map(JsonNode::asText) }

    fun TestRapid.RapidInspector.behov(forårsaketAv: UUID) =
        hendelser("behov")
            .filter { it.path("@forårsaket_av").path("id").asText() == forårsaketAv.toString() }
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").map(JsonNode::asText) }

    fun TestRapid.RapidInspector.behov(behov: String) =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .filter { it.path("@behov").map(JsonNode::asText).contains(behov) }

    fun TestRapid.RapidInspector.sisteBehov(vararg behov: String) =
        hendelser("behov")
            .last()
            .takeIf { it.path("@behov").map(JsonNode::asText).containsAll(behov.toList()) && !it.hasNonNull("@løsning") }

    fun TestRapid.RapidInspector.løsninger() =
        hendelser("behov")
            .filter { it.hasNonNull("@løsning") }

    fun TestRapid.RapidInspector.løsning(behov: String): JsonNode? =
        løsninger()
            .findLast { it.path("@behov").map(JsonNode::asText).contains(behov) }
            ?.path("@løsning")
            ?.path(behov)

    fun TestRapid.RapidInspector.løsningOrNull(behov: String): JsonNode? =
        løsninger()
            .lastOrNull { it.path("@behov").map(JsonNode::asText).contains(behov) }
            ?.path("@løsning")
            ?.path(behov)

    fun TestRapid.RapidInspector.contextId(): UUID =
        (hendelser("behov")
            .lastOrNull { it.hasNonNull("contextId") }
            ?: error("Prøver å finne contextId fra siste behov, men ingen behov er sendt ut"))
            .path("contextId")
            .asText()
            .let(UUID::fromString)

    fun TestRapid.RapidInspector.hendelseId(): UUID =
        (hendelser("behov")
            .lastOrNull { it.hasNonNull("hendelseId") }
            ?: error("Prøver å finne hendelseId fra siste behov, men ingen behov er sendt ut"))
            .path("hendelseId")
            .asText()
            .let(UUID::fromString)

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
            .let(UUID::fromString)

    fun TestRapid.RapidInspector.oppgaveId(hendelseId: UUID): String =
        hendelser("oppgave_opprettet")
            .last { it.path("hendelseId").asText() == hendelseId.toString() }
            .path("oppgaveId")
            .asText()

    fun TestRapid.RapidInspector.oppgaver(): Map<Int, OppgaveSnapshot> {
        val oppgaveider = mutableListOf<Long>()
        val oppgavemeldinger = mutableMapOf<Int, MutableList<JsonNode>>()
        hendelser("oppgave_opprettet")
            .forEach {
                oppgaveider.add(it.path("oppgaveId").asLong())
                oppgavemeldinger[oppgaveider.size - 1] = mutableListOf(it)
            }
        hendelser("oppgave_oppdatert")
            .forEach { oppgave ->
                val indeks = oppgaveider.indexOf(oppgave.path("oppgaveId").asLong())
                oppgavemeldinger[indeks]?.add(oppgave)
            }
        return oppgavemeldinger
            .mapValues { (_, oppgavemelding) ->
                OppgaveSnapshot(
                    type = oppgavemelding.first().path("type").asText(),
                    statuser = oppgavemelding.map { Oppgavestatus.valueOf(it.path("status").asText()) }
                )
            }
    }

    data class OppgaveSnapshot(
        val statuser: List<Oppgavestatus>,
        val type: String
    )

}

internal fun TestRapid.medRivers(vararg river: SpesialistRiver): TestRapid {
    river.forEach { River(this).validate(it.validations()).register(it) }
    return this
}
