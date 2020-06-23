package no.nav.helse.modell.arbeidsgiver

import kotliquery.Session
import no.nav.helse.modell.command.Command
import java.time.Duration
import java.util.*

internal class OpprettArbeidsgiverCommand(
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val orgnummer: String,
    eventId: UUID,
    parent: Command
) : Command(
    eventId = eventId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {

    override fun execute(session: Session): Resultat {
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
