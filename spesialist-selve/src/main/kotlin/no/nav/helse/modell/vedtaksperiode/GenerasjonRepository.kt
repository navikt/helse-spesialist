package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal interface GenerasjonRepository {
    fun opprettFørste(vedtaksperiodeId: UUID, hendelseId: UUID)
    fun forsøkOpprett(vedtaksperiodeId: UUID, hendelseId: UUID)
    fun låsFor(vedtaksperiodeId: UUID, hendelseId: UUID)
}

internal class ActualGenerasjonRepository(dataSource: DataSource) : GenerasjonRepository {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    private val dao = GenerasjonDao(dataSource)

    override fun opprettFørste(vedtaksperiodeId: UUID, hendelseId: UUID) {
        if (dao.finnSisteFor(vedtaksperiodeId) != null) {
            sikkerlogg.info(
                "Kan ikke opprette første generasjon for {} når det eksisterer generasjoner fra før av",
                keyValue("vedtaksperiodeId", vedtaksperiodeId)
            )
            return
        }
        dao.opprettFor(vedtaksperiodeId, hendelseId)
    }

    override fun forsøkOpprett(vedtaksperiodeId: UUID, hendelseId: UUID) {
        dao.finnSisteFor(vedtaksperiodeId)
            ?.forsøkOpprettNeste(hendelseId, dao::opprettFor)
            ?: sikkerlogg.info(
                """
                    Kan ikke opprette ny generasjon for {} fra vedtaksperiode_endret så lenge det ikke eksisterer minimum én generasjon fra før av. 
                    Første generasjon kan kun opprettes når vedtaksperioden opprettes.
                """,
                keyValue("vedtaksperiodeId", vedtaksperiodeId)
            )
    }

    override fun låsFor(vedtaksperiodeId: UUID, hendelseId: UUID) {
        dao.låsFor(vedtaksperiodeId, hendelseId) ?: sikkerlogg.error(
            "Fant ikke ulåst generasjon for {}. Forsøkt låst av {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("hendelseId", hendelseId)
        )
    }

}