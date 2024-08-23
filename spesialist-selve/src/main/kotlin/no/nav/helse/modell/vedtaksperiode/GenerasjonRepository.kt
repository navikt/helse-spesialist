package no.nav.helse.modell.vedtaksperiode

import kotliquery.TransactionalSession
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

internal class GenerasjonRepository(dataSource: DataSource) {
    private val generasjonDao = GenerasjonDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val hentedeGenerasjoner: MutableMap<UUID, List<GenerasjonDto>> = mutableMapOf()

    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    internal fun TransactionalSession.finnVedtaksperioder(fødselsnummer: String): List<VedtaksperiodeDto> {
        return with(generasjonDao) {
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
        return with(generasjonDao) {
            finnGenerasjoner(vedtaksperiodeId).also { hentedeGenerasjoner[vedtaksperiodeId] = it }
        }
    }

    private fun TransactionalSession.lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiode: VedtaksperiodeDto,
    ) {
        with(vedtakDao) {
            lagreVedtaksperiode(fødselsnummer, vedtaksperiode)
        }
        with(generasjonDao) {
            loggDiffMellomHentetOgSkalLagres(vedtaksperiode)
            hentedeGenerasjoner.remove(vedtaksperiode.vedtaksperiodeId)
            vedtaksperiode.generasjoner.forEach { generasjonDto ->
                lagreGenerasjon(generasjonDto)
            }
        }
        with(vedtakDao) {
            lagreOpprinneligSøknadsdato(vedtaksperiode.vedtaksperiodeId)
        }
    }

    private fun loggDiffMellomHentetOgSkalLagres(vedtaksperiode: VedtaksperiodeDto) {
        val hentedeGenerasjonerForPeriode = hentedeGenerasjoner[vedtaksperiode.vedtaksperiodeId] ?: return
        val antallHentet = hentedeGenerasjonerForPeriode.size
        if (antallHentet == 0) return
        val generasjonerForLagring = vedtaksperiode.generasjoner
        val antallNå = generasjonerForLagring.size
        val builder =
            StringBuilder().appendLine(
                "Hentet $antallHentet generasjon(er) for ${vedtaksperiode.vedtaksperiodeId}, skal lagre $antallNå.",
            )

        if (antallHentet == antallNå) {
            val nyesteHentet = hentedeGenerasjonerForPeriode.last()
            val nyesteSkalLagres = generasjonerForLagring.last()
            val nyesteErEendret = nyesteHentet != nyesteSkalLagres
            builder.appendLine("Ingen generasjoner ble lagt til. Nyeste versjon ble endret: $nyesteErEendret")
            if (nyesteErEendret) {
                builder.appendLine()
            }
            builder.diffMellomToGenerasjoner(nyesteHentet, nyesteSkalLagres)
        }
        val hentedeBortsettFraSiste = hentedeGenerasjonerForPeriode.dropLast(1)
        if (hentedeBortsettFraSiste.isNotEmpty()) {
            val historiskeForLagring = generasjonerForLagring.take(hentedeBortsettFraSiste.size)
            val historiskeErUlike = hentedeBortsettFraSiste != historiskeForLagring
            if (historiskeErUlike) {
                builder.appendLine("Historiske generasjoner ble endret")
                builder.appendLine("         før - etter:")
                hentedeGenerasjonerForPeriode.zip(historiskeForLagring).forEach { (hentet, skalLagres) ->
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

    internal fun førsteKjenteDag(fødselsnummer: String) = generasjonDao.førsteKjenteDag(fødselsnummer)
}
