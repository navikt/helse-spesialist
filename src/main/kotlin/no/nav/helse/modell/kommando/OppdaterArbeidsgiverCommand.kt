package no.nav.helse.modell.kommando

import no.nav.helse.mediator.meldinger.ArbeidsgiverLøsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import java.time.LocalDate

internal class OppdaterArbeidsgiverCommand(private val orgnummer: String, private val arbeidsgiverDao: ArbeidsgiverDao) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (erOppdatert()) return true
        return true
        // TODO: Faktisk hente arbeidsgiver info
        // return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun erOppdatert(): Boolean {
        val sistOppdatert = arbeidsgiverDao.findNavnSistOppdatert(orgnummer)
        return sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<ArbeidsgiverLøsning>() ?: return trengerMerInformasjon(context)
        løsning.oppdater(arbeidsgiverDao, orgnummer)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        context.behov("HentArbeidsgiver", mapOf(
            "organisasjonsnummer" to orgnummer
        ))
        return false
    }
}
