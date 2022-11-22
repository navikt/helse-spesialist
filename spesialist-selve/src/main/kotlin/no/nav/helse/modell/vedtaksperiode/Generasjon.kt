package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
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
        opprettBlock: (vedtaksperiodeId: UUID, hendelseId: UUID) -> Unit,
    ) {
        if (!låst) {
            sikkerlogg.info(
                "Oppretter ikke ny generasjon for {} da nåværende generasjon med {} er ulåst",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("id", id)
            )
            return
        }
        opprettBlock(vedtaksperiodeId, hendelseId)
    }

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