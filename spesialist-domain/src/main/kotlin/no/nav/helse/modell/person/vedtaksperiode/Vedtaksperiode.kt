package no.nav.helse.modell.person.vedtaksperiode

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.finnBehandlingForSpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.logg
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import java.time.LocalDate
import java.util.UUID

class Vedtaksperiode private constructor(
    private val vedtaksperiodeId: UUID,
    private val organisasjonsnummer: String,
    private var forkastet: Boolean,
    behandlinger: List<Behandling>,
) {
    private val behandlinger = behandlinger.toMutableList()
    private val gjeldendeBehandling get() = behandlinger.last()
    private val fom get() = gjeldendeBehandling.fom()
    private val tom get() = gjeldendeBehandling.tom()
    private val gjeldendeUtbetalingId get() = gjeldendeBehandling.utbetalingId
    internal val gjeldendeSkjæringstidspunkt get() = gjeldendeBehandling.skjæringstidspunkt()
    val gjeldendeUnikId get() = gjeldendeBehandling.unikId()

    fun vedtaksperiodeId() = vedtaksperiodeId

    fun organisasjonsnummer() = organisasjonsnummer

    internal fun toDto(): VedtaksperiodeDto =
        VedtaksperiodeDto(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forkastet = forkastet,
            behandlinger = behandlinger.map { it.toDto() },
        )

    internal fun erForkastet() = forkastet

    internal fun behandleTilbakedateringGodkjent(perioder: List<Periode>) {
        if (forkastet || perioder.none { it.overlapperMed(Periode(fom, tom)) }) return
        deaktiverVarselMedKode("RV_SØ_3")
    }

    private fun deaktiverVarselMedKode(
        @Suppress("SameParameterValue") varselkode: String,
    ) {
        gjeldendeBehandling.deaktiverVarsel(varselkode)
    }

    internal fun nyttGodkjenningsbehov(spleisVedtaksperioder: List<SpleisVedtaksperiode>) {
        if (forkastet) return
        val spleisVedtaksperiode = spleisVedtaksperioder.find { it.erRelevant(vedtaksperiodeId) } ?: return
        val aktuellBehandling = behandlinger.find { spleisVedtaksperiode.spleisBehandlingId == it.spleisBehandlingId() }
        if (aktuellBehandling == null) {
            logg.info(
                "Fant ikke behandling med {} for vedtaksperiode med {}",
                kv("behandlingId", spleisVedtaksperiode.spleisBehandlingId),
                kv("vedtaksperiodeId", spleisVedtaksperiode.vedtaksperiodeId),
            )
            return
        }
        aktuellBehandling.håndter(this, spleisVedtaksperiode)
    }

    internal fun nySpleisBehandling(spleisBehandling: SpleisBehandling) {
        if (forkastet || !spleisBehandling.erRelevantFor(vedtaksperiodeId) || finnes(spleisBehandling)) return
        nyBehandling(gjeldendeBehandling.nySpleisBehandling(spleisBehandling))
    }

    internal fun utbetalingForkastet(forkastetUtbetalingId: UUID) {
        if (forkastet) return
        val utbetalingId = gjeldendeUtbetalingId
        if (utbetalingId == null || gjeldendeUtbetalingId != forkastetUtbetalingId) return
        gjeldendeBehandling.håndterForkastetUtbetaling(utbetalingId)
    }

    private fun nyBehandling(behandling: Behandling) {
        behandlinger.addLast(behandling)
    }

    internal fun vedtaksperiodeForkastet() {
        forkastet = true
    }

    internal fun nyeVarsler(nyeVarsler: List<Varsel>) {
        val varsler = nyeVarsler.filter { it.erRelevantFor(vedtaksperiodeId) }
        if (forkastet || varsler.isEmpty()) return
        varsler.forEach { gjeldendeBehandling.håndterNyttVarsel(it) }
    }

    internal fun mottaBehandlingsinformasjon(
        tags: List<String>,
        spleisBehandlingId: UUID,
        utbetalingId: UUID,
    ) {
        if (forkastet) return
        gjeldendeBehandling.oppdaterBehandlingsinformasjon(tags, spleisBehandlingId, utbetalingId)
    }

    internal fun nyUtbetaling(utbetalingId: UUID) {
        if (forkastet) return
        gjeldendeBehandling.håndterNyUtbetaling(utbetalingId)
    }

    fun finnBehandling(spleisBehandlingId: UUID): Behandling =
        behandlinger.find { it.spleisBehandlingId() == spleisBehandlingId }
            ?: throw IllegalArgumentException("Forventer at behandling med spleisBehandlingId=$spleisBehandlingId finnes")

    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.organisasjonsnummer(organisasjonsnummer)
    }

    private fun finnes(spleisBehandling: SpleisBehandling): Boolean =
        behandlinger.finnBehandlingForSpleisBehandling(spleisBehandling.spleisBehandlingId) != null

    companion object {
        fun nyVedtaksperiode(spleisBehandling: SpleisBehandling): Vedtaksperiode =
            Vedtaksperiode(
                vedtaksperiodeId = spleisBehandling.vedtaksperiodeId,
                organisasjonsnummer = spleisBehandling.organisasjonsnummer,
                behandlinger =
                    listOf(
                        Behandling(
                            id = UUID.randomUUID(),
                            vedtaksperiodeId = spleisBehandling.vedtaksperiodeId,
                            spleisBehandlingId = spleisBehandling.spleisBehandlingId,
                            fom = spleisBehandling.fom,
                            tom = spleisBehandling.tom,
                            // Spleis sender oss ikke skjæringstidspunkt på dette tidspunktet
                            skjæringstidspunkt = spleisBehandling.fom,
                        ),
                    ),
                forkastet = false,
            )

        fun gjenopprett(
            organisasjonsnummer: String,
            vedtaksperiodeId: UUID,
            forkastet: Boolean,
            behandlinger: List<BehandlingDto>,
        ): Vedtaksperiode {
            check(behandlinger.isNotEmpty()) { "En vedtaksperiode uten behandlinger skal ikke være mulig" }
            return Vedtaksperiode(
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                forkastet = forkastet,
                behandlinger = behandlinger.map { it.tilBehandling() },
            )
        }

        internal fun List<Vedtaksperiode>.finnBehandling(spleisBehandlingId: UUID): Vedtaksperiode? =
            find { vedtaksperiode ->
                vedtaksperiode.behandlinger.any { it.spleisBehandlingId() == spleisBehandlingId }
            }

        internal fun List<Vedtaksperiode>.relevanteFor(skjæringstidspunkt: LocalDate) =
            filter { it.gjeldendeSkjæringstidspunkt == skjæringstidspunkt }
                .map { it.gjeldendeBehandling }

        private fun BehandlingDto.tilBehandling(): Behandling =
            Behandling.fraLagring(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                spleisBehandlingId = spleisBehandlingId,
                skjæringstidspunkt = skjæringstidspunkt,
                fom = fom,
                tom = tom,
                tilstand =
                    when (tilstand) {
                        TilstandDto.VedtakFattet -> Behandling.VedtakFattet
                        TilstandDto.VidereBehandlingAvklares -> Behandling.VidereBehandlingAvklares
                        TilstandDto.AvsluttetUtenVedtak -> Behandling.AvsluttetUtenVedtak
                        TilstandDto.AvsluttetUtenVedtakMedVarsler -> Behandling.AvsluttetUtenVedtakMedVarsler
                        TilstandDto.KlarTilBehandling -> Behandling.KlarTilBehandling
                    },
                tags = tags.toList(),
                varsler =
                    varsler
                        .map { varselDto ->
                            Varsel(
                                id = varselDto.id,
                                varselkode = varselDto.varselkode,
                                opprettet = varselDto.opprettet,
                                vedtaksperiodeId = varselDto.vedtaksperiodeId,
                                status =
                                    when (varselDto.status) {
                                        VarselStatusDto.AKTIV -> Varsel.Status.AKTIV
                                        VarselStatusDto.INAKTIV -> Varsel.Status.INAKTIV
                                        VarselStatusDto.GODKJENT -> Varsel.Status.GODKJENT
                                        VarselStatusDto.VURDERT -> Varsel.Status.VURDERT
                                        VarselStatusDto.AVVIST -> Varsel.Status.AVVIST
                                        VarselStatusDto.AVVIKLET -> Varsel.Status.AVVIKLET
                                    },
                            )
                        }.toSet(),
                vedtakBegrunnelse = vedtakBegrunnelse,
            )
    }
}
