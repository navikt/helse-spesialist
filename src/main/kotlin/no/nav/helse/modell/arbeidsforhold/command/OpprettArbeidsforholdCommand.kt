package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OpprettArbeidsforholdCommand(
    private val fødselsnummer: String,
    private val arbeidsforholdDao: ArbeidsforholdDao,
    private val organisasjonsnummer: String,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettArbeidsforholdCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.arbeidsforhold() || arbeidsforholdOpprettet()) return ignorer()
        return behandle(context)
    }

    override fun resume(context: CommandContext) = behandle(context)

    private fun arbeidsforholdOpprettet() =
        arbeidsforholdDao.findArbeidsforhold(fødselsnummer, organisasjonsnummer) != null

    private fun ignorer(): Boolean {
        logg.info(if (miljøstyrtFeatureToggle.arbeidsforhold()) "Arbeidsforhold togglet av" else "Arbeidsforhold finnes fra før")
        return true
    }

    private fun behandle(context: CommandContext): Boolean {
        context.get<Arbeidsforholdløsning>()?.lagre(arbeidsforholdDao, fødselsnummer) ?: return trengerMerInformasjon(
            context
        )
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        logg.info("Trenger mer informasjon for å opprette arbeidsforhold")
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
