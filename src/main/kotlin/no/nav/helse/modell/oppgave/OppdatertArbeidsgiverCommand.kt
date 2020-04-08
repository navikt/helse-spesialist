package no.nav.helse.modell.oppgave

import no.nav.helse.Løsningstype
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class OppdatertArbeidsgiverCommand(
    private val spleisbehov: Spleisbehov,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val orgnummer: String,
    behovId: UUID,
    ferdigstilt: LocalDateTime? = null
) : Command(behovId, ferdigstilt, Løsningstype.System) {

    override fun execute() {
        ferdigstilt = LocalDateTime.now()
        /*val sistOppdatert = arbeidsgiverDao.findNavnSistOppdatert(orgnummer.toLong())
        if (sistOppdatert.plusMonths(1) < LocalDate.now()) {
            spleisbehov.håndter(Behovtype.HentArbeidsgiverNavn)
        } else {
            ferdigstilt = LocalDateTime.now()
        }*/
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        arbeidsgiverDao.updateNavn(orgnummer, løsning.navn)
        ferdigstilt = LocalDateTime.now()
    }
}
