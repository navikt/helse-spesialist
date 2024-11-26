package no.nav.helse.modell.vedtaksperiode

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtak.SaksbehandlerVurdering
import no.nav.helse.modell.vedtak.SaksbehandlerVurderingDto
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.finnGenerasjonForSpleisBehandling
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.finnSisteGenerasjonUtenSpleisBehandlingId
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.logg
import java.time.LocalDate
import java.util.UUID

internal class Vedtaksperiode private constructor(
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
    internal val gjeldendeGenerasjonId get() = gjeldendeBehandling.unikId()

    fun vedtaksperiodeId() = vedtaksperiodeId

    fun organisasjonsnummer() = organisasjonsnummer

    internal fun toDto(): VedtaksperiodeDto =
        VedtaksperiodeDto(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forkastet = forkastet,
            generasjoner = behandlinger.map { it.toDto() },
        )

    internal fun behandleTilbakedateringGodkjent(perioder: List<Periode>) {
        if (forkastet || perioder.none { it.overlapperMed(Periode(fom, tom)) }) return
        deaktiverVarselMedKode("RV_SØ_3")
    }

    private fun deaktiverVarselMedKode(varselkode: String) {
        gjeldendeBehandling.deaktiverVarsel(varselkode)
    }

    internal fun nyttGodkjenningsbehov(spleisVedtaksperioder: List<SpleisVedtaksperiode>) {
        if (forkastet) return
        val spleisVedtaksperiode = spleisVedtaksperioder.find { it.erRelevant(vedtaksperiodeId) } ?: return
        gjeldendeBehandling.håndter(this, spleisVedtaksperiode)
    }

    internal fun nySpleisBehandling(spleisBehandling: SpleisBehandling) {
        if (forkastet || !spleisBehandling.erRelevantFor(vedtaksperiodeId) || finnes(spleisBehandling)) return
        nyGenerasjon(gjeldendeBehandling.nySpleisBehandling(spleisBehandling))
    }

    internal fun utbetalingForkastet(forkastetUtbetalingId: UUID) {
        if (forkastet) return
        val utbetalingId = gjeldendeUtbetalingId
        if (utbetalingId == null || gjeldendeUtbetalingId != forkastetUtbetalingId) return
        gjeldendeBehandling.håndterForkastetUtbetaling(utbetalingId)
    }

    internal fun nyGenerasjon(behandling: Behandling) {
        behandlinger.addLast(behandling)
    }

    internal fun vedtakFattet(spleisBehandlingId: UUID) {
        if (forkastet) return
        // Finn den generasjonen som ble avsluttet, det kan ha blitt opprettet nye generasjoner etter at vedtak_fattet
        // ble sendt ut
        behandlinger.finnGenerasjonForSpleisBehandling(spleisBehandlingId)?.håndterVedtakFattet() ?: logg.error(
            "Fant ikke generasjon for {} som kan håndtere vedtak_fattet",
            kv("spleisBehandlingId", spleisBehandlingId),
        )
    }

    internal fun avsluttetUtenVedtak(
        person: Person,
        avsluttetUtenVedtak: AvsluttetUtenVedtak,
    ) {
        if (forkastet) return
        val sykepengevedtakBuilder = SykepengevedtakBuilder()

        val relevantGenerasjon =
            behandlinger.finnGenerasjonForSpleisBehandling(avsluttetUtenVedtak.spleisBehandlingId())
                ?: behandlinger.finnSisteGenerasjonUtenSpleisBehandlingId().also {
                    if (it != null) {
                        logg.info(
                            "Fant ikke generasjon basert på {}, velger siste generasjon der spleisBehandlingId er null {}",
                            kv("spleisBehandlingId", avsluttetUtenVedtak.spleisBehandlingId()),
                            kv("unikId", it.unikId()),
                        )
                    }
                }

        if (relevantGenerasjon == null) {
            logg.error(
                "Fant ikke generasjon for {} som kan håndtere avsluttet_uten_vedtak",
                kv("spleisBehandlingId", avsluttetUtenVedtak.spleisBehandlingId()),
            )
            return
        }
        relevantGenerasjon.avsluttetUtenVedtak(avsluttetUtenVedtak, sykepengevedtakBuilder)
        sykepengevedtakBuilder
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
        avsluttetUtenVedtak
            .byggMelding(sykepengevedtakBuilder)
        person.supplerVedtakFattet(sykepengevedtakBuilder)
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

    internal fun finnGenerasjon(spleisBehandlingId: UUID): Behandling =
        behandlinger.find { it.spleisBehandlingId() == spleisBehandlingId }
            ?: throw IllegalArgumentException("Forventer at generasjon med spleisBehandlingId=$spleisBehandlingId finnes")

    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.organisasjonsnummer(organisasjonsnummer)
    }

    private fun finnes(spleisBehandling: SpleisBehandling): Boolean =
        behandlinger.finnGenerasjonForSpleisBehandling(spleisBehandling.spleisBehandlingId) != null

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
            generasjoner: List<GenerasjonDto>,
        ): Vedtaksperiode {
            check(generasjoner.isNotEmpty()) { "En vedtaksperiode uten generasjoner skal ikke være mulig" }
            return Vedtaksperiode(
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                forkastet = forkastet,
                behandlinger = generasjoner.map { it.tilGenerasjon() },
            )
        }

        internal fun List<Vedtaksperiode>.finnGenerasjon(spleisBehandlingId: UUID): Vedtaksperiode? =
            find { vedtaksperiode ->
                vedtaksperiode.behandlinger.any { it.spleisBehandlingId() == spleisBehandlingId }
            }

        internal fun List<Vedtaksperiode>.relevanteFor(skjæringstidspunkt: LocalDate) =
            filter { it.gjeldendeSkjæringstidspunkt == skjæringstidspunkt }
                .map { it.gjeldendeBehandling }

        private fun GenerasjonDto.tilGenerasjon(): Behandling =
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
                saksbehandlerVurdering =
                    saksbehandlerVurdering?.let {
                        when (it.vurdering) {
                            SaksbehandlerVurderingDto.VurderingDto.AVSLAG -> SaksbehandlerVurdering.Avslag(it.begrunnelse)
                            SaksbehandlerVurderingDto.VurderingDto.DELVIS_INNVILGELSE ->
                                SaksbehandlerVurdering.DelvisInnvilgelse(
                                    it.begrunnelse,
                                )
                            SaksbehandlerVurderingDto.VurderingDto.INNVILGELSE -> SaksbehandlerVurdering.Innvilgelse(it.begrunnelse)
                        }
                    },
            )
    }
}
