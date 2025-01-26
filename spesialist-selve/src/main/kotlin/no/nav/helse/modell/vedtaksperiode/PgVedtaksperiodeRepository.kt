package no.nav.helse.modell.vedtaksperiode

import kotliquery.TransactionalSession
import no.nav.helse.db.Repositories
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import org.slf4j.LoggerFactory
import java.util.UUID

class PgVedtaksperiodeRepository(private val repositories: Repositories) {
    private val generasjonDao = repositories.generasjonDao
    private val hentedeBehandlinger: MutableMap<UUID, List<BehandlingDto>> = mutableMapOf()

    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    fun TransactionalSession.finnVedtaksperioder(fødselsnummer: String): List<VedtaksperiodeDto> {
        return repositories.withSessionContext(this).generasjonDao.finnVedtaksperiodeIderFor(fødselsnummer).map { finnVedtaksperiode(it) }
    }

    fun TransactionalSession.lagreVedtaksperioder(
        fødselsnummer: String,
        vedtaksperioder: List<VedtaksperiodeDto>,
    ) {
        vedtaksperioder.forEach { lagreVedtaksperiode(fødselsnummer, it) }
    }

    private fun TransactionalSession.finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto {
        return repositories.withSessionContext(this).vedtakDao.finnVedtaksperiode(vedtaksperiodeId)
            ?.copy(behandlinger = finnGenerasjoner(vedtaksperiodeId))
            ?: throw IllegalStateException("Forventer å finne vedtaksperiode for vedtaksperiodeId=$vedtaksperiodeId")
    }

    private fun TransactionalSession.finnGenerasjoner(vedtaksperiodeId: UUID): List<BehandlingDto> {
        return repositories.withSessionContext(this).generasjonDao.finnGenerasjoner(vedtaksperiodeId).also {
            hentedeBehandlinger[vedtaksperiodeId] = it
        }
    }

    private fun TransactionalSession.lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiode: VedtaksperiodeDto,
    ) {
        val sessionContext = repositories.withSessionContext(this)
        sessionContext.vedtakDao.lagreVedtaksperiode(fødselsnummer, vedtaksperiode)
        loggDiffMellomHentetOgSkalLagres(vedtaksperiode)
        hentedeBehandlinger.remove(vedtaksperiode.vedtaksperiodeId)
        vedtaksperiode.behandlinger.forEach { generasjonDto ->
            sessionContext.generasjonDao.lagreGenerasjon(generasjonDto)
        }
        sessionContext.vedtakDao.lagreOpprinneligSøknadsdato(vedtaksperiode.vedtaksperiodeId)
    }

    private fun loggDiffMellomHentetOgSkalLagres(vedtaksperiode: VedtaksperiodeDto) {
        val hentedeGenerasjonerForPeriode = hentedeBehandlinger[vedtaksperiode.vedtaksperiodeId] ?: return
        val antallHentet = hentedeGenerasjonerForPeriode.size
        if (antallHentet == 0) return
        val generasjonerForLagring = vedtaksperiode.behandlinger
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
        hentet: BehandlingDto,
        skalLagres: BehandlingDto,
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

    internal fun førsteKjenteDag(fødselsnummer: String) = generasjonDao.førsteKjenteDag(fødselsnummer)
}
