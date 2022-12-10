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
    fun utbetalingFor(vedtaksperiodeId: UUID, utbetalingId: UUID)
    fun sisteFor(vedtaksperiodeId: UUID): Generasjon
}

internal class ActualGenerasjonRepository(dataSource: DataSource) : GenerasjonRepository {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
        private fun Generasjon.loggOpprettet(vedtaksperiodeId: UUID) {
            sikkerlogg.info(
                "Oppretter første generasjon {} for {}",
                keyValue("generasjon", this),
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
            )
        }

        private fun Generasjon.loggLåst() {
            sikkerlogg.info("Låser generasjon {}", keyValue("generasjon", this))
        }

        private fun Generasjon.loggKnyttetUtbetaling(utbetalingId: UUID) {
            sikkerlogg.info(
                "Knyttet {} til utbetaling {}",
                keyValue("generasjon", this),
                keyValue("utbetalingId", utbetalingId)
            )
        }
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
            .loggOpprettet(vedtaksperiodeId)
    }

    override fun forsøkOpprett(vedtaksperiodeId: UUID, hendelseId: UUID) {
        val generasjon = dao.finnSisteFor(vedtaksperiodeId)
        if (generasjon == null) {
            sikkerlogg.info(
                """
                Kan ikke opprette ny generasjon for {} fra vedtaksperiode_endret så lenge det ikke eksisterer minimum én generasjon fra før av. 
                Første generasjon kan kun opprettes når vedtaksperioden opprettes.
                """.trimIndent(),
                keyValue("vedtaksperiodeId", vedtaksperiodeId)
            )
            return
        }
        generasjon
            .forsøkOpprettNeste(hendelseId, dao::opprettFor)
            ?.loggOpprettet(vedtaksperiodeId)
    }

    override fun sisteFor(vedtaksperiodeId: UUID) =
        dao.finnSisteFor(vedtaksperiodeId) ?: throw IllegalStateException("Forventer å finne en generasjon for perioden")

    override fun låsFor(vedtaksperiodeId: UUID, hendelseId: UUID) {
        dao.finnSisteFor(vedtaksperiodeId) ?: return
        dao.låsFor(vedtaksperiodeId, hendelseId)
            ?.loggLåst()
            ?: sikkerlogg.error(
                "Finner ikke ulåst generasjon for {}. Forsøkt låst av {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("hendelseId", hendelseId)
            )
    }

    override fun utbetalingFor(vedtaksperiodeId: UUID, utbetalingId: UUID) {
        dao.utbetalingFor(vedtaksperiodeId, utbetalingId)
            ?.loggKnyttetUtbetaling(utbetalingId)
            ?: sikkerlogg.info(
                "Finner ikke ulåst generasjon for {}. Forsøkt knyttet til utbetaling {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("utbetalingId", utbetalingId)
            )
    }

}