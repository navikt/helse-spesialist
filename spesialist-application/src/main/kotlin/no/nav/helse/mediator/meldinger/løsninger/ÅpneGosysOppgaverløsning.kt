package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.BehandlingRepository
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_1
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_3
import no.nav.helse.spesialist.application.VarselRepository
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDateTime
import java.util.*

class ÅpneGosysOppgaverløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val antall: Int?,
    private val oppslagFeilet: Boolean,
) {
    internal fun lagre(åpneGosysOppgaverDao: ÅpneGosysOppgaverDao) {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
                opprettet = opprettet,
            ),
        )
    }

    internal fun evaluer(
        vedtaksperiodeId: UUID,
        varselRepository: VarselRepository,
        behandlingRepository: BehandlingRepository,
        harTildeltOppgave: Boolean,
        oppgaveService: OppgaveService,
    ) {
        varslerForOppslagFeilet(
            vedtaksperiodeId,
            varselRepository,
            behandlingRepository,
        )
        varslerForÅpneGosysOppgaver(
            vedtaksperiodeId,
            varselRepository,
            behandlingRepository,
            harTildeltOppgave,
            oppgaveService,
        )
    }

    private fun varslerForOppslagFeilet(
        vedtaksperiodeId: UUID,
        varselRepository: VarselRepository,
        behandlingRepository: BehandlingRepository,
    ) {
        val nyesteBehandling =
            behandlingRepository.finnNyesteForVedtaksperiode(VedtaksperiodeId(vedtaksperiodeId))
                ?: error("Fant ikke behandling")

        if (oppslagFeilet) {
            val varsler = varselRepository.finnVarslerFor(behandlingUnikId = nyesteBehandling.id)
            varsler.none { it.kode == SB_EX_3.name }.let {
                varselRepository.lagre(
                    Varsel.nytt(
                        id = VarselId(UUID.randomUUID()),
                        behandlingUnikId = nyesteBehandling.id,
                        spleisBehandlingId = nyesteBehandling.spleisBehandlingId,
                        kode = SB_EX_3.name,
                        opprettetTidspunkt = LocalDateTime.now(),
                    ),
                )
            }
        } else {
            val varsler = varselRepository.finnVarslerFor(behandlingUnikId = nyesteBehandling.id)
            varsler.find { it.kode == SB_EX_3.name }?.let {
                it.deaktiver()
                varselRepository.lagre(it)
            }
        }
    }

    private fun varslerForÅpneGosysOppgaver(
        vedtaksperiodeId: UUID,
        varselRepository: VarselRepository,
        behandlingRepository: BehandlingRepository,
        harTildeltOppgave: Boolean,
        oppgaveService: OppgaveService,
    ) {
        if (antall == null) return

        when {
            antall > 0 -> {
                val nyesteBehandling =
                    behandlingRepository.finnNyesteForVedtaksperiode(VedtaksperiodeId(vedtaksperiodeId))
                        ?: error("Fant ikke behandling")

                val varsler = varselRepository.finnVarslerFor(nyesteBehandling.id)
                varsler.find { it.kode == SB_EX_1.name }?.let {
                    it.deaktiver()
                    varselRepository.lagre(it)
                }
                    ?: varselRepository.lagre(
                        Varsel.nytt(
                            id = VarselId(UUID.randomUUID()),
                            behandlingUnikId = nyesteBehandling.id,
                            spleisBehandlingId = nyesteBehandling.spleisBehandlingId,
                            kode = SB_EX_1.name,
                            opprettetTidspunkt = LocalDateTime.now(),
                        ),
                    )

                val behandlingspakke =
                    behandlingRepository.finnAndreBehandlingerISykefraværstilfelle(nyesteBehandling, fødselsnummer)
                val behandlingUnikIder = behandlingspakke.map { behandling -> behandling.id }
                val varslerPåTvers = varselRepository.finnVarslerFor(behandlingUnikIder)

                if (varslerPåTvers.filterNot { it.kode == SB_EX_1.name }.isEmpty()) {
                    oppgaveService.leggTilGosysEgenskap(vedtaksperiodeId)
                }
            }

            antall == 0 && !harTildeltOppgave -> {
                oppgaveService.fjernGosysEgenskap(vedtaksperiodeId)

                val nyesteBehandling =
                    behandlingRepository.finnNyesteForVedtaksperiode(VedtaksperiodeId(vedtaksperiodeId))
                        ?: error("Fant ikke behandling")

                val varsler = varselRepository.finnVarslerFor(behandlingUnikId = nyesteBehandling.id)
                varsler.find { it.kode == SB_EX_1.name }?.let {
                    it.deaktiver()
                    varselRepository.lagre(it)
                }
            }
        }
    }
}
