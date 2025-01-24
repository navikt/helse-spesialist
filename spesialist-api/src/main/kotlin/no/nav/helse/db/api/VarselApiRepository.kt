package no.nav.helse.db.api

import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.Varsel
import java.util.UUID

interface VarselApiRepository {
    fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDTO>

    fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDTO>

    fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDTO>

    fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDTO>

    fun godkjennVarslerFor(oppgaveId: Long)

    fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: Varsel.Varselstatus,
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
    ): VarselDTO?

    fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
    ): VarselDTO?

    fun perioderSomSkalViseVarsler(oppgaveId: Long?): Set<UUID>
}
