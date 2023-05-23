package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.builders.GenerasjonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class ActualGenerasjonRepository(dataSource: DataSource): IVedtaksperiodeObserver {

    private val dao = GenerasjonDao(dataSource)

    internal fun byggGenerasjon(vedtaksperiodeId: UUID, generasjonBuilder: GenerasjonBuilder) {
        dao.byggSisteFor(vedtaksperiodeId, generasjonBuilder)
    }

    internal fun finnVedtaksperiodeIderFor(fødselsnummer: String, skjæringstidspunkt: LocalDate): Set<UUID> {
        return dao.finnVedtaksperiodeIderFor(fødselsnummer, skjæringstidspunkt)
    }

    internal fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID> {
        return dao.finnVedtaksperiodeIderFor(fødselsnummer)
    }

    internal fun finnVedtaksperiodeIderFor(utbetalingId: UUID): Set<UUID> {
        return dao.finnVedtaksperiodeIderFor(utbetalingId)
    }

    internal fun skjæringstidspunktFor(vedtaksperiodeId: UUID): LocalDate {
        return dao.finnSkjæringstidspunktFor(vedtaksperiodeId)
            ?: throw IllegalStateException("Forventer å finne skjæringstidspunkt for vedtaksperiodeId=$vedtaksperiodeId")
    }

    override fun førsteGenerasjonOpprettet(
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        tilstand: Generasjon.Tilstand
    ) {
        if (dao.harGenerasjonFor(vedtaksperiodeId)) {
            return sikkerlogg.info(
                "Kan ikke opprette første generasjon for {} når det eksisterer generasjoner fra før av",
                kv("vedtaksperiodeId", vedtaksperiodeId)
            )
        }
        dao.opprettFor(generasjonId, vedtaksperiodeId, hendelseId, skjæringstidspunkt, Periode(fom, tom), tilstand)
            .also {
                it.loggFørsteOpprettet(vedtaksperiodeId)
            }
    }

    override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
        dao.oppdaterSykefraværstilfelle(generasjonId, skjæringstidspunkt, Periode(fom, tom))
    }

    override fun generasjonOpprettet(
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        tilstand: Generasjon.Tilstand
    ) {
        dao.opprettFor(generasjonId, vedtaksperiodeId, hendelseId, skjæringstidspunkt, Periode(fom, tom), tilstand)
            .also {
                it.loggNesteOpprettet(vedtaksperiodeId)
            }
    }

    override fun nyUtbetaling(generasjonId: UUID, utbetalingId: UUID) {
        dao.utbetalingFor(generasjonId = generasjonId, utbetalingId = utbetalingId)
    }

    override fun utbetalingForkastet(generasjonId: UUID, utbetalingId: UUID) {
        fjernUtbetalingFor(generasjonId)
    }

    override fun tilstandEndret(
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        gammel: Generasjon.Tilstand,
        ny: Generasjon.Tilstand,
        hendelseId: UUID
    ) {
        dao.oppdaterTilstandFor(generasjonId, ny, hendelseId)
    }

    private fun fjernUtbetalingFor(generasjonId: UUID) {
        dao.fjernUtbetalingFor(generasjonId)
            ?.loggFjernetUtbetaling()
            ?: sikkerlogg.error(
                "Finner ikke generasjon med {}. Utbetaling forsøkt fjernet",
                kv("generasjonId", generasjonId)
            )
    }

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
        private fun Generasjon.loggFørsteOpprettet(vedtaksperiodeId: UUID) {
            sikkerlogg.info(
                "Oppretter første generasjon {} for {}",
                kv("generasjon", this),
                kv("vedtaksperiodeId", vedtaksperiodeId),
            )
        }

        private fun Generasjon.loggNesteOpprettet(vedtaksperiodeId: UUID) {
            sikkerlogg.info(
                "Oppretter neste generasjon {} for {}",
                kv("generasjon", this),
                kv("vedtaksperiodeId", vedtaksperiodeId),
            )
        }

        private fun Generasjon.loggFjernetUtbetaling() {
            sikkerlogg.info(
                "Fjernet knytning for {} til utbetaling",
                kv("generasjon", this),
            )
        }
    }
}
