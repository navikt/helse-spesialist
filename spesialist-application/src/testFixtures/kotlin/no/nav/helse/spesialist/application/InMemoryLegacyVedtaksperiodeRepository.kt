package no.nav.helse.spesialist.application

import no.nav.helse.db.LegacyVedtaksperiodeRepository
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class InMemoryLegacyVedtaksperiodeRepository(
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
    private val behandlingRepository: InMemoryBehandlingRepository,
    private val vedtakBegrunnelseRepository: InMemoryVedtakBegrunnelseRepository,
    private val varselRepository: VarselRepository,
) : LegacyVedtaksperiodeRepository {
    override fun finnVedtaksperioder(fødselsnummer: String) =
        vedtaksperiodeRepository.alle()
            .filter { it.fødselsnummer == fødselsnummer }
            .map { vedtaksperiode -> vedtaksperiode.toVedtaksperiodeDto() }

    override fun lagreVedtaksperioder(fødselsnummer: String, vedtaksperioder: List<VedtaksperiodeDto>) {
        vedtaksperiodeRepository.alle().filter { it.fødselsnummer == fødselsnummer }.forEach { vedtaksperiode ->
            behandlingRepository.alle().filter { it.vedtaksperiodeId == vedtaksperiode.id() }.forEach { behandling ->
                behandlingRepository.slett(behandling.id())
            }
            vedtaksperiodeRepository.slett(vedtaksperiode.id())
        }
        vedtaksperioder.forEach { vedtaksperiode ->
            val vedtaksperiodeId = VedtaksperiodeId(vedtaksperiode.vedtaksperiodeId)
            vedtaksperiodeRepository.lagre(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                    forkastet = vedtaksperiode.forkastet
                )
            )
            vedtaksperiode.behandlinger.forEach { behandling ->
                behandlingRepository.lagre(
                    Behandling.fraLagring(
                        id = BehandlingUnikId(behandling.id),
                        spleisBehandlingId = behandling.spleisBehandlingId?.let(::SpleisBehandlingId),
                        vedtaksperiodeId = vedtaksperiodeId,
                        utbetalingId = behandling.utbetalingId?.let(::UtbetalingId),
                        tags = behandling.tags.toSet(),
                        fom = behandling.fom,
                        tom = behandling.tom,
                        skjæringstidspunkt = behandling.skjæringstidspunkt,
                        søknadIder = emptySet(),
                        tilstand = when (behandling.tilstand) {
                            TilstandDto.VedtakFattet -> Behandling.Tilstand.VedtakFattet
                            TilstandDto.VidereBehandlingAvklares -> Behandling.Tilstand.VidereBehandlingAvklares
                            TilstandDto.AvsluttetUtenVedtak -> Behandling.Tilstand.AvsluttetUtenVedtak
                            TilstandDto.AvsluttetUtenVedtakMedVarsler -> Behandling.Tilstand.AvsluttetUtenVedtakMedVarsler
                            TilstandDto.KlarTilBehandling -> Behandling.Tilstand.KlarTilBehandling
                        },
                        yrkesaktivitetstype = behandling.yrkesaktivitetstype
                    )
                )
            }
        }
    }

    fun alle(): List<VedtaksperiodeDto> = vedtaksperiodeRepository.alle().map { it.toVedtaksperiodeDto() }

    override fun førsteKjenteDag(fødselsnummer: String) =
        finnVedtaksperioder(fødselsnummer).flatMap { it.behandlinger }.minOfOrNull { it.fom }

    private fun Vedtaksperiode.toVedtaksperiodeDto(): VedtaksperiodeDto =
        VedtaksperiodeDto(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = id().value,
            forkastet = forkastet,
            behandlinger = behandlingRepository.alle()
                .filter { it.vedtaksperiodeId == id() }
                .map { behandling ->
                    BehandlingDto(
                        id = behandling.id().value,
                        vedtaksperiodeId = behandling.vedtaksperiodeId.value,
                        utbetalingId = behandling.utbetalingId?.value,
                        spleisBehandlingId = behandling.spleisBehandlingId?.value,
                        skjæringstidspunkt = behandling.skjæringstidspunkt,
                        fom = behandling.fom,
                        tom = behandling.tom,
                        tilstand = when (behandling.tilstand) {
                            Behandling.Tilstand.VedtakFattet -> TilstandDto.VedtakFattet
                            Behandling.Tilstand.VidereBehandlingAvklares -> TilstandDto.VidereBehandlingAvklares
                            Behandling.Tilstand.AvsluttetUtenVedtak -> TilstandDto.AvsluttetUtenVedtak
                            Behandling.Tilstand.AvsluttetUtenVedtakMedVarsler -> TilstandDto.AvsluttetUtenVedtakMedVarsler
                            Behandling.Tilstand.KlarTilBehandling -> TilstandDto.KlarTilBehandling
                        },
                        tags = behandling.tags.toList(),
                        vedtakBegrunnelse = behandling.spleisBehandlingId
                            ?.let(vedtakBegrunnelseRepository::finn)
                            ?.let {
                                VedtakBegrunnelse(
                                    utfall = it.utfall,
                                    begrunnelse = it.tekst
                                )
                            },
                        varsler = varselRepository.finnVarslerFor(listOf(behandling.id()))
                            .map { varsel ->
                                VarselDto(
                                    id = varsel.id().value,
                                    varselkode = varsel.kode,
                                    opprettet = varsel.opprettetTidspunkt,
                                    vedtaksperiodeId = id().value,
                                    status = when (varsel.status) {
                                        Varsel.Status.AKTIV -> VarselStatusDto.AKTIV
                                        Varsel.Status.INAKTIV -> VarselStatusDto.INAKTIV
                                        Varsel.Status.GODKJENT -> VarselStatusDto.GODKJENT
                                        Varsel.Status.VURDERT -> VarselStatusDto.VURDERT
                                        Varsel.Status.AVVIST -> VarselStatusDto.AVVIST
                                        Varsel.Status.AVVIKLET -> VarselStatusDto.AVVIKLET
                                    }
                                )
                            },
                        yrkesaktivitetstype = behandling.yrkesaktivitetstype
                    )
                }
        )
}
