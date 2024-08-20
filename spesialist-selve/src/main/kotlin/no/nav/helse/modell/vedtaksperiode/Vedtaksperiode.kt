package no.nav.helse.modell.vedtaksperiode

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.vedtak.Avslag
import no.nav.helse.modell.vedtak.Avslagstype
import no.nav.helse.modell.vedtak.AvslagstypeDto
import no.nav.helse.modell.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjonForSpleisBehandling
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnSisteGenerasjonUtenSpleisBehandlingId
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.logg
import java.time.LocalDate
import java.util.UUID

internal class Vedtaksperiode private constructor(
    private val vedtaksperiodeId: UUID,
    private val organisasjonsnummer: String,
    private var forkastet: Boolean,
    generasjoner: List<Generasjon>,
) {
    private val generasjoner = generasjoner.toMutableList()
    private val gjeldendeGenerasjon get() = generasjoner.last()
    private val fom get() = gjeldendeGenerasjon.fom()
    private val tom get() = gjeldendeGenerasjon.tom()
    private val gjeldendeUtbetalingId get() = gjeldendeGenerasjon.utbetalingId
    internal val gjeldendeSkjæringstidspunkt get() = gjeldendeGenerasjon.skjæringstidspunkt()
    internal val gjeldendeBehandlingId get() = gjeldendeGenerasjon.spleisBehandlingId()

    fun vedtaksperiodeId() = vedtaksperiodeId

    fun organisasjonsnummer() = organisasjonsnummer

    internal fun toDto(): VedtaksperiodeDto {
        return VedtaksperiodeDto(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forkastet = forkastet,
            generasjoner = generasjoner.map { it.toDto() },
        )
    }

    internal fun behandleTilbakedateringGodkjent(perioder: List<Periode>) {
        if (forkastet || perioder.none { it.overlapperMed(Periode(fom, tom)) }) return
        deaktiverVarselMedKode("RV_SØ_3")
    }

    private fun deaktiverVarselMedKode(varselkode: String) {
        gjeldendeGenerasjon.deaktiverVarsel(varselkode)
    }

    internal fun nyttGodkjenningsbehov(spleisVedtaksperioder: List<SpleisVedtaksperiode>) {
        if (forkastet) return
        val spleisVedtaksperiode = spleisVedtaksperioder.find { it.erRelevant(vedtaksperiodeId) } ?: return
        gjeldendeGenerasjon.håndter(this, spleisVedtaksperiode)
    }

    internal fun nySpleisBehandling(spleisBehandling: SpleisBehandling) {
        if (forkastet || !spleisBehandling.erRelevantFor(vedtaksperiodeId) || finnes(spleisBehandling)) return
        nyGenerasjon(gjeldendeGenerasjon.nySpleisBehandling(spleisBehandling))
    }

    internal fun utbetalingForkastet(forkastetUtbetalingId: UUID) {
        if (forkastet) return
        val utbetalingId = gjeldendeUtbetalingId
        if (utbetalingId == null || gjeldendeUtbetalingId != forkastetUtbetalingId) return
        gjeldendeGenerasjon.håndterForkastetUtbetaling(utbetalingId)
    }

    internal fun nyGenerasjon(generasjon: Generasjon) {
        generasjoner.addLast(generasjon)
    }

    internal fun vedtakFattet(spleisBehandlingId: UUID) {
        if (forkastet) return
        // Finn den generasjonen som ble avsluttet, det kan ha blitt opprettet nye generasjoner etter at vedtak_fattet
        // ble sendt ut
        generasjoner.finnGenerasjonForSpleisBehandling(spleisBehandlingId)?.håndterVedtakFattet() ?: logg.error(
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
            generasjoner.finnGenerasjonForSpleisBehandling(avsluttetUtenVedtak.spleisBehandlingId())
                ?: generasjoner.finnSisteGenerasjonUtenSpleisBehandlingId().also {
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
        varsler.forEach { gjeldendeGenerasjon.håndterNyttVarsel(it) }
    }

    internal fun mottaBehandlingsinformasjon(
        tags: List<String>,
        spleisBehandlingId: UUID,
        utbetalingId: UUID,
    ) {
        if (forkastet) return
        gjeldendeGenerasjon.oppdaterBehandlingsinformasjon(tags, spleisBehandlingId, utbetalingId)
    }

    internal fun nyUtbetaling(utbetalingId: UUID) {
        if (forkastet) return
        gjeldendeGenerasjon.håndterNyUtbetaling(utbetalingId)
    }

    internal fun finnGenerasjon(spleisBehandlingId: UUID): Generasjon {
        return generasjoner.find { it.spleisBehandlingId() == spleisBehandlingId }
            ?: throw IllegalArgumentException("Forventer at generasjon med spleisBehandlingId=$spleisBehandlingId finnes")
    }

    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.organisasjonsnummer(organisasjonsnummer)
    }

    private fun finnes(spleisBehandling: SpleisBehandling): Boolean {
        return generasjoner.finnGenerasjonForSpleisBehandling(spleisBehandling.spleisBehandlingId) != null
    }

    companion object {
        fun nyVedtaksperiode(spleisBehandling: SpleisBehandling): Vedtaksperiode {
            return Vedtaksperiode(
                vedtaksperiodeId = spleisBehandling.vedtaksperiodeId,
                organisasjonsnummer = spleisBehandling.organisasjonsnummer,
                generasjoner =
                    listOf(
                        Generasjon(
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
        }

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
                generasjoner = generasjoner.map { it.tilGenerasjon() },
            )
        }

        internal fun List<Vedtaksperiode>.finnGenerasjon(spleisBehandlingId: UUID): Vedtaksperiode? {
            return find { vedtaksperiode ->
                vedtaksperiode.generasjoner.any { it.spleisBehandlingId() == spleisBehandlingId }
            }
        }

        internal fun List<Vedtaksperiode>.relevanteFor(skjæringstidspunkt: LocalDate) =
            filter { it.gjeldendeSkjæringstidspunkt == skjæringstidspunkt }
                .map { it.gjeldendeGenerasjon }

        private fun GenerasjonDto.tilGenerasjon(): Generasjon {
            return Generasjon.fraLagring(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                spleisBehandlingId = spleisBehandlingId,
                skjæringstidspunkt = skjæringstidspunkt,
                fom = fom,
                tom = tom,
                tilstand =
                    when (tilstand) {
                        TilstandDto.VedtakFattet -> Generasjon.VedtakFattet
                        TilstandDto.VidereBehandlingAvklares -> Generasjon.VidereBehandlingAvklares
                        TilstandDto.AvsluttetUtenVedtak -> Generasjon.AvsluttetUtenVedtak
                        TilstandDto.AvsluttetUtenVedtakMedVarsler -> Generasjon.AvsluttetUtenVedtakMedVarsler
                        TilstandDto.KlarTilBehandling -> Generasjon.KlarTilBehandling
                    },
                tags = tags.toList(),
                avslag =
                    avslag?.let {
                        Avslag(
                            type =
                                when (it.type) {
                                    AvslagstypeDto.AVSLAG -> Avslagstype.AVSLAG
                                    AvslagstypeDto.DELVIS_AVSLAG -> Avslagstype.DELVIS_AVSLAG
                                },
                            begrunnelse = it.begrunnelse,
                        )
                    },
                varsler =
                    varsler.map { varselDto ->
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
            )
        }
    }
}
