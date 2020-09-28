package no.nav.helse.modell.risiko

import kotliquery.Session
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.kafka.meldinger.RisikovurderingLøsning
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import java.time.Duration
import java.util.*

internal class OldRisikoCommando(
    eventId: UUID,
    val risikovurderingDao: RisikovurderingDao,
    val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle,
    parent: Command
) : Command(eventId, parent, Duration.ofHours(1)) {

    var resultat: Resultat = Resultat.HarBehov(Behovtype.Risikovurdering)



    override fun execute(session: Session): Resultat {
        resultat = if(miljøstyrtFeatureToggle.risikovurdering()) { resultat } else Resultat.Ok.System
        return resultat
    }

    override fun resume(session: Session, løsninger: Løsninger) {
        resultat = if(miljøstyrtFeatureToggle.risikovurdering()) {
            val løsning = løsninger.løsning<RisikovurderingLøsning>()
            løsning.lagre(risikovurderingDao)
            Resultat.Ok.System
        } else {
            Resultat.Ok.System
        }

        resultat = Resultat.Ok.System
    }
}
