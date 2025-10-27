package no.nav.helse.modell.person.vedtaksperiode

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.finnBehandlingForSpleisBehandling
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.logg
import java.time.LocalDate
import java.util.UUID

class LegacyVedtaksperiode(
    private val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    private var forkastet: Boolean,
    behandlinger: List<LegacyBehandling>,
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

    fun behandlinger() = behandlinger.map { BehandlingData(it.vedtaksperiodeId(), it.spleisBehandlingId()) }

    fun råBehandlinger(): List<LegacyBehandling> = behandlinger

    internal fun toDto(): VedtaksperiodeDto =
        VedtaksperiodeDto(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forkastet = forkastet,
            behandlinger = behandlinger.map { it.toDto() },
        )

    internal fun erForkastet() = forkastet

    internal fun behandleTilbakedateringGodkjent(perioder: List<Periode>) {
        if (forkastet || perioder.none { it.overlapper(Periode(fom, tom)) }) return
        logg.info(
            "Godkjent tilbakedatert sykmelding overlapper med vedtaksperiode med fom=$fom, tom=$tom - perioder i sykmeldingen: $perioder",
        )
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

    private fun nyBehandling(legacyBehandling: LegacyBehandling) {
        behandlinger.addLast(legacyBehandling)
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

    fun finnBehandling(spleisBehandlingId: UUID): LegacyBehandling =
        behandlinger.find { it.spleisBehandlingId() == spleisBehandlingId }
            ?: throw IllegalArgumentException("Forventer at behandling med spleisBehandlingId=$spleisBehandlingId finnes")

    private fun finnes(spleisBehandling: SpleisBehandling): Boolean = behandlinger.finnBehandlingForSpleisBehandling(spleisBehandling.spleisBehandlingId) != null

    companion object {
        fun nyVedtaksperiode(spleisBehandling: SpleisBehandling): LegacyVedtaksperiode =
            LegacyVedtaksperiode(
                vedtaksperiodeId = spleisBehandling.vedtaksperiodeId,
                organisasjonsnummer = spleisBehandling.organisasjonsnummer,
                behandlinger =
                    listOf(
                        LegacyBehandling(
                            id = UUID.randomUUID(),
                            vedtaksperiodeId = spleisBehandling.vedtaksperiodeId,
                            spleisBehandlingId = spleisBehandling.spleisBehandlingId,
                            fom = spleisBehandling.fom,
                            tom = spleisBehandling.tom,
                            yrkesaktivitetstype = spleisBehandling.yrkesaktivitetstype,
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

        internal fun List<LegacyVedtaksperiode>.relevanteFor(skjæringstidspunkt: LocalDate) =
            filter { it.gjeldendeSkjæringstidspunkt == skjæringstidspunkt }
                .map { it.gjeldendeBehandling }

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
                        TilstandDto.VedtakFattet -> LegacyBehandling.VedtakFattet
                        TilstandDto.VidereBehandlingAvklares -> LegacyBehandling.VidereBehandlingAvklares
                        TilstandDto.AvsluttetUtenVedtak -> LegacyBehandling.AvsluttetUtenVedtak
                        TilstandDto.AvsluttetUtenVedtakMedVarsler -> LegacyBehandling.AvsluttetUtenVedtakMedVarsler
                        TilstandDto.KlarTilBehandling -> LegacyBehandling.KlarTilBehandling
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
                yrkesaktivitetstype = yrkesaktivitetstype,
            )
    }

    data class BehandlingData(
        val vedtaksperiodeId: UUID,
        val spleisBehandlingId: UUID?,
    )
}
