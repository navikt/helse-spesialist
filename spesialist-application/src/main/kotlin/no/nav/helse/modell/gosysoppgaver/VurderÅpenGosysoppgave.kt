package no.nav.helse.modell.gosysoppgaver

import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Sykefraværstilfelle
import java.time.LocalDate
import java.util.UUID

internal class VurderÅpenGosysoppgave(
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val vedtaksperiodeId: UUID,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val harTildeltOppgave: Boolean,
    private val oppgaveService: OppgaveService,
) : Command() {
    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<ÅpneGosysOppgaverløsning>()
        if (løsning == null) {
            context.behov(
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
        val ikkeEldreEnn = sykefraværstilfelle.skjæringstidspunkt().minusYears(1)
        aktivitetslogg.info(
            tekst = "Har behov for oppgaveinformasjon fra Gosys",
            "ikkeEldreEnn" to ikkeEldreEnn,
            "vedtaksperiodeId" to vedtaksperiodeId,
        )
        return ikkeEldreEnn
    }
}
