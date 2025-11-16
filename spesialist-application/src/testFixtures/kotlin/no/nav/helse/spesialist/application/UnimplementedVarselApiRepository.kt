package no.nav.helse.spesialist.application

import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VarselDbDto
import java.util.UUID

class UnimplementedVarselApiRepository : VarselApiRepository {
    override fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID
    ): Set<VarselDbDto> {
        TODO("Not yet implemented")
    }

    override fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID
    ): Set<VarselDbDto> {
        TODO("Not yet implemented")
    }

    override fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> {
        TODO("Not yet implemented")
    }

    override fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> {
        TODO("Not yet implemented")
    }

    override fun godkjennVarslerFor(oppgaveId: Long) {
        TODO("Not yet implemented")
    }

    override fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: VarselDbDto.Varselstatus,
        saksbehandlerIdent: String
    ) {
        TODO("Not yet implemented")
    }

    override fun erAktiv(varselkode: String, generasjonId: UUID): Boolean? {
        TODO("Not yet implemented")
    }

    override fun erGodkjent(varselkode: String, generasjonId: UUID): Boolean? {
        TODO("Not yet implemented")
    }

    override fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String
    ): VarselDbDto? {
        TODO("Not yet implemented")
    }

    override fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String
    ): VarselDbDto? {
        TODO("Not yet implemented")
    }

    override fun perioderSomSkalViseVarsler(oppgaveId: Long?): Set<UUID> {
        TODO("Not yet implemented")
    }
}
