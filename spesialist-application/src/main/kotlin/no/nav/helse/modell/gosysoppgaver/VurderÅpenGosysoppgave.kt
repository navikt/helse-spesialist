package no.nav.helse.modell.gosysoppgaver

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.SessionContext
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import java.time.LocalDate
import java.util.UUID

internal class VurderÅpenGosysoppgave(
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val vedtaksperiodeId: UUID,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val harTildeltOppgave: Boolean,
    private val oppgaveService: OppgaveService,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ) = behandle(commandContext)

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean = behandle(commandContext)

    private fun behandle(commandContext: CommandContext): Boolean {
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

        løsning.lagre(åpneGosysOppgaverDao)
        løsning.evaluer(vedtaksperiodeId, sykefraværstilfelle, harTildeltOppgave, oppgaveService)
        return true
    }

    private fun ikkeEldreEnn(vedtaksperiodeId: UUID): LocalDate {
        val ikkeEldreEnn = sykefraværstilfelle.skjæringstidspunkt.minusYears(1)
        logg.info(
            "Sender {} for {} i behov for oppgaveinformasjon fra Gosys",
            kv("ikkeEldreEnn", ikkeEldreEnn),
            kv("vedtaksperiodeId", vedtaksperiodeId),
        )
        return ikkeEldreEnn
    }
}
