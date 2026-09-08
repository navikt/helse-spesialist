package no.nav.helse.modell.person.vedtaksperiode

import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import java.util.UUID

class LegacyVedtaksperiode(
    private val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    forkastet: Boolean,
    behandlinger: List<LegacyBehandling>,
) {
    var forkastet: Boolean = forkastet
        private set
    private val behandlinger = behandlinger.toMutableList()

    fun vedtaksperiodeId() = vedtaksperiodeId

    internal fun toDto(): VedtaksperiodeDto =
        VedtaksperiodeDto(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forkastet = forkastet,
            behandlinger = behandlinger.map { it.toDto() },
        )

    fun finnBehandling(spleisBehandlingId: UUID): LegacyBehandling =
        behandlinger.find { it.spleisBehandlingId() == spleisBehandlingId }
            ?: throw IllegalArgumentException("Forventer at behandling med spleisBehandlingId=$spleisBehandlingId finnes")

    companion object {
        fun gjenopprett(
            organisasjonsnummer: String,
            vedtaksperiodeId: UUID,
            forkastet: Boolean,
            behandlinger: List<BehandlingDto>,
        ): LegacyVedtaksperiode {
            check(behandlinger.isNotEmpty()) { "En vedtaksperiode uten behandlinger skal ikke være mulig" }
            return LegacyVedtaksperiode(
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                forkastet = forkastet,
                behandlinger = behandlinger.map { it.tilBehandling() },
            )
        }

        fun List<LegacyVedtaksperiode>.finnBehandling(spleisBehandlingId: UUID): LegacyVedtaksperiode? =
            find { vedtaksperiode ->
                vedtaksperiode.behandlinger.any { it.spleisBehandlingId() == spleisBehandlingId }
            }

        fun BehandlingDto.tilBehandling(): LegacyBehandling =
            LegacyBehandling.fraLagring(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                spleisBehandlingId = spleisBehandlingId,
                skjæringstidspunkt = skjæringstidspunkt,
                fom = fom,
                tom = tom,
                tilstand =
                    when (tilstand) {
                        TilstandDto.VedtakFattet -> LegacyBehandling.Tilstand.VedtakFattet
                        TilstandDto.VidereBehandlingAvklares -> LegacyBehandling.Tilstand.VidereBehandlingAvklares
                        TilstandDto.AvsluttetUtenVedtak -> LegacyBehandling.Tilstand.AvsluttetUtenVedtak
                        TilstandDto.AvsluttetUtenVedtakMedVarsler -> LegacyBehandling.Tilstand.AvsluttetUtenVedtakMedVarsler
                        TilstandDto.KlarTilBehandling -> LegacyBehandling.Tilstand.KlarTilBehandling
                    },
                tags = tags.toList(),
                varsler =
                    varsler
                        .map { varselDto ->
                            LegacyVarsel(
                                id = varselDto.id,
                                varselkode = varselDto.varselkode,
                                opprettet = varselDto.opprettet,
                                vedtaksperiodeId = varselDto.vedtaksperiodeId,
                                status =
                                    when (varselDto.status) {
                                        VarselStatusDto.AKTIV -> LegacyVarsel.Status.AKTIV
                                        VarselStatusDto.INAKTIV -> LegacyVarsel.Status.INAKTIV
                                        VarselStatusDto.GODKJENT -> LegacyVarsel.Status.GODKJENT
                                        VarselStatusDto.VURDERT -> LegacyVarsel.Status.VURDERT
                                        VarselStatusDto.AVVIST -> LegacyVarsel.Status.AVVIST
                                        VarselStatusDto.AVVIKLET -> LegacyVarsel.Status.AVVIKLET
                                    },
                            )
                        }.toSet(),
                yrkesaktivitetstype = yrkesaktivitetstype,
            )
    }
}
