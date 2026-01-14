package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.LegacyBehandlingDao
import no.nav.helse.db.LegacyVedtaksperiodeRepository
import no.nav.helse.db.VedtakDao
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.spesialist.application.logg.sikkerlogg
import java.util.UUID

class PgLegacyVedtaksperiodeRepository(
    private val legacyBehandlingDao: LegacyBehandlingDao,
    private val vedtakDao: VedtakDao,
) : LegacyVedtaksperiodeRepository {
    private val hentedeBehandlinger: MutableMap<UUID, List<BehandlingDto>> = mutableMapOf()

    override fun finnVedtaksperioder(fødselsnummer: String): List<VedtaksperiodeDto> = legacyBehandlingDao.finnVedtaksperiodeIderFor(fødselsnummer).map { finnVedtaksperiode(it) }

    override fun lagreVedtaksperioder(
        fødselsnummer: String,
        vedtaksperioder: List<VedtaksperiodeDto>,
    ) {
        vedtaksperioder.forEach { lagreVedtaksperiode(fødselsnummer, it) }
    }

    private fun finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto =
        vedtakDao
            .finnVedtaksperiode(vedtaksperiodeId)
            ?.copy(behandlinger = finnBehandlinger(vedtaksperiodeId))
            ?: throw IllegalStateException("Forventer å finne vedtaksperiode for vedtaksperiodeId=$vedtaksperiodeId")

    private fun finnBehandlinger(vedtaksperiodeId: UUID): List<BehandlingDto> =
        legacyBehandlingDao.finnLegacyBehandlinger(vedtaksperiodeId).also {
            hentedeBehandlinger[vedtaksperiodeId] = it
        }

    private fun lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiode: VedtaksperiodeDto,
    ) {
        vedtakDao.lagreVedtaksperiode(fødselsnummer, vedtaksperiode)
        loggDiffMellomHentetOgSkalLagres(vedtaksperiode)
        hentedeBehandlinger.remove(vedtaksperiode.vedtaksperiodeId)
        vedtaksperiode.behandlinger.forEach { behandlingDto ->
            legacyBehandlingDao.finnLegacyBehandling(behandlingDto)
        }
        vedtakDao.lagreOpprinneligSøknadsdato(vedtaksperiode.vedtaksperiodeId)
    }

    private fun loggDiffMellomHentetOgSkalLagres(vedtaksperiode: VedtaksperiodeDto) {
        val hentedeBehandlingerForPeriode = hentedeBehandlinger[vedtaksperiode.vedtaksperiodeId] ?: return
        val antallHentet = hentedeBehandlingerForPeriode.size
        if (antallHentet == 0) return
        val behandlingerForLagring = vedtaksperiode.behandlinger
        val antallNå = behandlingerForLagring.size
        val builder =
            StringBuilder().appendLine(
                "Hentet $antallHentet behandling(er) for ${vedtaksperiode.vedtaksperiodeId}, skal lagre $antallNå.",
            )

        if (antallHentet == antallNå) {
            val nyesteHentet = hentedeBehandlingerForPeriode.last()
            val nyesteSkalLagres = behandlingerForLagring.last()
            val nyesteErEendret = nyesteHentet != nyesteSkalLagres
            builder.appendLine("Ingen behandlinger ble lagt til. Nyeste versjon ble endret: $nyesteErEendret")
            if (nyesteErEendret) {
                builder.appendLine()
            }
            builder.diffMellomToBehandlinger(nyesteHentet, nyesteSkalLagres)
        }
        val hentedeBortsettFraSiste = hentedeBehandlingerForPeriode.dropLast(1)
        if (hentedeBortsettFraSiste.isNotEmpty()) {
            val historiskeForLagring = behandlingerForLagring.take(hentedeBortsettFraSiste.size)
            val historiskeErUlike = hentedeBortsettFraSiste != historiskeForLagring
            if (historiskeErUlike) {
                builder.appendLine("Historiske behandlinger ble endret")
                builder.appendLine("         før - etter:")
                hentedeBehandlingerForPeriode.zip(historiskeForLagring).forEach { (hentet, skalLagres) ->
                    builder.diffMellomToBehandlinger(hentet, skalLagres)
                }
            } else {
                builder.appendLine("Historiske behandlinger ble ikke endret")
            }
        }
        sikkerlogg.info(builder.toString())
    }

    private fun StringBuilder.diffMellomToBehandlinger(
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

    override fun førsteKjenteDag(fødselsnummer: String) = legacyBehandlingDao.førsteKjenteDag(fødselsnummer)
}
