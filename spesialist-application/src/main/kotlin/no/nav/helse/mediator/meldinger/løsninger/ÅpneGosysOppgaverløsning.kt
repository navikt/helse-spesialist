package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import java.time.LocalDateTime

class ÅpneGosysOppgaverløsning(
    val opprettet: LocalDateTime,
    val fødselsnummer: String,
    val antall: Int?,
    val oppslagFeilet: Boolean,
) {
    internal fun lagre(åpneGosysOppgaverDao: ÅpneGosysOppgaverDao) {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
                opprettet = opprettet,
            ),
        )
    }
}
