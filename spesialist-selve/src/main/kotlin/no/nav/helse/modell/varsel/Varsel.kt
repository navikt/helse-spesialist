package no.nav.helse.modell.varsel

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varsel.Status.VURDERT
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
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
        VURDERT,
        AVVIST
    }

    private val observers = mutableSetOf<IVedtaksperiodeObserver>()

    internal fun registrer(vararg observer: IVedtaksperiodeObserver) {
        observers.addAll(observer)
    }

    internal fun erAktiv(): Boolean = this.status == AKTIV

    internal fun opprett(generasjonId: UUID) {
        observers.forEach { it.varselOpprettet(vedtaksperiodeId, generasjonId, id, varselkode, opprettet) }
    }

    internal fun godkjennFor(generasjonId: UUID, ident: String, varselRepository: VarselRepository) {
        if (status !in listOf(AKTIV, VURDERT)) return sikkerlogg.info(
            "Godkjenner ikke varsel med {}, {}, {} som ikke har status AKTIV eller VURDERT. Varselet har status=$status",
            keyValue("varselkode", varselkode),
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("generasjonId", generasjonId)
        )
        status = GODKJENT
        varselRepository.godkjennFor(vedtaksperiodeId, generasjonId, varselkode, ident, null)
    }

    internal fun avvisFor(generasjonId: UUID, ident: String, varselRepository: VarselRepository) {
        if (status != AKTIV) return sikkerlogg.info(
            "Avviser ikke varsel med {}, {}, {} som ikke har status $AKTIV. Varselet har status=$status",
            keyValue("varselkode", varselkode),
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("generasjonId", generasjonId)
        )
        status = AVVIST
        varselRepository.avvisFor(vedtaksperiodeId, generasjonId, varselkode, ident, null)
    }

    internal fun reaktiverFor(generasjonId: UUID, varselRepository: VarselRepository) {
        if (status != INAKTIV) return sikkerlogg.info(
            "Reaktiverer ikke varsel med {}, {}, {} som ikke har status $INAKTIV. Varselet har status=$status",
            keyValue("varselkode", varselkode),
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("generasjonId", generasjonId)
        )
        this.status = AKTIV
        varselRepository.reaktiverFor(vedtaksperiodeId, generasjonId, varselkode)
    }

    internal fun reaktiver(generasjonId: UUID) {
        if (status != INAKTIV) return
        this.status = AKTIV
        observers.forEach { it.varselReaktivert(id, varselkode, generasjonId, vedtaksperiodeId) }
    }

    internal fun deaktiver(generasjonId: UUID) {
        if(status != AKTIV) return
        this.status = INAKTIV
        observers.forEach { it.varselDeaktivert(id, varselkode, generasjonId, vedtaksperiodeId) }
    }

    private fun oppdaterGenerasjon(gammelGenerasjonId: UUID, nyGenerasjonId: UUID, varselRepository: VarselRepository) {
        varselRepository.oppdaterGenerasjonFor(this.id, gammelGenerasjonId, nyGenerasjonId)
    }

    override fun toString(): String {
        return "varselkode=$varselkode, vedtaksperiodeId=$vedtaksperiodeId, status=${status.name}"
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
        result = 31 * result + opprettet.withNano(0).hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        return result
    }

    internal fun erRelevantFor(vedtaksperiodeId: UUID): Boolean = this.vedtaksperiodeId == vedtaksperiodeId

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun List<Varsel>.flyttVarslerFor(gammelGenerasjonId: UUID, nyGenerasjonId: UUID, varselRepository: VarselRepository) {
            forEach { it.oppdaterGenerasjon(gammelGenerasjonId, nyGenerasjonId, varselRepository) }
        }

        internal fun List<Varsel>.godkjennFor(generasjonId: UUID, varselkode: String, ident: String, varselRepository: VarselRepository) {
            find { it.varselkode == varselkode }?.godkjennFor(generasjonId, ident, varselRepository)
        }

        internal fun List<Varsel>.reaktiverFor(generasjonId: UUID, varselkode: String, varselRepository: VarselRepository): Varsel? {
            return find { it.varselkode == varselkode }?.also { it.reaktiverFor(generasjonId, varselRepository) }
        }

        internal fun List<Varsel>.godkjennAlleFor(generasjonId: UUID, ident: String, varselRepository: VarselRepository) {
            forEach { it.godkjennFor(generasjonId, ident, varselRepository) }
        }

        internal fun List<Varsel>.avvisAlleFor(generasjonId: UUID, ident: String, varselRepository: VarselRepository) {
            forEach { it.avvisFor(generasjonId, ident, varselRepository) }
        }
        internal fun List<Varsel>.finnEksisterendeVarsel(varsel: Varsel): Varsel? {
            return find { it.varselkode == varsel.varselkode }
        }

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