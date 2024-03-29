package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselStatusDto

internal class GenerasjonRepository(private val dataSource: DataSource): IVedtaksperiodeObserver {

    private val dao = GenerasjonDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)

    internal fun brukVedtaksperiode(fødselsnummer: String, vedtaksperiodeId: UUID, block: (vedtaksperiode: Vedtaksperiode) -> Unit) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val vedtaksperiode = tx.finnVedtaksperiode(vedtaksperiodeId).let {
                    Vedtaksperiode.gjenopprett(it.organisasjonsnummer, it.vedtaksperiodeId, it.forkastet, it.generasjoner)
                }
                block(vedtaksperiode)
                tx.lagreVedtaksperiode(fødselsnummer, vedtaksperiode.toDto())
            }
        }
    }

    internal fun TransactionalSession.finnVedtaksperioder(fødselsnummer: String): List<VedtaksperiodeDto> {
        return with(dao) {
            finnVedtaksperiodeIderFor(fødselsnummer).map { finnVedtaksperiode(it) }
        }
    }

    internal fun TransactionalSession.lagreVedtaksperioder(fødselsnummer: String, vedtaksperioder: List<VedtaksperiodeDto>) {
        vedtaksperioder.forEach { lagreVedtaksperiode(fødselsnummer, it) }
    }

    private fun TransactionalSession.finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto {
        return with(vedtakDao) {
            finnVedtaksperiode(vedtaksperiodeId)
                ?.copy(generasjoner = finnGenerasjoner(vedtaksperiodeId))
                ?: throw IllegalStateException("Forventer å finne vedtaksperiode for vedtaksperiodeId=$vedtaksperiodeId")
        }
    }

    private fun TransactionalSession.finnGenerasjoner(vedtaksperiodeId: UUID): List<GenerasjonDto> {
        return with(dao) {
            finnGenerasjoner(vedtaksperiodeId)
        }
    }

    private fun TransactionalSession.lagreVedtaksperiode(fødselsnummer: String, vedtaksperiode: VedtaksperiodeDto) {
        with(vedtakDao) {
            lagreVedtaksperiode(fødselsnummer, vedtaksperiode)
        }
        with(dao) {
            vedtaksperiode.generasjoner.forEach { generasjonDto ->
                lagreGenerasjon(generasjonDto)
            }
        }
        with(vedtakDao) {
            lagreOpprinneligSøknadsdato(vedtaksperiode.vedtaksperiodeId)
        }
    }

    internal fun brukGenerasjon(vedtaksperiodeId: UUID, block: (generasjon: Generasjon) -> Unit) {
        val generasjon = dao.finnGjeldendeGenerasjon(vedtaksperiodeId)?.tilGenerasjon()
            ?: throw IllegalStateException("Forventer å finne en generasjon for vedtaksperiodeId=$vedtaksperiodeId")
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

    internal fun skjæringstidspunktFor(vedtaksperiodeId: UUID): LocalDate {
        return dao.finnSkjæringstidspunktFor(vedtaksperiodeId)
            ?: throw IllegalStateException("Forventer å finne skjæringstidspunkt for vedtaksperiodeId=$vedtaksperiodeId")
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

    internal fun førsteKjenteDag(fødselsnummer: String) = dao.førsteKjenteDag(fødselsnummer)

    private fun GenerasjonDto.tilGenerasjon(): Generasjon {
        return Generasjon.fraLagring(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = fom,
            tom = tom,
            tilstand = when (tilstand) {
                TilstandDto.Låst -> Generasjon.Låst
                TilstandDto.Ulåst -> Generasjon.Ulåst
                TilstandDto.AvsluttetUtenUtbetaling -> Generasjon.AvsluttetUtenUtbetaling
                TilstandDto.UtenUtbetalingMåVurderes -> Generasjon.UtenUtbetalingMåVurderes
            },
            tags = tags,
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
