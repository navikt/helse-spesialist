package no.nav.helse.spesialist.application

import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto

class UnimplementedÅpneGosysOppgaverDao : ÅpneGosysOppgaverDao {
    override fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto) {
        TODO("Not yet implemented")
    }

    override fun antallÅpneOppgaver(fødselsnummer: String): Int? {
        TODO("Not yet implemented")
    }
}
