package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.varsel.varselkodeformat
import no.nav.helse.tellVarsel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Generasjon(
    private val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val låst: Boolean,
) {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun forsøkOpprettNeste(
        hendelseId: UUID,
        opprettBlock: (vedtaksperiodeId: UUID, hendelseId: UUID) -> Generasjon,
    ): Generasjon? {
        if (!låst) {
            sikkerlogg.info(
                "Oppretter ikke ny generasjon for {} da nåværende generasjon med {} er ulåst",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("id", id)
            )
            return null
        }
        return opprettBlock(vedtaksperiodeId, hendelseId)
    }

    internal fun lagreVarsel(
        varselId: UUID,
        varselkode: String,
        opprettet: LocalDateTime,
        opprettBlock: (varselId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID, generasjonId: UUID) -> Unit,
    ) {
        if (låst) return sikkerlogg.info(
            "Kan ikke lagre varsel {} på låst generasjon {}",
            keyValue("varselId", varselId),
            keyValue("generasjon", this)
        )
        opprettBlock(varselId, varselkode, opprettet, vedtaksperiodeId, id)
        if (varselkode.matches(varselkodeformat.toRegex())) tellVarsel(varselkode)
    }

    override fun toString(): String = "generasjonId=$id, vedtaksperiodeId=$vedtaksperiodeId, låst=$låst"

    override fun equals(other: Any?): Boolean =
        this === other || (other is Generasjon
                && javaClass == other.javaClass
                && id == other.id
                && vedtaksperiodeId == other.vedtaksperiodeId
                && låst == other.låst)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + låst.hashCode()
        return result
    }

}