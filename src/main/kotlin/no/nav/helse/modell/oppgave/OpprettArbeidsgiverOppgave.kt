package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import java.time.LocalDateTime

internal class OpprettArbeidsgiverOppgave(
    private val spleisBehov: SpleisBehov,
    private val arbeidsgiverDao: ArbeidsgiverDao
) : Oppgave() {
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        if (arbeidsgiverDao.finnArbeidsgiver(spleisBehov.orgnummer.toLong())) {
            ferdigstilt = LocalDateTime.now()
        } else {
            spleisBehov.håndter(Behovtype.HentArbeidsgiverNavn)
        }
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        arbeidsgiverDao.opprettArbeidsgiver(spleisBehov.orgnummer.toLong(), løsning.navn)
        ferdigstilt = LocalDateTime.now()
    }
}
