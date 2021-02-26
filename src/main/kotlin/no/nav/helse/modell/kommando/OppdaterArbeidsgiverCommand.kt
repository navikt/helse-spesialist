package no.nav.helse.modell.kommando

import no.nav.helse.mediator.Toggles
import no.nav.helse.mediator.meldinger.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import java.time.LocalDate

internal class OppdaterArbeidsgiverCommand(
    private val orgnummere: List<String>,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val trengerOppdateringer = (ikkeOppdaterteBransjer() + ikkeOppdaterteNavn()).isEmpty()
        if (!Toggles.Arbeidsgiverinformasjon.enabled || trengerOppdateringer) return true
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun ikkeOppdaterteNavn() = orgnummere.filterNot { orgnummer ->
        arbeidsgiverDao.findNavnSistOppdatert(orgnummer) > LocalDate.now().minusDays(14)
    }

    private fun ikkeOppdaterteBransjer() = orgnummere.filterNot { orgnummer ->
        val sistOppdatert = arbeidsgiverDao.findBransjerSistOppdatert(orgnummer) ?: return@filterNot false
        sistOppdatert > LocalDate.now().minusDays(14)
    }

    private fun behandle(context: CommandContext): Boolean {
        val løsning = context.get<Arbeidsgiverinformasjonløsning>() ?: return trengerMerInformasjon(context)
        løsning.oppdater(arbeidsgiverDao)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        context.behov(
            "Arbeidsgiverinformasjon",
            mapOf("organisasjonsnummer" to (ikkeOppdaterteBransjer() + ikkeOppdaterteNavn()).distinct())
        )
        return false
    }
}
