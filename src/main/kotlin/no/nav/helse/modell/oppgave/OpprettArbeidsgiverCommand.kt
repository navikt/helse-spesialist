package no.nav.helse.modell.oppgave

import no.nav.helse.Oppgavestatus
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
    parent: Command
) : Command(
    behovId = behovId,
    initiellStatus = Oppgavestatus.AvventerSystem,
    parent = parent,
    timeout = Duration.ofHours(1)
) {

    override fun execute() {
        if (arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer.toLong()) != null) {
            ferdigstillSystem()
        } else {
            arbeidsgiverDao.insertArbeidsgiver(orgnummer.toLong(), "Ukjent")
            ferdigstillSystem()
            //spleisbehov.håndter(Behovtype.HentArbeidsgiverNavn)
        }
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        // TODO: Faktisk hente arbeidsgiver info
    }
}
