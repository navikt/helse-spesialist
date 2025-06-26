package no.nav.helse.modell.kommando

import no.nav.helse.spesialist.application.ArbeidsgiverRepository
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import org.slf4j.LoggerFactory

internal class OpprettMinimalArbeidsgiverCommand(
    private val organisasjonsnummer: String,
    private val arbeidsgiverRepository: ArbeidsgiverRepository,
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettMinimalArbeidsgiverCommand::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val identifikator = ArbeidsgiverIdentifikator.fraString(organisasjonsnummer)
        if (arbeidsgiverRepository.finn(identifikator) != null) {
            log.info("Inntekstkilde finnes fra før, lager ikke ny")
        } else {
            sikkerLog.info("Oppretter minimal arbeidsgiver for organisasjonsnummer: $organisasjonsnummer")
            arbeidsgiverRepository.lagre(Arbeidsgiver.Factory.ny(identifikator = identifikator))
        }
        return true
    }
}
