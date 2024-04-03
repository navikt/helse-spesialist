package no.nav.helse.modell.gosysoppgaver

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.UUID

internal class VurderÅpenGosysoppgave(
    private val hendelseId: UUID,
    private val aktørId: String,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val generasjonRepository: GenerasjonRepository,
    private val vedtaksperiodeId: UUID,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val harTildeltOppgave: Boolean,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(VurderÅpenGosysoppgave::class.java)
    }

    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<ÅpneGosysOppgaverløsning>()
        if (løsning == null) {
            logg.info("Trenger oppgaveinformasjon fra Gosys")
            context.behov(
                "ÅpneOppgaver",
                mapOf("aktørId" to aktørId, "ikkeEldreEnn" to ikkeEldreEnn(vedtaksperiodeId)),
            )
            return false
        }

        løsning.lagre(åpneGosysOppgaverDao)
        løsning.evaluer(vedtaksperiodeId, sykefraværstilfelle, hendelseId, harTildeltOppgave)
        return true
    }

    private fun ikkeEldreEnn(vedtaksperiodeId: UUID): LocalDate {
        val ikkeEldreEnn =
            runCatching { generasjonRepository.skjæringstidspunktFor(vedtaksperiodeId) }.fold(
                onSuccess = { it },
                onFailure = {
                    // Jeg tror egentlig ikke vi trenger å forvente at det ikke går å finne skjæringstidspunkt, men greit å være på den sikre siden
                    logg.warn(
                        "Mangler skjæringstidspunkt for {}, det er ikke forventet",
                        kv("vedtaksperiodeId", vedtaksperiodeId),
                    )
                    now()
                },
            ).minusYears(1)
        logg.info(
            "Sender {} for {} i behov for oppgaveinformasjon fra Gosys",
            kv("ikkeEldreEnn", ikkeEldreEnn),
            kv("vedtaksperiodeId", vedtaksperiodeId),
        )
        return ikkeEldreEnn
    }
}
