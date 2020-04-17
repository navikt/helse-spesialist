package no.nav.helse.modell.oppgave

import no.nav.helse.Løsningstype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

internal class OpprettArbeidsgiverCommand(
    private val spleisbehov: Spleisbehov,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val orgnummer: String,
    behovId: UUID,
    parent: Command,
    ferdigstilt: LocalDateTime? = null
) : Command(
    behovId = behovId,
    ferdigstilt = ferdigstilt,
    løsningstype = Løsningstype.System,
    parent = parent,
    timeout = Duration.ofHours(1)
) {

    override fun execute() {
        if (arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer.toLong()) != null) {
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
