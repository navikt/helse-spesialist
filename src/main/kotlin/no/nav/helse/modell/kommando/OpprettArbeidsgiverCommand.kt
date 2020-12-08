package no.nav.helse.modell.kommando

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.meldinger.Arbeidsgiverløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.slf4j.LoggerFactory

internal class OpprettArbeidsgiverCommand(
    private val orgnummer: String,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettArbeidsgiverCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer) != null) return ignorer()
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
        if (!miljøstyrtFeatureToggle.arbeidsgiverinformasjon()) {
            log.info("oppretter arbeidsgiver")
            arbeidsgiverDao.insertArbeidsgiver(orgnummer, "Ukjent", "Ukjent")
        } else {
            val arbeidsgiver = context.get<Arbeidsgiverløsning>() ?: return trengerMerInformasjon(context)
            log.info("oppretter arbeidsgiver")
            arbeidsgiver.opprett(arbeidsgiverDao, orgnummer)
        }
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        context.behov("Arbeidsgiverinformasjon", mapOf("organisasjonsnummer" to orgnummer))
        return false
    }
}
