package no.nav.helse.db

import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto

interface ÅpneGosysOppgaverRepository {
    fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto)

    fun antallÅpneOppgaver(fødselsnummer: String): Int?
}
