package no.nav.helse.modell.vedtaksperiode

import kotliquery.TransactionalSession
import no.nav.helse.db.PgVedtakDao
import no.nav.helse.modell.person.vedtaksperiode.GenerasjonDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

internal class GenerasjonService(dataSource: DataSource) {
    private val pgGenerasjonDao = PgGenerasjonDao(dataSource)
    private val hentedeGenerasjoner: MutableMap<UUID, List<GenerasjonDto>> = mutableMapOf()

    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    internal fun TransactionalSession.finnVedtaksperioder(fødselsnummer: String): List<VedtaksperiodeDto> {
        return PgGenerasjonDao(this).finnVedtaksperiodeIderFor(fødselsnummer).map { finnVedtaksperiode(it) }
    }

    internal fun TransactionalSession.lagreVedtaksperioder(
        fødselsnummer: String,
        vedtaksperioder: List<VedtaksperiodeDto>,
    ) {
        vedtaksperioder.forEach { lagreVedtaksperiode(fødselsnummer, it) }
    }

    private fun TransactionalSession.finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto {
        return PgVedtakDao(this).finnVedtaksperiode(vedtaksperiodeId)
            ?.copy(generasjoner = finnGenerasjoner(vedtaksperiodeId))
            ?: throw IllegalStateException("Forventer å finne vedtaksperiode for vedtaksperiodeId=$vedtaksperiodeId")
    }

    private fun TransactionalSession.finnGenerasjoner(vedtaksperiodeId: UUID): List<GenerasjonDto> {
        return PgGenerasjonDao(this).finnGenerasjoner(vedtaksperiodeId).also { hentedeGenerasjoner[vedtaksperiodeId] = it }
    }

    private fun TransactionalSession.lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiode: VedtaksperiodeDto,
    ) {
        PgVedtakDao(this).lagreVedtaksperiode(fødselsnummer, vedtaksperiode)
        loggDiffMellomHentetOgSkalLagres(vedtaksperiode)
        hentedeGenerasjoner.remove(vedtaksperiode.vedtaksperiodeId)
        vedtaksperiode.generasjoner.forEach { generasjonDto ->
            PgGenerasjonDao(this).lagreGenerasjon(generasjonDto)
        }
        PgVedtakDao(this).lagreOpprinneligSøknadsdato(vedtaksperiode.vedtaksperiodeId)
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
        appendLine("               forrige - skal lagres")
        appendLine("   utbetaling: ${hentet.utbetalingId} - ${skalLagres.utbetalingId}")
        appendLine("      varsler: ${hentet.varsler.prettyPrint()} - ${skalLagres.varsler.prettyPrint()}")
        appendLine("         tags: ${hentet.tags} - ${skalLagres.tags}")
        appendLine("          fom: ${hentet.fom} - ${skalLagres.fom}")
        appendLine("          tom: ${hentet.tom} - ${skalLagres.tom}")
        appendLine("     tilstand: ${hentet.tilstand} - ${skalLagres.tilstand}")
        appendLine("  skj.tidspkt: ${hentet.skjæringstidspunkt} - ${skalLagres.skjæringstidspunkt}")
    }

    private fun List<VarselDto>.prettyPrint(): List<String> = map { "${it.varselkode} (${it.status})" }

    internal fun førsteKjenteDag(fødselsnummer: String) = pgGenerasjonDao.førsteKjenteDag(fødselsnummer)
}
