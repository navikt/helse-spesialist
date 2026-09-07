package no.nav.helse.modell.gosysoppgaver

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_1
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_3
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel.Companion.oppdatertEllerNyttVarsel
import java.time.LocalDate
import java.util.*

internal class VurderÅpenGosysoppgave(
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: SpleisBehandlingId,
    private val harTildeltOppgave: Boolean,
    private val oppgaveService: OppgaveService,
    private val skjæringstidspunkt: LocalDate,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ) = behandle(commandContext, sessionContext)

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean = behandle(commandContext, sessionContext)

    private fun behandle(
        commandContext: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val behandling = sessionContext.behandlingRepository.finn(spleisBehandlingId) ?: error("Fant ikke behandling med id $spleisBehandlingId")

        val løsning = commandContext.get<ÅpneGosysOppgaverløsning>()
        if (løsning == null) {
            logg.info("Trenger oppgaveinformasjon fra Gosys")
            commandContext.behov(
                Behov.ÅpneOppgaver(
                    ikkeEldreEnn = ikkeEldreEnn(vedtaksperiodeId),
                ),
            )
            return false
        }

        løsning.lagre(sessionContext.åpneGosysOppgaverDao)
        løsning.evaluer(vedtaksperiodeId, harTildeltOppgave, oppgaveService, sessionContext, behandling)
        return true
    }

    private fun ikkeEldreEnn(vedtaksperiodeId: UUID): LocalDate {
        val ikkeEldreEnn = skjæringstidspunkt.minusYears(1)
        logg.info(
            "Sender {} for {} i behov for oppgaveinformasjon fra Gosys",
            kv("ikkeEldreEnn", ikkeEldreEnn),
            kv("vedtaksperiodeId", vedtaksperiodeId),
        )
        return ikkeEldreEnn
    }

    internal fun ÅpneGosysOppgaverløsning.evaluer(
        vedtaksperiodeId: UUID,
        harTildeltOppgave: Boolean,
        oppgaveService: OppgaveService,
        sessionContext: SessionContext,
        behandling: Behandling,
    ) {
        varslerForOppslagFeilet(sessionContext, behandling)
        varslerForÅpneGosysOppgaver(vedtaksperiodeId, harTildeltOppgave, oppgaveService, sessionContext, behandling)
    }

    private fun ÅpneGosysOppgaverløsning.varslerForOppslagFeilet(
        sessionContext: SessionContext,
        behandling: Behandling,
    ) {
        val varslerForBehandling = sessionContext.varselRepository.finnVarslerFor(behandling.id)
        if (oppslagFeilet) {
            varslerForBehandling.oppdatertEllerNyttVarsel(SB_EX_3, behandling)?.also {
                sessionContext.varselRepository.lagre(it)
            }
        } else {
            val eksisterendeVarsel = varslerForBehandling.find { it.kode == SB_EX_3.name } ?: return
            eksisterendeVarsel.deaktiver()
            sessionContext.varselRepository.lagre(eksisterendeVarsel)
        }
    }

    private fun ÅpneGosysOppgaverløsning.varslerForÅpneGosysOppgaver(
        vedtaksperiodeId: UUID,
        harTildeltOppgave: Boolean,
        oppgaveService: OppgaveService,
        sessionContext: SessionContext,
        behandling: Behandling,
    ) {
        if (antall == null) return

        when {
            antall > 0 -> {
                val varslerForBehandling = sessionContext.varselRepository.finnVarslerFor(behandling.id)
                varslerForBehandling.oppdatertEllerNyttVarsel(SB_EX_1, behandling)?.also {
                    sessionContext.varselRepository.lagre(it)
                }

                val behandling = sessionContext.behandlingRepository.finnBehandlingspakke(behandling, fødselsnummer)
                val varsler = sessionContext.varselRepository.finnVarslerFor(behandling.map { it.id })
                if (varsler.all { it.kode == SB_EX_1.name }) {
                    oppgaveService.leggTilGosysEgenskap(vedtaksperiodeId)
                }
            }

            antall == 0 && !harTildeltOppgave -> {
                val varslerForBehandling = sessionContext.varselRepository.finnVarslerFor(behandling.id)
                oppgaveService.fjernGosysEgenskap(vedtaksperiodeId)
                val eksisterendeVarsel = varslerForBehandling.find { it.kode == SB_EX_1.name } ?: return
                eksisterendeVarsel.deaktiver()
                sessionContext.varselRepository.lagre(eksisterendeVarsel)
            }
        }
    }
}
