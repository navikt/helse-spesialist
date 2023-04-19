package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.builders.GenerasjonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal interface GenerasjonRepository {
    fun sisteForLenient(vedtaksperiodeId: UUID): Generasjon? = null
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
        opprettFørste(vedtaksperiodeId, hendelseId, generasjonId, fom, tom, skjæringstidspunkt, tilstand)
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
        opprettNeste(generasjonId, vedtaksperiodeId, hendelseId, skjæringstidspunkt, Periode(fom, tom), tilstand)
    }

    override fun nyUtbetaling(generasjonId: UUID, utbetalingId: UUID) {
        utbetalingFor(generasjonId = generasjonId, utbetalingId = utbetalingId)
    }

    override fun utbetalingForkastet(generasjonId: UUID, utbetalingId: UUID) {
        fjernUtbetalingFor(generasjonId)
    }

    override fun vedtakFattet(generasjonId: UUID, hendelseId: UUID) {
        låsFor(generasjonId, hendelseId)
    }

    private fun opprettFørste(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        id: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        tilstand: Generasjon.Tilstand
    ): Generasjon? {
        if (dao.finnSisteFor(vedtaksperiodeId) != null) {
            sikkerlogg.info(
                "Kan ikke opprette første generasjon for {} når det eksisterer generasjoner fra før av",
                keyValue("vedtaksperiodeId", vedtaksperiodeId)
            )
            return null
        }
        return dao.opprettFor(id, vedtaksperiodeId, hendelseId, skjæringstidspunkt, Periode(fom, tom), tilstand).also {
            it.loggFørsteOpprettet(vedtaksperiodeId)
            it.registrer(this)
        }
    }

    override fun sisteForLenient(vedtaksperiodeId: UUID): Generasjon? {
        return dao.finnSisteFor(vedtaksperiodeId)?.also { it.registrer(this) }
    }

    override fun tilhørendeFor(utbetalingId: UUID): List<Generasjon> {
        return dao.alleFor(utbetalingId).onEach { it.registrer(this) }
    }

    override fun tilstandEndret(
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        gammel: Generasjon.Tilstand,
        ny: Generasjon.Tilstand
    ) {
        dao.oppdaterTilstandFor(generasjonId, ny)
    }

    private fun låsFor(generasjonId: UUID, hendelseId: UUID) {
        dao.låsFor(generasjonId, hendelseId)
            ?.loggLåst()
            ?: sikkerlogg.error(
                "Finner ikke generasjon med {}. Forsøkt låst av {}",
                keyValue("generasjonId", generasjonId),
                keyValue("hendelseId", hendelseId)
            )
    }

    private fun utbetalingFor(generasjonId: UUID, utbetalingId: UUID) {
        dao.utbetalingFor(generasjonId, utbetalingId)
            ?.loggKnyttetUtbetaling(utbetalingId)
            ?: sikkerlogg.info(
                "Finner ikke ulåst generasjon for {}. Forsøkt knyttet til utbetaling {}",
                keyValue("generasjonId", generasjonId),
                keyValue("utbetalingId", utbetalingId)
            )
    }

    private fun fjernUtbetalingFor(generasjonId: UUID) {
        dao.fjernUtbetalingFor(generasjonId)
            ?.loggFjernetUtbetaling()
            ?: sikkerlogg.error(
                "Finner ikke generasjon med {}. Utbetaling forsøkt fjernet",
                keyValue("generasjonId", generasjonId)
            )
    }

    private fun opprettNeste(
        id: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        skjæringstidspunkt: LocalDate,
        periode: Periode,
        tilstand: Generasjon.Tilstand
    ) {
        dao.opprettFor(id, vedtaksperiodeId, hendelseId, skjæringstidspunkt, periode, tilstand).also {
            it.loggNesteOpprettet(vedtaksperiodeId)
        }
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
