package no.nav.helse.modell.arbeidsgiver

import kotliquery.Session
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import java.time.Duration
import java.util.*

internal class OpprettArbeidsgiverCommand(
    private val orgnummer: String,
    eventId: UUID,
    parent: Command
) : Command(
    eventId = eventId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {

    override fun execute(session: Session): Resultat {
        return if (session.findArbeidsgiverByOrgnummer(orgnummer.toLong()) != null) {
            Resultat.Ok.System
        } else {
            session.insertArbeidsgiver(orgnummer.toLong(), "Ukjent")
            Resultat.Ok.System
            //spleisbehov.håndter(Behovtype.HentArbeidsgiverNavn)
        }
    }

    override fun resume(session: Session, løsninger: Løsninger) {
        val løsning = løsninger.løsning<ArbeidsgiverLøsning>()
        // TODO: Faktisk hente arbeidsgiver info
    }
}
