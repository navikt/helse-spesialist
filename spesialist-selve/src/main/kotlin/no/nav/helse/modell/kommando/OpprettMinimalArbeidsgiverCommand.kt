package no.nav.helse.modell.kommando

import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.slf4j.LoggerFactory

internal class OpprettMinimalArbeidsgiverCommand(
    private val organisasjonsnummer: String,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettMinimalArbeidsgiverCommand::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        if (arbeidsgiverFinnes()) {
            log.info("arbeidsgiver finnes fra før, lager ikke ny")
        } else {
            sikkerLog.info("Oppretter minimal arbeidsgiver for organisasjonsnummer: $organisasjonsnummer")
            arbeidsgiverDao.insertArbeidsgiver(organisasjonsnummer)
        }
        return true
    }

    private fun arbeidsgiverFinnes() =
        arbeidsgiverDao.findArbeidsgiverByOrgnummer(organisasjonsnummer) != null
}
