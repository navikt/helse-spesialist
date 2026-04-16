package no.nav.helse.modell.kommando

import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.ArbeidsforholdDto
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.Outbox
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
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        if (arbeidsforhold.any { it.måOppdateres() }) {
            return trengerMerInformasjon(commandContext).also {
                log.info("Trenger mer informasjon for å opprette eller oppdatere arbeidsforhold")
            }
        } else {
            log.info("Arbeidsforhold er allerede oppdatert")
        }
        return true
    }

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val løsning = commandContext.get<Arbeidsforholdløsning>() ?: return trengerMerInformasjon(commandContext)
        return behandle(løsning)
    }

    fun behandle(løsning: Arbeidsforholdløsning): Boolean {
        if (arbeidsforhold.any { it.måOppdateres() }) {
            løsning.upsert(arbeidsforholdDao, fødselsnummer, organisasjonsnummer)
        }
        return true
    }

    private fun trengerMerInformasjon(commandContext: CommandContext): Boolean {
        commandContext.behov(Behov.Arbeidsforhold(fødselsnummer, organisasjonsnummer))
        return false
    }
}
