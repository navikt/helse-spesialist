package no.nav.helse.spesialist.application

import no.nav.helse.db.LegacyVedtaksperiodeRepository
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Vedtaksperiode

class DelegatingLegacyVedtaksperiodeRepository(
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
    private val behandlingRepository: InMemoryBehandlingRepository,
    private val varselRepository: VarselRepository,
) : LegacyVedtaksperiodeRepository {
    override fun finnVedtaksperioder(fødselsnummer: String) =
        vedtaksperiodeRepository
            .alle()
            .filter { it.identitetsnummer.value == fødselsnummer }
            .map { vedtaksperiode -> vedtaksperiode.toVedtaksperiodeDto() }

    fun alle(): List<VedtaksperiodeDto> = vedtaksperiodeRepository.alle().map { it.toVedtaksperiodeDto() }

    override fun førsteKjenteDag(fødselsnummer: String) = finnVedtaksperioder(fødselsnummer).flatMap { it.behandlinger }.minOfOrNull { it.fom }

    private fun Vedtaksperiode.toVedtaksperiodeDto(): VedtaksperiodeDto =
        VedtaksperiodeDto(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = id.value,
            forkastet = forkastet,
            behandlinger =
                behandlingRepository
                    .alle()
                    .filter { it.vedtaksperiodeId == id }
                    .map { behandling ->
                        BehandlingDto(
                            id = behandling.id.value,
                            vedtaksperiodeId = behandling.vedtaksperiodeId.value,
                            utbetalingId = behandling.utbetalingId?.value,
                            spleisBehandlingId = behandling.spleisBehandlingId?.value,
                            skjæringstidspunkt = behandling.skjæringstidspunkt,
                            fom = behandling.fom,
                            tom = behandling.tom,
                            tilstand =
                                when (behandling.tilstand) {
                                    Behandling.Tilstand.VedtakFattet -> TilstandDto.VedtakFattet
                                    Behandling.Tilstand.VidereBehandlingAvklares -> TilstandDto.VidereBehandlingAvklares
                                    Behandling.Tilstand.AvsluttetUtenVedtak -> TilstandDto.AvsluttetUtenVedtak
                                    Behandling.Tilstand.AvsluttetUtenVedtakMedVarsler -> TilstandDto.AvsluttetUtenVedtakMedVarsler
                                    Behandling.Tilstand.KlarTilBehandling -> TilstandDto.KlarTilBehandling
                                },
                            tags = behandling.tags.toList(),
                            varsler =
                                varselRepository
                                    .finnVarslerFor(listOf(behandling.id))
                                    .map { varsel ->
                                        VarselDto(
                                            id = varsel.id.value,
                                            varselkode = varsel.kode,
                                            opprettet = varsel.opprettetTidspunkt,
                                            vedtaksperiodeId = id.value,
                                            status =
                                                when (varsel.status) {
                                                    Varsel.Status.AKTIV -> VarselStatusDto.AKTIV
                                                    Varsel.Status.INAKTIV -> VarselStatusDto.INAKTIV
                                                    Varsel.Status.GODKJENT -> VarselStatusDto.GODKJENT
                                                    Varsel.Status.VURDERT -> VarselStatusDto.VURDERT
                                                    Varsel.Status.AVVIST -> VarselStatusDto.AVVIST
                                                    Varsel.Status.AVVIKLET -> VarselStatusDto.AVVIKLET
                                                },
                                        )
                                    },
                            yrkesaktivitetstype = behandling.yrkesaktivitetstype,
                        )
                    },
        )
}
