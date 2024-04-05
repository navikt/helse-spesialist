package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.modell.person.Person
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselStatusDto
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtaksperiode.vedtak.SykepengevedtakBuilder
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
    private val gjeldendeUtbetalingId get() = gjeldendeGenerasjon.utbetalingId()

    fun vedtaksperiodeId() = vedtaksperiodeId

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

    internal fun håndter(spleisVedtaksperioder: List<SpleisVedtaksperiode>) {
        if (forkastet) return
        val spleisVedtaksperiode = spleisVedtaksperioder.find { it.erRelevant(vedtaksperiodeId) } ?: return
        gjeldendeGenerasjon.håndter(spleisVedtaksperiode)
    }

    internal fun nySpleisBehandling(spleisBehandling: SpleisBehandling) {
        if (forkastet || !spleisBehandling.erRelevantFor(vedtaksperiodeId)) return
        gjeldendeGenerasjon.nySpleisBehandling(this, spleisBehandling)
    }

    internal fun utbetalingForkastet(utbetalingEndret: UtbetalingEndret) {
        if (forkastet) return
        val utbetalingId = gjeldendeUtbetalingId
        if (utbetalingId == null || !utbetalingEndret.erRelevantFor(utbetalingId)) return
        gjeldendeGenerasjon.håndterForkastetUtbetaling(utbetalingId)
    }

    internal fun nyGenerasjon(generasjon: Generasjon) {
        generasjoner.addLast(generasjon)
    }

    internal fun vedtakFattet(meldingId: UUID) {
        if (forkastet) return
        gjeldendeGenerasjon.håndterVedtakFattet(meldingId)
    }

    internal fun avsluttetUtenVedtak(
        person: Person,
        avsluttetUtenVedtak: AvsluttetUtenVedtak,
    ) {
        if (forkastet) return
        val sykepengevedtakBuilder = SykepengevedtakBuilder()
        gjeldendeGenerasjon.avsluttetUtenVedtak(avsluttetUtenVedtak, sykepengevedtakBuilder)
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
        varsler.forEach { gjeldendeGenerasjon.håndterNyttVarsel(it, UUID.randomUUID()) }
    }

    internal fun mottaBehandlingsinformasjon(
        tags: List<String>,
        spleisBehandlingId: UUID,
    ) {
        if (forkastet) return
        gjeldendeGenerasjon.oppdaterBehandlingsinformasjon(tags, spleisBehandlingId)
    }

    internal fun nyUtbetaling(
        meldingId: UUID,
        utbetalingId: UUID,
    ) {
        if (forkastet) return
        gjeldendeGenerasjon.håndterNyUtbetaling(meldingId, utbetalingId)
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
                        TilstandDto.Låst -> Generasjon.Låst
                        TilstandDto.Åpen -> Generasjon.Åpen
                        TilstandDto.AvsluttetUtenUtbetaling -> Generasjon.AvsluttetUtenUtbetaling
                        TilstandDto.UtenUtbetalingMåVurderes -> Generasjon.UtenUtbetalingMåVurderes
                    },
                tags = tags.toList(),
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
