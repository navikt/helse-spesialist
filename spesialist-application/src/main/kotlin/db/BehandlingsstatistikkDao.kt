package no.nav.helse.db

import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
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

    data class StatistikkPerKombinasjon(
        val perInntekttype: Map<Inntektskilde, Int>,
        val perPeriodetype: Map<Periodetype, Int>,
        val perMottakertype: Map<Mottakertype, Int>,
        val perUtbetalingtype: Map<Utbetalingtype, Int>,
    ) {
        enum class Mottakertype {
            ARBEIDSGIVER,
            SYKMELDT,
            BEGGE,
        }

        enum class Utbetalingtype {
            ANNULLERING,
            ETTERUTBETALING,
            FERIEPENGER,
            REVURDERING,
            UTBETALING,
            UKJENT,
        }
    }
}
