package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselStatusDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class ActualGenerasjonRepository(private val dataSource: DataSource): IVedtaksperiodeObserver {

    private val dao = GenerasjonDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)

    internal fun brukVedtaksperiode(vedtaksperiodeId: UUID, block: (vedtaksperiode: Vedtaksperiode) -> Unit) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val vedtaksperiode = tx.finnVedtaksperiode(vedtaksperiodeId)
                block(vedtaksperiode)
                tx.lagreVedtaksperiode(vedtaksperiode)
            }
        }
    }

    private fun TransactionalSession.finnVedtaksperiode(vedtaksperiodeId: UUID): Vedtaksperiode {
        return with(vedtakDao) {
            finnVedtaksperiode(vedtaksperiodeId)
                ?.copy(generasjoner = finnGenerasjoner(vedtaksperiodeId))
                ?.let { Vedtaksperiode.gjenopprett(it.vedtaksperiodeId, it.generasjoner) }
                ?: throw IllegalStateException("Forventer å finne vedtaksperiode for vedtaksperiodeId=$vedtaksperiodeId")
        }
    }

    private fun TransactionalSession.finnGenerasjoner(vedtaksperiodeId: UUID): List<GenerasjonDto> {
        return with(dao) {
            finnGenerasjoner(vedtaksperiodeId)
        }
    }

    private fun TransactionalSession.lagreVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        val dto = vedtaksperiode.toDto()
        with(dao) {
            dto.generasjoner.forEach { generasjonDto ->
                lagreGenerasjon(generasjonDto)
            }
        }
    }

    internal fun brukGenerasjon(vedtaksperiodeId: UUID, block: (generasjon: Generasjon) -> Unit) {
        val generasjon = dao.finnGjeldendeGenerasjon(vedtaksperiodeId)?.tilGenerasjon()
            ?: throw IllegalStateException("Forventer å finne en generasjon for vedtaksperiodeId=$vedtaksperiodeId")
        block(generasjon)
        val generasjonForLagring = generasjon.toDto()
        dao.lagre(generasjonForLagring)
    }

    internal fun brukGenerasjonHvisFinnes(vedtaksperiodeId: UUID, block: (generasjon: Generasjon) -> Unit) {
        val generasjon = dao.finnGjeldendeGenerasjon(vedtaksperiodeId)?.tilGenerasjon()
            ?: return sikkerlogg.warn("Generasjon for vedtaksperiodeId=$vedtaksperiodeId finnes ikke, returnerer tidlig ved forsøk på å utføre noe i kontekst av generasjon")
        block(generasjon)
        val generasjonForLagring = generasjon.toDto()
        dao.lagre(generasjonForLagring)
    }

    internal fun byggGenerasjon(vedtaksperiodeId: UUID, generasjonBuilder: GenerasjonBuilder) {
        dao.byggSisteFor(vedtaksperiodeId, generasjonBuilder)
    }

    internal fun finnVedtaksperiodeIderFor(fødselsnummer: String, skjæringstidspunkt: LocalDate): Set<UUID> {
        return dao.finnVedtaksperiodeIderFor(fødselsnummer, skjæringstidspunkt)
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

    internal fun førsteKjenteDag(fødselsnummer: String) = dao.førsteKjenteDag(fødselsnummer)

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

    private fun GenerasjonDto.tilGenerasjon(): Generasjon {
        return Generasjon.fraLagring(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = fom,
            tom = tom,
            tilstand = when (tilstand) {
                TilstandDto.Låst -> Generasjon.Låst
                TilstandDto.Ulåst -> Generasjon.Ulåst
                TilstandDto.AvsluttetUtenUtbetaling -> Generasjon.AvsluttetUtenUtbetaling
                TilstandDto.UtenUtbetalingMåVurderes -> Generasjon.UtenUtbetalingMåVurderes
            },
            varsler = varsler.map { varselDto ->
                Varsel(
                    id = varselDto.id,
                    varselkode = varselDto.varselkode,
                    opprettet = varselDto.opprettet,
                    vedtaksperiodeId = varselDto.vedtaksperiodeId,
                    status = when (varselDto.status) {
                        VarselStatusDto.AKTIV -> Varsel.Status.AKTIV
                        VarselStatusDto.INAKTIV -> Varsel.Status.INAKTIV
                        VarselStatusDto.GODKJENT -> Varsel.Status.GODKJENT
                        VarselStatusDto.VURDERT -> Varsel.Status.VURDERT
                        VarselStatusDto.AVVIST -> Varsel.Status.AVVIST
                        VarselStatusDto.AVVIKLET -> Varsel.Status.AVVIKLET
                    }
                )
            }.toSet()
        )
    }
}
