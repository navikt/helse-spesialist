package no.nav.helse.modell.kommando

import no.nav.helse.mediator.Toggles
import no.nav.helse.mediator.meldinger.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.slf4j.LoggerFactory

internal class OpprettArbeidsgiverCommand(
    private val orgnummere: List<String>,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettArbeidsgiverCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (arbeidsgivereSomIkkeFinnes().isEmpty()) return ignorer()
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
        if (!Toggles.Arbeidsgiverinformasjon.enabled) {
            log.info("oppretter arbeidsgiver")
            arbeidsgivereSomIkkeFinnes().forEach {
                arbeidsgiverDao.insertArbeidsgiver(it, "Ukjent", emptyList())
            }
        } else {
            val arbeidsgivereSomIkkeFinnes = arbeidsgivereSomIkkeFinnes()
            if (arbeidsgivereSomIkkeFinnes.isEmpty()) return ignorer()
            val arbeidsgiver = context.get<Arbeidsgiverinformasjonløsning>() ?: return trengerMerInformasjon(context, arbeidsgivereSomIkkeFinnes)
            log.info("oppretter arbeidsgiver")
            arbeidsgiver.opprett(arbeidsgiverDao)
        }
        return true
    }

    private fun arbeidsgivereSomIkkeFinnes() = orgnummere.filter { arbeidsgiverDao.findArbeidsgiverByOrgnummer(it) == null }

    private fun trengerMerInformasjon(context: CommandContext, arbeidsgivereSomIkkeFinnes: List<String>): Boolean {
        context.behov("Arbeidsgiverinformasjon", mapOf("organisasjonsnummer" to arbeidsgivereSomIkkeFinnes))
        return false
    }
}
