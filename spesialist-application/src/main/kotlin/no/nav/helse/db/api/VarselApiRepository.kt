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

    fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: VarselDbDto.Varselstatus,
        saksbehandlerIdent: String,
    )

    fun perioderSomSkalViseVarsler(oppgaveId: Long?): Set<UUID>
}
