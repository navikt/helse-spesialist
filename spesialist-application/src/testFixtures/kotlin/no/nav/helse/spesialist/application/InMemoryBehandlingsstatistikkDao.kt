package no.nav.helse.spesialist.application

import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.EgenskapForDatabase
import java.time.LocalDate

class InMemoryBehandlingsstatistikkDao : BehandlingsstatistikkDao {
    override fun getAntallTilgjengeligeBeslutteroppgaver(): Int {
        TODO("Not yet implemented")
    }

    override fun getAntallTilgjengeligeEgenAnsattOppgaver(): Int {
        TODO("Not yet implemented")
    }

    override fun getAntallManueltFullførteEgenAnsattOppgaver(fom: LocalDate): Int {
        TODO("Not yet implemented")
    }

    override fun getAntallFullførteBeslutteroppgaver(fom: LocalDate): Int {
        TODO("Not yet implemented")
    }

    override fun getAutomatiseringPerKombinasjon(fom: LocalDate): BehandlingsstatistikkDao.StatistikkPerKombinasjon {
        TODO("Not yet implemented")
    }

    override fun getTilgjengeligeOppgaverPerInntektOgPeriodetype(): BehandlingsstatistikkDao.StatistikkPerKombinasjon {
        TODO("Not yet implemented")
    }

    override fun getManueltUtførteOppgaverPerInntektOgPeriodetype(fom: LocalDate): BehandlingsstatistikkDao.StatistikkPerKombinasjon {
        TODO("Not yet implemented")
    }

    override fun antallTilgjengeligeOppgaverFor(egenskap: EgenskapForDatabase): Int {
        TODO("Not yet implemented")
    }

    override fun antallFerdigstilteOppgaverFor(
        egenskap: EgenskapForDatabase,
        fom: LocalDate
    ): Int {
        TODO("Not yet implemented")
    }

    override fun getAntallAnnulleringer(fom: LocalDate): Int {
        TODO("Not yet implemented")
    }

    override fun getAntallAvvisninger(fom: LocalDate): Int {
        TODO("Not yet implemented")
    }
}
