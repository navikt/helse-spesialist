package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import java.time.LocalDateTime

internal class OpprettArbeidsgiverCommand(
    private val spleisbehov: Spleisbehov,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Command() {
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        if (arbeidsgiverDao.findArbeidsgiver(spleisbehov.orgnummer.toLong()) != null) {
            ferdigstilt = LocalDateTime.now()
        } else {
            spleisbehov.håndter(Behovtype.HentArbeidsgiverNavn)
        }
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        arbeidsgiverDao.insertArbeidsgiver(spleisbehov.orgnummer.toLong(), løsning.navn)
        ferdigstilt = LocalDateTime.now()
    }
}
