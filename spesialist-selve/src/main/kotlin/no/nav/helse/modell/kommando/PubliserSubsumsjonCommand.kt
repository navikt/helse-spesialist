package no.nav.helse.modell.kommando

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.modell.overstyring.SkjønnsfastsattArbeidsgiver
import no.nav.helse.objectMapper

internal class PubliserSubsumsjonCommand(
    private val fødselsnummer: String,
    private val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
    private val overstyringMediator: OverstyringMediator,
    private val versjonAvKode: String?
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val id = UUID.randomUUID().toString()
        val packet: ObjectNode = objectMapper.createObjectNode()

        // Hva med å legge til "skjonnsfastsettingHendelse" med hendelseId'en til selve skjønnsfastsettelsen innunder sporing?
        packet.put("@event_name", "subsumsjon")
        packet.put("@id", id)
        packet.replace("@opprettet", objectMapper.valueToTree(LocalDateTime.now()))
        packet.putIfAbsent("subsumsjon", objectMapper.valueToTree(mapOf(
            "id" to id,
            "versjon" to "1.0.0",
            "kilde" to "spesialist",
            "versjonAvKode" to versjonAvKode,
            "fodselsnummer" to fødselsnummer,
            "sporing" to mapOf(
                "vedtaksperiode" to arbeidsgivere.mapNotNull { ag -> ag.initierendeVedtaksperiodeId?.toString() }.toList(),
                "organisasjonsnummer" to arbeidsgivere.map { ag -> ag.organisasjonsnummer }.toList()
            ),
            "tidsstempel" to "${LocalDateTime.now()}",
            "lovverk" to "folketrygdloven",
            "lovverksversjon" to "2023-03-15",
            "paragraf" to "${arbeidsgivere.first().subsumsjon?.paragraf}",
            "ledd" to "${arbeidsgivere.first().subsumsjon?.ledd}",
            "bokstav" to "${arbeidsgivere.first().subsumsjon?.bokstav}",
            "input" to mapOf(
                "sattÅrligInntektPerArbeidsgiver" to arbeidsgivere.map { ag -> mapOf(ag.organisasjonsnummer to ag.årlig) },
            ),
            "output" to mapOf(
                "grunnlagForSykepengegrunnlag" to arbeidsgivere.sumOf { ag -> ag.årlig }
            ),
            "utfall" to "VILKÅR_BEREGNET"
        )))
        overstyringMediator.sendSubsumsjon(packet)
        return true
    }
}
