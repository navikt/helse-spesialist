package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OppdaterArbeidsforholdCommand(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val arbeidsforholdDao: ArbeidsforholdDao,
    private val periodetype: Periodetype
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(OppdaterArbeidsforholdCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val sistOppdatert = arbeidsforholdDao.findArbeidsforholdSistOppdatert(fødselsnummer, organisasjonsnummer)
        return when {
            skalOppdateres(sistOppdatert) -> behandle(context)
            else -> {
                logg.info("Arbeidsforhold allerede oppdatert")
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
        periodetype == Periodetype.FØRSTEGANGSBEHANDLING
            && sistOppdatert <= LocalDate.now().minusDays(1)

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
