package no.nav.helse.modell.varsel

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class Varsel(
    private val id: UUID,
    private val varselkode: String,
    private val opprettet: LocalDateTime,
    private val vedtaksperiodeId: UUID,
    private var status: Status = AKTIV
) {

    internal enum class Status {
        AKTIV,
        INAKTIV,
        GODKJENT,
        AVVIST
    }

    internal fun lagre(generasjon: Generasjon, varselRepository: VarselRepository) {
        generasjon.håndterNyttVarsel(id, varselkode, opprettet, varselRepository)
    }

    internal fun godkjennFor(generasjonId: UUID, ident: String, varselRepository: VarselRepository) {
        if (status !in listOf(AKTIV, INAKTIV)) return sikkerlogg.info(
            "Godkjenner ikke varsel med {}, {}, {} som ikke har status AKTIV eller INAKTIV",
            keyValue("varselkode", varselkode),
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("generasjonId", generasjonId)
        )
        status = GODKJENT
        varselRepository.godkjennFor(vedtaksperiodeId, generasjonId, varselkode, ident, null)
    }

    internal fun deaktiverFor(generasjonId: UUID, varselRepository: VarselRepository) {
        if (status != AKTIV) return sikkerlogg.info(
            "Deaktiverer ikke varsel med {}, {}, {} som ikke har status AKTIV",
            keyValue("varselkode", varselkode),
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("generasjonId", generasjonId)
        )
        status = INAKTIV
        varselRepository.deaktiverFor(vedtaksperiodeId, generasjonId, varselkode, null)
    }

    internal fun avvisFor(generasjonId: UUID, ident: String, varselRepository: VarselRepository) {
        if (status !in listOf(AKTIV, INAKTIV)) return sikkerlogg.info(
            "Avviser ikke varsel med {}, {}, {} som ikke har status AKTIV eller INAKTIV",
            keyValue("varselkode", varselkode),
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("generasjonId", generasjonId)
        )
        status = AVVIST
        varselRepository.avvisFor(vedtaksperiodeId, generasjonId, varselkode, ident, null)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Varsel
        && javaClass == other.javaClass
        && id == other.id
        && vedtaksperiodeId == other.vedtaksperiodeId
        && opprettet.withNano(0) == other.opprettet.withNano(0)
        && varselkode == other.varselkode)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + varselkode.hashCode()
        result = 31 * result + opprettet.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        return result
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun List<Varsel>.lagre(varselRepository: VarselRepository, generasjonRepository: GenerasjonRepository) {
            groupBy { it.vedtaksperiodeId }.forEach { (vedtaksperiodeId, varsler) ->
                try {
                    val generasjon = generasjonRepository.sisteFor(vedtaksperiodeId)
                    varsler.lagre(generasjon, varselRepository)
                } catch (e: IllegalStateException) {
                    sikkerlogg.info(
                        "Varsler for {} ble ikke lagret fordi det ikke finnes noen generasjon for perioden. Perioden er trolig forkastet",
                        keyValue("vedtaksperiodeId", vedtaksperiodeId)
                    )
                }
            }
        }

        private fun List<Varsel>.lagre(generasjon: Generasjon, varselRepository: VarselRepository) {
            forEach { it.lagre(generasjon, varselRepository) }
        }

        internal fun List<Varsel>.godkjennFor(generasjonId: UUID, varselkode: String, ident: String, varselRepository: VarselRepository) {
            find { it.varselkode == varselkode }?.godkjennFor(generasjonId, ident, varselRepository)
        }

        internal fun List<Varsel>.deaktiverFor(generasjonId: UUID, varselkode: String, varselRepository: VarselRepository) {
            find { it.varselkode == varselkode }?.deaktiverFor(generasjonId, varselRepository)
        }

        internal fun List<Varsel>.godkjennAlleFor(generasjonId: UUID, ident: String, varselRepository: VarselRepository) {
            forEach { it.godkjennFor(generasjonId, ident, varselRepository) }
        }

        internal fun List<Varsel>.avvisAlleFor(generasjonId: UUID, ident: String, varselRepository: VarselRepository) {
            forEach { it.avvisFor(generasjonId, ident, varselRepository) }
        }

        internal fun List<Varsel>.harVarsel(varselkode: String) = find { it.varselkode == varselkode } != null

        internal fun JsonNode.varsler(): List<Varsel> {
            return this
                .filter { it["nivå"].asText() == "VARSEL" && it["varselkode"]?.asText() != null }
                .filter { it["kontekster"].any { kontekst -> kontekst["konteksttype"].asText() == "Vedtaksperiode" } }
                .map { jsonNode ->
                    val vedtaksperiodeId =
                        UUID.fromString(
                            jsonNode["kontekster"]
                                .find { it["konteksttype"].asText() == "Vedtaksperiode" }!!["kontekstmap"]
                                .get("vedtaksperiodeId").asText()
                        )
                    Varsel(
                        UUID.fromString(jsonNode["id"].asText()),
                        jsonNode["varselkode"].asText(),
                        jsonNode["tidsstempel"].asLocalDateTime(),
                        vedtaksperiodeId
                    )
                }
        }
    }
}