package no.nav.helse.db.api

import java.util.UUID

interface VarselApiRepository {
    fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDbDto>

    fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDbDto>

    fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto>

    fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto>

    fun godkjennVarslerFor(oppgaveId: Long)

    fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: VarselDbDto.Varselstatus,
        saksbehandlerIdent: String,
    )

    fun erAktiv(
        varselkode: String,
        generasjonId: UUID,
    ): Boolean?

    fun erGodkjent(
        varselkode: String,
        generasjonId: UUID,
    ): Boolean?

    fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
    ): VarselDbDto?

    fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
    ): VarselDbDto?

    fun perioderSomSkalViseVarsler(oppgaveId: Long?): Set<UUID>
}
