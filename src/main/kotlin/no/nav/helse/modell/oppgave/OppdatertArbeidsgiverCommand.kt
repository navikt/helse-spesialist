package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import java.time.LocalDate
import java.time.LocalDateTime

internal class OppdatertArbeidsgiverCommand(
    private val spleisbehov: Spleisbehov,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Command() {
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        val sistOppdatert = arbeidsgiverDao.findNavnSistOppdatert(spleisbehov.orgnummer.toLong())
        if (sistOppdatert.plusMonths(1) < LocalDate.now()) {
            spleisbehov.håndter(Behovtype.HentArbeidsgiverNavn)
        } else {
            ferdigstilt = LocalDateTime.now()
        }
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        arbeidsgiverDao.updateNavn(spleisbehov.orgnummer, løsning.navn)
        ferdigstilt = LocalDateTime.now()
    }
}
