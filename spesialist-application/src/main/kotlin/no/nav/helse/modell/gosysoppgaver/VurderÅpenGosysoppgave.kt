package no.nav.helse.modell.gosysoppgaver

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Fødselsnummer
import java.time.LocalDate
import java.util.UUID

internal class VurderÅpenGosysoppgave(
    private val vedtaksperiodeId: UUID,
    private val harTildeltOppgave: Boolean,
    private val oppgaveService: OppgaveService,
    private val skjæringstidspunkt: LocalDate,
    private val fødselsnummer: Fødselsnummer,
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
        return sessionContext.legacyPersonRepository.brukPerson(fødselsnummer.value) {
            val sykefraværstilfelle = this.sykefraværstilfelle(vedtaksperiodeId)
            val løsning = commandContext.get<ÅpneGosysOppgaverløsning>()
            if (løsning == null) {
                logg.info("Trenger oppgaveinformasjon fra Gosys")
                commandContext.behov(
                    Behov.ÅpneOppgaver(
                        ikkeEldreEnn = ikkeEldreEnn(vedtaksperiodeId),
                    ),
                )
                return@brukPerson false
            }

            løsning.lagre(sessionContext.åpneGosysOppgaverDao)
            løsning.evaluer(vedtaksperiodeId, sykefraværstilfelle, harTildeltOppgave, oppgaveService)
            return@brukPerson true
        }
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
}
