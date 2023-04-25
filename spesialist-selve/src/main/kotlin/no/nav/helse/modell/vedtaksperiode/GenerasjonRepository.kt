package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.builders.GenerasjonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal interface GenerasjonRepository {
    fun tilhørendeFor(utbetalingId: UUID): List<Generasjon>
}

internal class ActualGenerasjonRepository(dataSource: DataSource) : GenerasjonRepository, IVedtaksperiodeObserver {

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
            ?.loggKnyttetUtbetaling(utbetalingId = utbetalingId)
            ?: sikkerlogg.info(
                "Finner ikke ulåst generasjon for {}. Forsøkt knyttet til utbetaling {}",
                kv("generasjonId", generasjonId),
                kv("utbetalingId", utbetalingId)
            )
    }

    override fun utbetalingForkastet(generasjonId: UUID, utbetalingId: UUID) {
        fjernUtbetalingFor(generasjonId)
    }

    override fun vedtakFattet(generasjonId: UUID, hendelseId: UUID) {
        dao.låsFor(generasjonId, hendelseId)
            ?.loggLåst()
            ?: sikkerlogg.error(
                "Finner ikke generasjon med {}. Forsøkt låst av {}",
                kv("generasjonId", generasjonId),
                kv("hendelseId", hendelseId)
            )
    }

    override fun tilhørendeFor(utbetalingId: UUID): List<Generasjon> {
        return dao.alleFor(utbetalingId).onEach { it.registrer(this) }
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

        private fun Generasjon.loggLåst() {
            sikkerlogg.info("Låser generasjon {}", kv("generasjon", this))
        }

        private fun Generasjon.loggKnyttetUtbetaling(utbetalingId: UUID) {
            sikkerlogg.info(
                "Knyttet {} til utbetaling {}",
                kv("generasjon", this),
                kv("utbetalingId", utbetalingId)
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
