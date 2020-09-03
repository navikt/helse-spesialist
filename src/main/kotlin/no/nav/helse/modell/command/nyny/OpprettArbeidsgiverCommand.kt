package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.slf4j.LoggerFactory

internal class OpprettArbeidsgiverCommand(
    private val orgnummer: String,
    private val arbeidsgiverDao: ArbeidsgiverDao) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettArbeidsgiverCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer.toLong()) != null) return ignorer()
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun ignorer(): Boolean {
        log.info("arbeidsgiver finnes fra før, lager ikke ny")
        return true
    }

    private fun behandle(context: CommandContext): Boolean {
        // TODO: Faktisk hente arbeidsgiver info
        //val arbeidsgiver = context.get<Arbeidsgiverløsning>() ?: trengerMerInformasjon(context)
        log.info("oppretter arbeidsgiver")
        arbeidsgiverDao.insertArbeidsgiver(orgnummer.toLong(), "Ukjent")
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        context.behov("HentArbeidsgiver", mapOf(
            "organisasjonsnummer" to orgnummer
        ))
        return false
    }
}
