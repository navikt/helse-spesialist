package no.nav.helse.spesialist.application

import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto

class InMemoryÅpneGosysOppgaverDao : ÅpneGosysOppgaverDao {
    val persisterteÅpneGosysOppgaver = mutableListOf<ÅpneGosysOppgaverDto>()

    override fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto) {
        persisterteÅpneGosysOppgaver.add(åpneGosysOppgaver)
    }

    override fun antallÅpneOppgaver(fødselsnummer: String): Int? = persisterteÅpneGosysOppgaver.lastOrNull { it.fødselsnummer == fødselsnummer }?.antall
}
