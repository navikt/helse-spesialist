package no.nav.helse.spesialist.application

import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.BehandlingsstatistikkDao.StatistikkPerKombinasjon
import no.nav.helse.db.EgenskapForDatabase
import java.time.LocalDate

class InMemoryBehandlingsstatistikkDao : BehandlingsstatistikkDao {
    override fun getAntallTilgjengeligeBeslutteroppgaver() = 1

    override fun getAntallTilgjengeligeEgenAnsattOppgaver() = 2

    override fun getAntallManueltFullførteEgenAnsattOppgaver(fom: LocalDate) = 3

    override fun getAntallFullførteBeslutteroppgaver(fom: LocalDate) = 4

    override fun getAutomatiseringPerKombinasjon(fom: LocalDate) = StatistikkPerKombinasjon(
        perInntekttype = emptyMap(),
        perPeriodetype = emptyMap(),
        perMottakertype = emptyMap(),
        perUtbetalingtype = emptyMap(),
    )

    override fun getTilgjengeligeOppgaverPerInntektOgPeriodetype() = StatistikkPerKombinasjon(
        perInntekttype = emptyMap(),
        perPeriodetype = emptyMap(),
        perMottakertype = emptyMap(),
        perUtbetalingtype = emptyMap(),
    )

    override fun getManueltUtførteOppgaverPerInntektOgPeriodetype(fom: LocalDate) = StatistikkPerKombinasjon(
        perInntekttype = emptyMap(),
        perPeriodetype = emptyMap(),
        perMottakertype = emptyMap(),
        perUtbetalingtype = emptyMap(),
    )

    override fun antallTilgjengeligeOppgaverFor(egenskap: EgenskapForDatabase) = 5

    override fun antallFerdigstilteOppgaverFor(egenskap: EgenskapForDatabase, fom: LocalDate) = 6

    override fun getAntallAnnulleringer(fom: LocalDate) = 7

    override fun getAntallAvvisninger(fom: LocalDate) = 8
}
