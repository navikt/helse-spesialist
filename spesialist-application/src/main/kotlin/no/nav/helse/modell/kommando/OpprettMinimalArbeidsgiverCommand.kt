package no.nav.helse.modell.kommando

import no.nav.helse.db.InntektskilderRepository
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.NyInntektskildeDto
import org.slf4j.LoggerFactory

internal class OpprettMinimalArbeidsgiverCommand(
    private val organisasjonsnummer: String,
    private val inntektskilderRepository: InntektskilderRepository,
) : Command() {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettMinimalArbeidsgiverCommand::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        if (inntektskilderRepository.inntektskildeEksisterer(organisasjonsnummer)) {
            log.info("Inntekstkilde finnes fra før, lager ikke ny")
        } else {
            sikkerLog.info("Oppretter minimal arbeidsgiver for organisasjonsnummer: $organisasjonsnummer")
            inntektskilderRepository.lagreInntektskilder(
                listOf(
                    NyInntektskildeDto(identifikator = organisasjonsnummer, type = InntektskildetypeDto.ORDINÆR),
                ),
            )
        }
        return true
    }
}
