package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant

class EgenAnsattStatus(
    val erEgenAnsatt: Boolean,
    val oppdatertTidspunkt: Instant,
) : ValueObject {
    companion object {
        fun ny(erEgenAnsatt: Boolean): EgenAnsattStatus =
            EgenAnsattStatus(
                erEgenAnsatt = erEgenAnsatt,
                oppdatertTidspunkt = Instant.now(),
            )

        fun fraLagring(
            erEgenAnsatt: Boolean,
            oppdatertTidspunkt: Instant,
        ): EgenAnsattStatus =
            EgenAnsattStatus(
                erEgenAnsatt = erEgenAnsatt,
                oppdatertTidspunkt = oppdatertTidspunkt,
            )
    }
}
