package no.nav.helse.db

import no.nav.helse.spesialist.api.behandlingsstatistikk.StatistikkPerKombinasjon
import java.time.LocalDate

interface BehandlingsstatistikkDao {
    fun getAntallTilgjengeligeBeslutteroppgaver(): Int

    fun getAntallTilgjengeligeEgenAnsattOppgaver(): Int

    fun getAntallManueltFullførteEgenAnsattOppgaver(fom: LocalDate): Int

    fun getAntallFullførteBeslutteroppgaver(fom: LocalDate): Int

    fun getAutomatiseringPerKombinasjon(fom: LocalDate): StatistikkPerKombinasjon

    fun getTilgjengeligeOppgaverPerInntektOgPeriodetype(): StatistikkPerKombinasjon

    fun getManueltUtførteOppgaverPerInntektOgPeriodetype(fom: LocalDate): StatistikkPerKombinasjon

    fun antallTilgjengeligeOppgaverFor(egenskap: EgenskapForDatabase): Int

    fun antallFerdigstilteOppgaverFor(
        egenskap: EgenskapForDatabase,
        fom: LocalDate,
    ): Int

    fun getAntallAnnulleringer(fom: LocalDate): Int

    fun getAntallAvvisninger(fom: LocalDate): Int
}
