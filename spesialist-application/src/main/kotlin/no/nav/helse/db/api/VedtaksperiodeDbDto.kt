package no.nav.helse.db.api

import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeDbDto(
    val vedtaksperiodeId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val varsler: Set<VarselDbDto>,
) {
    fun vedtaksperiodeId() = this.vedtaksperiodeId

    fun tidligereEnnOgSammenhengende(other: VedtaksperiodeDbDto): Boolean =
        this.fom <= other.tom && this.skjæringstidspunkt == other.skjæringstidspunkt

    private fun harAktiveVarsler(): Boolean {
        return varsler.any { it.erAktiv() }
    }

    companion object {
        fun Set<VedtaksperiodeDbDto>.harAktiveVarsler(): Boolean {
            return any { it.harAktiveVarsler() }
        }

        fun Set<VedtaksperiodeDbDto>.godkjennVarsler(
            fødselsnummer: String,
            behandlingId: UUID,
            ident: String,
            godkjenner: (
                fødselsnummer: String,
                behandlingId: UUID,
                vedtaksperiodeId: UUID,
                varselId: UUID,
                varselTittel: String,
                varselkode: String,
                forrigeStatus: VarselDbDto.Varselstatus,
                gjeldendeStatus: VarselDbDto.Varselstatus,
                saksbehandlerIdent: String,
            ) -> Unit,
        ) {
            forEach { vedtaksperiode ->
                vedtaksperiode.varsler.forEach {
                    it.vurder(
                        godkjent = true,
                        fødselsnummer = fødselsnummer,
                        behandlingId = behandlingId,
                        vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                        ident = ident,
                        vurderer = godkjenner,
                    )
                }
            }
        }

        fun VedtaksperiodeDbDto.avvisVarsler(
            fødselsnummer: String,
            behandlingId: UUID,
            ident: String,
            godkjenner: (
                fødselsnummer: String,
                behandlingId: UUID,
                vedtaksperiodeId: UUID,
                varselId: UUID,
                varselTittel: String,
                varselkode: String,
                forrigeStatus: VarselDbDto.Varselstatus,
                gjeldendeStatus: VarselDbDto.Varselstatus,
                saksbehandlerIdent: String,
            ) -> Unit,
        ) {
            this.varsler.forEach {
                it.vurder(
                    godkjent = false,
                    fødselsnummer = fødselsnummer,
                    behandlingId = behandlingId,
                    vedtaksperiodeId = this.vedtaksperiodeId,
                    ident = ident,
                    vurderer = godkjenner,
                )
            }
        }
    }
}
