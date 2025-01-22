package no.nav.helse.db.api

import no.nav.helse.spesialist.api.varsel.Varsel
import java.time.LocalDateTime
import java.util.UUID

interface ApiVarselDao {
    fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<Varsel>

    fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<Varsel>

    fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<Varsel>

    fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<Varsel>

    fun godkjennVarslerFor(vedtaksperioder: List<UUID>): Int

    fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
        endretTidspunkt: LocalDateTime? = LocalDateTime.now(),
    ): Varsel?

    fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
        endretTidspunkt: LocalDateTime? = LocalDateTime.now(),
    ): Varsel?

    fun finnStatusFor(
        varselkode: String,
        generasjonId: UUID,
    ): Varsel.Varselstatus?

    fun finnVarslerFor(generasjonId: UUID): Set<Varsel>

    fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: Varsel.Varselstatus,
        saksbehandlerIdent: String,
    ): Int
}
