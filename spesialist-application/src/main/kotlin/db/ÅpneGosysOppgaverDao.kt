package no.nav.helse.db

import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto

interface ÅpneGosysOppgaverDao {
    fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto)

    fun antallÅpneOppgaver(fødselsnummer: String): Int?
}
