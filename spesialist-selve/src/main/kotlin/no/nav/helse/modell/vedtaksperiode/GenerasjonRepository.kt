package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal interface GenerasjonRepository {
    fun opprettFørste(vedtaksperiodeId: UUID, hendelseId: UUID, id: UUID = UUID.randomUUID()): Generasjon?
    fun opprettNeste(
        id: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        skjæringstidspunkt: LocalDate?,
        periode: Periode?
    ): Generasjon
    fun låsFor(generasjonId: UUID, hendelseId: UUID)
    fun utbetalingFor(generasjonId: UUID, utbetalingId: UUID)
    fun sisteFor(vedtaksperiodeId: UUID): Generasjon
    fun tilhørendeFor(utbetalingId: UUID): List<Generasjon>
    fun fjernUtbetalingFor(generasjonId: UUID)
    fun finnÅpenGenerasjonFor(vedtaksperiodeId: UUID): Generasjon?
    fun oppdaterSykefraværstilfelle(id: UUID, skjæringstidspunkt: LocalDate, periode: Periode)
}

internal class ActualGenerasjonRepository(dataSource: DataSource) : GenerasjonRepository {

    private val dao = GenerasjonDao(dataSource)

    override fun opprettFørste(vedtaksperiodeId: UUID, hendelseId: UUID, id: UUID): Generasjon? {
        if (dao.finnSisteFor(vedtaksperiodeId) != null) {
            sikkerlogg.info(
                "Kan ikke opprette første generasjon for {} når det eksisterer generasjoner fra før av",
                keyValue("vedtaksperiodeId", vedtaksperiodeId)
            )
            return null
        }
        return dao.opprettFor(id, vedtaksperiodeId, hendelseId, null, null).also {
            it.loggFørsteOpprettet(vedtaksperiodeId)
        }
    }

    override fun opprettNeste(
        id: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        skjæringstidspunkt: LocalDate?,
        periode: Periode?
    ): Generasjon {
        return dao.opprettFor(id, vedtaksperiodeId, hendelseId, skjæringstidspunkt, periode).also {
            it.loggNesteOpprettet(vedtaksperiodeId)
        }
    }

    override fun sisteFor(vedtaksperiodeId: UUID) =
        dao.finnSisteFor(vedtaksperiodeId) ?: throw IllegalStateException("Forventer å finne en generasjon for perioden")

    override fun tilhørendeFor(utbetalingId: UUID): List<Generasjon> {
        return dao.alleFor(utbetalingId)
    }

    override fun låsFor(generasjonId: UUID, hendelseId: UUID) {
        dao.låsFor(generasjonId, hendelseId)
            ?.loggLåst()
            ?: sikkerlogg.error(
                "Finner ikke generasjon med {}. Forsøkt låst av {}",
                keyValue("generasjonId", generasjonId),
                keyValue("hendelseId", hendelseId)
            )
    }

    override fun utbetalingFor(generasjonId: UUID, utbetalingId: UUID) {
        dao.utbetalingFor(generasjonId, utbetalingId)
            ?.loggKnyttetUtbetaling(utbetalingId)
            ?: sikkerlogg.info(
                "Finner ikke ulåst generasjon for {}. Forsøkt knyttet til utbetaling {}",
                keyValue("generasjonId", generasjonId),
                keyValue("utbetalingId", utbetalingId)
            )
    }

    override fun fjernUtbetalingFor(generasjonId: UUID) {
        dao.fjernUtbetalingFor(generasjonId)
            ?.loggFjernetUtbetaling()
            ?: sikkerlogg.error(
                "Finner ikke generasjon med {}. Utbetaling forsøkt fjernet",
                keyValue("generasjonId", generasjonId)
            )
    }

    override fun finnÅpenGenerasjonFor(vedtaksperiodeId: UUID): Generasjon? {
        return dao.åpenGenerasjonForVedtaksperiode(vedtaksperiodeId)
    }

    override fun oppdaterSykefraværstilfelle(id: UUID, skjæringstidspunkt: LocalDate, periode: Periode) {
        return dao.oppdaterSykefraværstilfelle(id, skjæringstidspunkt, periode)
    }

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
        private fun Generasjon.loggFørsteOpprettet(vedtaksperiodeId: UUID) {
            sikkerlogg.info(
                "Oppretter første generasjon {} for {}",
                keyValue("generasjon", this),
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
            )
        }
        private fun Generasjon.loggNesteOpprettet(vedtaksperiodeId: UUID) {
            sikkerlogg.info(
                "Oppretter neste generasjon {} for {}",
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

        private fun Generasjon.loggFjernetUtbetaling() {
            sikkerlogg.info(
                "Fjernet knytning for {} til utbetaling",
                keyValue("generasjon", this),
            )
        }
    }
}
