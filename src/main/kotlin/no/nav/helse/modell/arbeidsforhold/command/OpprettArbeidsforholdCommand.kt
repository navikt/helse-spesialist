package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.mediator.FeatureToggle.ARBEIDSFORHOLD_TOGGLE
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OpprettArbeidsforholdCommand(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val arbeidsforholdDao: ArbeidsforholdDao,
    private val organisasjonsnummer: String
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettArbeidsforholdCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (!ARBEIDSFORHOLD_TOGGLE.enabled || arbeidsforholdOpprettet()) return ignorer()
        return behandle(context)
    }

    override fun resume(context: CommandContext) = behandle(context)

    private fun arbeidsforholdOpprettet() =
        arbeidsforholdDao.findArbeidsforhold(fødselsnummer, organisasjonsnummer).isNotEmpty()

    private fun ignorer(): Boolean {
        logg.info(ARBEIDSFORHOLD_TOGGLE.ifEnabled({ "Arbeidsforhold togglet av" }) { "Arbeidsforhold finnes fra før" })
        return true
    }

    private fun behandle(context: CommandContext): Boolean {
        context.get<Arbeidsforholdløsning>()?.opprett(arbeidsforholdDao, fødselsnummer, organisasjonsnummer)
            ?: return trengerMerInformasjon(
                context
            )
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        logg.info("Trenger mer informasjon for å opprette arbeidsforhold")
        context.behov(
            "Arbeidsforhold", mapOf(
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "fom" to LocalDate.now().minusYears(3),
                "tom" to LocalDate.now()
            )
        )
        return false
    }
}
