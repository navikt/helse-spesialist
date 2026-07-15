package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.SpesialistRiver
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import tools.jackson.databind.JsonNode
import java.util.UUID

object TestRapidHelpers {
    fun TestRapid.RapidInspector.meldinger() = (0 until size).map { index -> message(index) }

    fun TestRapid.RapidInspector.hendelser(type: String) = meldinger().filter { it.path("@event_name").asString() == type }

    fun TestRapid.RapidInspector.hendelser() = meldinger().map { it.path("@event_name").asString() }

    fun TestRapid.RapidInspector.hendelser(forårsaketAv: UUID) =
        meldinger()
            .filterNot { it.path("@event_name").isMissingOrNull() }
            .filter { it.path("@forårsaket_av").path("id").asString() == forårsaketAv.toString() }

    fun TestRapid.RapidInspector.hendelser(
        type: String,
        forårsaketAv: UUID,
    ) = meldinger()
        .filter { it.path("@event_name").asString() == type }
        .filter { it.path("@forårsaket_av").path("id").asString() == forårsaketAv.toString() }

    fun TestRapid.RapidInspector.siste(type: String) = hendelser(type).last()

    fun TestRapid.RapidInspector.behov() =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").toList().map(JsonNode::asString) }

    fun TestRapid.RapidInspector.behov(forårsaketAv: UUID) =
        hendelser("behov")
            .filter { it.path("@forårsaket_av").path("id").asString() == forårsaketAv.toString() }
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").toList().map(JsonNode::asString) }

    fun TestRapid.RapidInspector.behov(behov: String) =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .filter {
                it
                    .path("@behov")
                    .toList()
                    .map(JsonNode::asString)
                    .contains(behov)
            }

    fun TestRapid.RapidInspector.sisteBehov(vararg behov: String) =
        hendelser("behov")
            .last()
            .takeIf {
                it
                    .path("@behov")
                    .toList()
                    .map(JsonNode::asString)
                    .containsAll(behov.toList()) &&
                    !it.hasNonNull("@løsning")
            }

    fun TestRapid.RapidInspector.løsninger() =
        hendelser("behov")
            .filter { it.hasNonNull("@løsning") }

    fun TestRapid.RapidInspector.løsning(behov: String): JsonNode? =
        løsninger()
            .findLast {
                it
                    .path("@behov")
                    .toList()
                    .map(JsonNode::asString)
                    .contains(behov)
            }?.path("@løsning")
            ?.path(behov)

    fun TestRapid.RapidInspector.løsningOrNull(behov: String): JsonNode? =
        løsninger()
            .lastOrNull {
                it
                    .path("@behov")
                    .toList()
                    .map(JsonNode::asString)
                    .contains(behov)
            }?.path("@løsning")
            ?.path(behov)

    fun TestRapid.RapidInspector.contextId(): UUID =
        (
            hendelser("behov")
                .lastOrNull { it.hasNonNull("contextId") }
                ?: error("Prøver å finne contextId fra siste behov, men ingen behov er sendt ut")
        ).path("contextId")
            .asString()
            .let(UUID::fromString)

    fun TestRapid.RapidInspector.hendelseId(): UUID =
        (
            hendelser("behov")
                .lastOrNull { it.hasNonNull("hendelseId") }
                ?: error("Prøver å finne hendelseId fra siste behov, men ingen behov er sendt ut")
        ).path("hendelseId")
            .asString()
            .let(UUID::fromString)

    fun TestRapid.RapidInspector.oppgaveId() =
        hendelser("oppgave_opprettet")
            .last()
            .path("oppgaveId")
            .asLong()

    fun TestRapid.RapidInspector.contextId(hendelseId: UUID): UUID =
        hendelser("behov")
            .last { it.hasNonNull("contextId") && it.path("hendelseId").asString() == hendelseId.toString() }
            .path("contextId")
            .asString()
            .let(UUID::fromString)

    fun TestRapid.RapidInspector.oppgaveId(hendelseId: UUID): String =
        hendelser("oppgave_opprettet")
            .last { it.path("hendelseId").asString() == hendelseId.toString() }
            .path("oppgaveId")
            .asString()

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
                    type = oppgavemelding.first().path("type").asString(),
                    statuser = oppgavemelding.map { Oppgavestatus.valueOf(it.path("status").asString()) },
                )
            }
    }

    data class OppgaveSnapshot(
        val statuser: List<Oppgavestatus>,
        val type: String,
    )
}

fun TestRapid.medRivers(vararg river: SpesialistRiver): TestRapid {
    river.forEach {
        River(this)
            .precondition(it.preconditions())
            .validate(it.validations())
            .register(it)
    }
    return this
}
