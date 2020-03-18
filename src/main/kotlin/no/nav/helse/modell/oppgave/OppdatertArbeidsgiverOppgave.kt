package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import java.time.LocalDate
import java.time.LocalDateTime

internal class OppdatertArbeidsgiverOppgave(
    private val spleisBehov: SpleisBehov,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Oppgave() {
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        val sistOppdatert = arbeidsgiverDao.navnSistOppdatert(spleisBehov.orgnummer.toLong())
        if (sistOppdatert.plusMonths(1) > LocalDate.now()) {
            spleisBehov.håndter(Behovtype.HentArbeidsgiverNavn)
        } else {
            ferdigstilt = LocalDateTime.now()
        }
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        arbeidsgiverDao.oppdaterNavn(spleisBehov.orgnummer, løsning.navn)
    }
}
