package no.nav.helse.modell.oppgave

import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import java.time.Duration
import java.util.*

internal class OpprettArbeidsgiverCommand(
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val orgnummer: String,
    behovId: UUID,
    parent: Command
) : Command(
    behovId = behovId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {

    override fun execute(): Resultat {
        return if (arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer.toLong()) != null) {
            Resultat.Ok.System
        } else {
            arbeidsgiverDao.insertArbeidsgiver(orgnummer.toLong(), "Ukjent")
            Resultat.Ok.System
            //spleisbehov.håndter(Behovtype.HentArbeidsgiverNavn)
        }
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        // TODO: Faktisk hente arbeidsgiver info
    }
}
