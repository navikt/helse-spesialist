package no.nav.helse.modell.kommando

import no.nav.helse.db.ArbeidsforholdRepository
import no.nav.helse.modell.ArbeidsforholdDto
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import org.slf4j.LoggerFactory

internal class OpprettEllerOppdaterArbeidsforhold(
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettEllerOppdaterArbeidsforhold::class.java)
    }

    private val arbeidsforhold =
        arbeidsforholdRepository.findArbeidsforhold(fødselsnummer, organisasjonsnummer)
            .ifEmpty {
                listOf(ArbeidsforholdDto(fødselsnummer = fødselsnummer, organisasjonsnummer = organisasjonsnummer))
            }

    override fun execute(context: CommandContext): Boolean {
        if (arbeidsforhold.any { it.måOppdateres() }) {
            return trengerMerInformasjon(context).also {
                log.info("Trenger mer informasjon for å opprette eller oppdatere arbeidsforhold")
            }
        } else {
            log.info("Arbeidsforhold er allerede oppdatert")
        }
        return true
    }

    override fun resume(context: CommandContext): Boolean {
        val løsning = context.get<Arbeidsforholdløsning>() ?: return trengerMerInformasjon(context)
        return behandle(løsning)
    }

    fun behandle(løsning: Arbeidsforholdløsning): Boolean {
        if (arbeidsforhold.any { it.måOppdateres() }) {
            løsning.upsert(arbeidsforholdRepository, fødselsnummer, organisasjonsnummer)
        }
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        context.behov(
            "Arbeidsforhold",
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
            ),
        )
        return false
    }
}
