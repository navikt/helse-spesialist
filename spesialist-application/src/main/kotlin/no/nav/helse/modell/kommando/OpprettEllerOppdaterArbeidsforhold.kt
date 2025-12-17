package no.nav.helse.modell.kommando

import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.ArbeidsforholdDto
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.melding.Behov
import org.slf4j.LoggerFactory

internal class OpprettEllerOppdaterArbeidsforhold(
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val arbeidsforholdDao: ArbeidsforholdDao,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettEllerOppdaterArbeidsforhold::class.java)
    }

    private val arbeidsforhold =
        arbeidsforholdDao
            .findArbeidsforhold(fødselsnummer, organisasjonsnummer)
            .ifEmpty {
                listOf(ArbeidsforholdDto(fødselsnummer = fødselsnummer, organisasjonsnummer = organisasjonsnummer))
            }

    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        if (arbeidsforhold.any { it.måOppdateres() }) {
            return trengerMerInformasjon(context).also {
                log.info("Trenger mer informasjon for å opprette eller oppdatere arbeidsforhold")
            }
        } else {
            log.info("Arbeidsforhold er allerede oppdatert")
        }
        return true
    }

    override fun resume(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val løsning = context.get<Arbeidsforholdløsning>() ?: return trengerMerInformasjon(context)
        return behandle(løsning)
    }

    fun behandle(løsning: Arbeidsforholdløsning): Boolean {
        if (arbeidsforhold.any { it.måOppdateres() }) {
            løsning.upsert(arbeidsforholdDao, fødselsnummer, organisasjonsnummer)
        }
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        context.behov(Behov.Arbeidsforhold(fødselsnummer, organisasjonsnummer))
        return false
    }
}
