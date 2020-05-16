package no.nav.helse.modell

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Behov(
    internal val typer: List<Behovtype>,
    internal val fødselsnummer: String,
    internal val orgnummer: String?,
    internal val spleisBehovId: UUID,
    internal val vedtaksperiodeId: UUID?
) {
    fun toJson() = JsonMessage.newMessage(mutableMapOf(
        "@event_name" to "behov",
        "@behov" to typer.map { it.name },
        "@id" to UUID.randomUUID(),
        "@opprettet" to LocalDateTime.now(),
        "spleisBehovId" to spleisBehovId,
        "fødselsnummer" to fødselsnummer
    ).apply {
        vedtaksperiodeId?.also { this["vedtaksperiodeId"] = it }
        orgnummer?.also { this["orgnummer"] = it }
    } + typer.flatMap { it.ekstrafelter.map { entry ->
        "${it.name}.${entry.key}" to entry.value
    }.toList()}).toJson()
}

internal sealed class Behovtype {
    internal open val ekstrafelter: Map<String, Any> = emptyMap()
    internal val name get() = this::class.simpleName!!

    internal object HentEnhet : Behovtype()
    internal object HentPersoninfo : Behovtype()
    internal object HentArbeidsgiverNavn : Behovtype()
    internal class HentInfotrygdutbetalinger : Behovtype() {
        private val nå = LocalDate.now()
        override val ekstrafelter: Map<String, Any> = mapOf(
            "historikkFom" to nå.minusYears(3),
            "historikkTom" to nå
        )
    }
}
