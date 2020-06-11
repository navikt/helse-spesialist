package no.nav.helse.modell.arbeidsgiver

import kotliquery.Session
import no.nav.helse.modell.command.Command
import java.time.Duration
import java.util.*

internal class OppdatertArbeidsgiverCommand(
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val orgnummer: String,
    behovId: UUID,
    parent: Command
) : Command(
    behovId = behovId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {
    override fun execute(session: Session): Resultat {
        return Resultat.Ok.System
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        arbeidsgiverDao.updateNavn(orgnummer, løsning.navn)
    }
}
