package no.nav.helse.modell.vedtaksperiode

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.person.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

internal class GenerasjonRepository(private val dataSource: DataSource) : IVedtaksperiodeObserver {
    private val dao = GenerasjonDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private var hentedeGenerasjoner: List<GenerasjonDto> = emptyList()

    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    internal fun brukVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        block: (vedtaksperiode: Vedtaksperiode) -> Unit,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val vedtaksperiode =
                    tx.finnVedtaksperiode(vedtaksperiodeId).let {
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

    internal fun TransactionalSession.lagreVedtaksperioder(
        fødselsnummer: String,
        vedtaksperioder: List<VedtaksperiodeDto>,
    ) {
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
            finnGenerasjoner(vedtaksperiodeId).also { hentedeGenerasjoner = it }
        }
    }

    private fun TransactionalSession.lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiode: VedtaksperiodeDto,
    ) {
        with(vedtakDao) {
            lagreVedtaksperiode(fødselsnummer, vedtaksperiode)
        }
        with(dao) {
            loggDiffMellomHentetOgSkalLagres(vedtaksperiode)
            hentedeGenerasjoner = emptyList()
            vedtaksperiode.generasjoner.forEach { generasjonDto ->
                lagreGenerasjon(generasjonDto)
            }
        }
        with(vedtakDao) {
            lagreOpprinneligSøknadsdato(vedtaksperiode.vedtaksperiodeId)
        }
    }

    private fun loggDiffMellomHentetOgSkalLagres(vedtaksperiode: VedtaksperiodeDto) {
        val antallHentet = hentedeGenerasjoner.size
        if (antallHentet == 0) return
        val generasjonerForLagring = vedtaksperiode.generasjoner
        val antallNå = generasjonerForLagring.size
        val builder =
            StringBuilder().appendLine(
                "Hentet $antallHentet generasjon(er) for ${vedtaksperiode.vedtaksperiodeId}, skal lagre $antallNå.",
            )

        if (antallHentet == antallNå) {
            val nyesteHentet = hentedeGenerasjoner.last()
            val nyesteSkalLagres = generasjonerForLagring.last()
            val nyesteErEendret = nyesteHentet != nyesteSkalLagres
            builder.appendLine("Ingen generasjoner ble lagt til. Nyeste versjon ble endret: $nyesteErEendret")
            if (nyesteErEendret) {
                builder.appendLine()
            }
            builder.diffMellomToGenerasjoner(nyesteHentet, nyesteSkalLagres)
        }
        val hentedeBortsettFraSiste = hentedeGenerasjoner.dropLast(1)
        if (hentedeBortsettFraSiste.isNotEmpty()) {
            val historiskeForLagring = generasjonerForLagring.take(hentedeBortsettFraSiste.size)
            val historiskeErUlike = hentedeBortsettFraSiste != historiskeForLagring
            if (historiskeErUlike) {
                builder.appendLine("Historiske generasjoner ble endret")
                builder.appendLine("         før - etter:")
                hentedeGenerasjoner.zip(historiskeForLagring).forEach { (hentet, skalLagres) ->
                    builder.diffMellomToGenerasjoner(hentet, skalLagres)
                }
            } else {
                builder.appendLine("Historiske generasjoner ble ikke endret")
            }
        }
        sikkerLogger.info(builder.toString())
    }

    private fun StringBuilder.diffMellomToGenerasjoner(
        hentet: GenerasjonDto,
        skalLagres: GenerasjonDto,
    ) {
        appendLine("   utbetaling: ${hentet.utbetalingId} - ${skalLagres.utbetalingId}")
        appendLine("      varsler: ${hentet.varsler.map { it.varselkode }} - ${skalLagres.varsler.map(VarselDto::varselkode)}")
        appendLine("         tags: ${hentet.tags} - ${skalLagres.tags}")
        appendLine("          fom: ${hentet.fom} - ${skalLagres.fom}")
        appendLine("          tom: ${hentet.tom} - ${skalLagres.tom}")
        appendLine("     tilstand: ${hentet.tilstand} - ${skalLagres.tilstand}")
        appendLine("  skj.tidspkt: ${hentet.skjæringstidspunkt} - ${skalLagres.skjæringstidspunkt}")
    }

    internal fun byggGenerasjon(
        vedtaksperiodeId: UUID,
        generasjonBuilder: GenerasjonBuilder,
    ) {
        dao.byggSisteFor(vedtaksperiodeId, generasjonBuilder)
    }

    internal fun finnVedtaksperiodeIderFor(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): Set<UUID> {
        return dao.finnVedtaksperiodeIderFor(fødselsnummer, skjæringstidspunkt)
    }

    internal fun skjæringstidspunktFor(vedtaksperiodeId: UUID): LocalDate {
        return dao.finnSkjæringstidspunktFor(vedtaksperiodeId)
            ?: throw IllegalStateException("Forventer å finne skjæringstidspunkt for vedtaksperiodeId=$vedtaksperiodeId")
    }

    override fun tilstandEndret(
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        gammel: String,
        ny: String,
        hendelseId: UUID,
    ) {
        dao.oppdaterTilstandFor(generasjonId, ny, hendelseId)
    }

    internal fun førsteKjenteDag(fødselsnummer: String) = dao.førsteKjenteDag(fødselsnummer)
}
