package no.nav.helse.spesialist.api.varsel

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao

class VarselService {

    internal fun uberegnedePerioderSomSkalViseVarsler(
        generasjon: GraphQLGenerasjon,
        oppgaveApiDao: OppgaveApiDao,
    ): Set<UUID> {
        val periodeMedOppgave = generasjon.perioder.singleOrNull {
            it.periodetilstand == GraphQLPeriodetilstand.TILGODKJENNING &&
            oppgaveApiDao.finnPeriodeoppgave(UUID.fromString(it.vedtaksperiodeId)) != null
        } ?: return emptySet()
        return generasjon.finnPerioderRettFør(periodeMedOppgave)
    }

    private fun GraphQLGenerasjon.finnPerioderRettFør(periode: GraphQLTidslinjeperiode) =
        this.finnPerioderRettFør(periode, emptySet())

    private fun GraphQLGenerasjon.finnPerioderRettFør(
        periode: GraphQLTidslinjeperiode,
        perioderFør: Set<GraphQLTidslinjeperiode>,
    ): Set<UUID> {
        perioder.firstOrNull { other ->
            other.erPeriodeRettFør(periode)
        }?.also {
            return finnPerioderRettFør(it, perioderFør + setOf(it))
        }
        return perioderFør.map { UUID.fromString(it.vedtaksperiodeId) }.toSet()
    }

    private fun GraphQLTidslinjeperiode.erPeriodeRettFør(
        periode: GraphQLTidslinjeperiode,
    ): Boolean =
        LocalDate.parse(this.tom).erRettFør(LocalDate.parse(periode.fom))

    private fun LocalDate.erRettFør(other: LocalDate): Boolean {
        if (this >= other) return false
        if (this.nesteDag == other) return true
        return when (this.dayOfWeek) {
            DayOfWeek.FRIDAY -> other in this.plusDays(2)..this.plusDays(3)
            DayOfWeek.SATURDAY -> other == this.plusDays(2)
            else -> false
        }
    }

    private val LocalDate.nesteDag get() = this.plusDays(1)
}