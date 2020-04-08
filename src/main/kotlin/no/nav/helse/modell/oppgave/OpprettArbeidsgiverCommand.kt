package no.nav.helse.modell.oppgave

import no.nav.helse.Løsningstype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import java.time.LocalDateTime
import java.util.UUID

internal class OpprettArbeidsgiverCommand(
    private val spleisbehov: Spleisbehov,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val orgnummer: String,
    behovId: UUID,
    ferdigstilt: LocalDateTime? = null
) : Command(behovId, ferdigstilt, Løsningstype.System) {

    override fun execute() {
        if (arbeidsgiverDao.findArbeidsgiver(orgnummer.toLong()) != null) {
            ferdigstilt = LocalDateTime.now()
        } else {
            arbeidsgiverDao.insertArbeidsgiver(orgnummer.toLong(), "Ukjent")
            ferdigstilt = LocalDateTime.now()
            //spleisbehov.håndter(Behovtype.HentArbeidsgiverNavn)
        }
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        // TODO: Faktisk hente arbeidsgiver info
    }
}
