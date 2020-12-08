package no.nav.helse.modell.kommando

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.meldinger.Arbeidsgiverløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import java.time.LocalDate

internal class OppdaterArbeidsgiverCommand(
    private val orgnummer: String,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (!miljøstyrtFeatureToggle.arbeidsgiverinformasjon() || (navnErOppdatert() && bransjerErOppdatert())) return true
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun navnErOppdatert(): Boolean {
        val sistOppdatert = arbeidsgiverDao.findNavnSistOppdatert(orgnummer)
        return sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun bransjerErOppdatert(): Boolean {
        val sistOppdatert = arbeidsgiverDao.findBransjerSistOppdatert(orgnummer)
        return sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<Arbeidsgiverløsning>() ?: return trengerMerInformasjon(context)
        løsning.oppdater(arbeidsgiverDao, orgnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        context.behov("Arbeidsgiverinformasjon", mapOf("organisasjonsnummer" to orgnummer))
        return false
    }
}
