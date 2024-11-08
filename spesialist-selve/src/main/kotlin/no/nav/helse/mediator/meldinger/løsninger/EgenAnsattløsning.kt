package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.EgenAnsattRepository
import java.time.LocalDateTime

internal class EgenAnsattløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val erEgenAnsatt: Boolean,
) {
    internal fun lagre(egenAnsattRepository: EgenAnsattRepository) {
        egenAnsattRepository.lagre(fødselsnummer, erEgenAnsatt, opprettet)
    }
}
