package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterArbeidsforholdCommand(
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val arbeidsforholdDao: ArbeidsforholdDao,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OppdaterArbeidsforholdCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.arbeidsforhold() || arbeidsforholdOppdatert()) return ignorer()
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun arbeidsforholdOppdatert() =
        arbeidsforholdDao.findArbeidsforholdSistOppdatert(fødselsnummer, organisasjonsnummer) > LocalDate.now()
            .minusDays(14)

    private fun ignorer(): Boolean {
        logg.info(if (miljøstyrtFeatureToggle.arbeidsforhold()) "Arbeidsforhold togglet av" else "Arbeidsforhold allerede oppdatert")
        return true
    }

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<Arbeidsforholdløsning>() ?: return trengerMerInformasjon(context)
        løsning.oppdater(arbeidsforholdDao, fødselsnummer, organisasjonsnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        logg.info("Trenger mer informasjon for å oppdatere arbeidsforhold")
        context.behov(
            "Arbeidsforhold", mapOf(
                "organisasjonnummer" to organisasjonsnummer,
                "fom" to LocalDate.now().minusYears(3),
                "tom" to LocalDate.now()
            )
        )
        return false
    }
}
