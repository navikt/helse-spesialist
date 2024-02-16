package no.nav.helse.modell.arbeidsforhold.command

import java.time.LocalDate
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory

internal class OppdaterArbeidsforholdCommand(
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val arbeidsforholdDao: ArbeidsforholdDao,
    private val førstegangsbehandling: Boolean
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(OppdaterArbeidsforholdCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val sistOppdatert = arbeidsforholdDao.findArbeidsforholdSistOppdatert(fødselsnummer, organisasjonsnummer)
        return when {
            sistOppdatert == null -> {
                logg.warn("Oppdaterer ikke arbeidsforhold som ikke finnes i databasen")
                true
            }
            skalOppdateres(sistOppdatert) -> behandle(context)
            else -> {
                logg.info("Arbeidsforhold er allerede oppdatert")
                true
            }
        }
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun skalOppdateres(sistOppdatert: LocalDate) =
         erIkkeOppdatertSiste14Dager(sistOppdatert) || erFørstegangIkkeOppdatertSisteDag(sistOppdatert)

    private fun erIkkeOppdatertSiste14Dager(sistOppdatert: LocalDate) =
         sistOppdatert < LocalDate.now().minusDays(14)

    private fun erFørstegangIkkeOppdatertSisteDag(sistOppdatert: LocalDate) =
        førstegangsbehandling && sistOppdatert <= LocalDate.now().minusDays(1)

    fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<Arbeidsforholdløsning>() ?: return trengerMerInformasjon(context)
        løsning.oppdater(arbeidsforholdDao, fødselsnummer, organisasjonsnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        logg.info("Trenger mer informasjon for å oppdatere arbeidsforhold")
        context.behov(
            "Arbeidsforhold", mapOf(
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
            )
        )
        return false
    }
}
