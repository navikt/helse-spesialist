package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselStatusDto

internal class Vedtaksperiode(
    private val vedtaksperiodeId: UUID,
    generasjoner: List<Generasjon>
) {
    private val generasjoner = generasjoner.toMutableList()
    private val gjeldendeGenerasjon get() = generasjoner.last()
    private val fom get() = gjeldendeGenerasjon.fom()
    private val tom get() = gjeldendeGenerasjon.tom()

    internal fun toDto(): VedtaksperiodeDto {
        return VedtaksperiodeDto(vedtaksperiodeId, generasjoner.map { it.toDto() })
    }

    internal fun behandleTilbakedateringGodkjent(perioder: List<Periode>) {
        if (perioder.none { it.overlapperMed(Periode(fom, tom)) }) return
        deaktiverVarselMedKode("RV_SØ_3")
    }

    private fun deaktiverVarselMedKode(varselkode: String) {
        gjeldendeGenerasjon.deaktiverVarsel(varselkode)
    }

    internal fun mottaBehandlingsinformasjon(tags: List<String>, spleisBehandlingId: UUID) =
        gjeldendeGenerasjon.oppdaterBehandlingsinformasjon(tags, spleisBehandlingId)

    companion object {
        fun nyVedtaksperiode(
            vedtaksperiodeId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
        ): Vedtaksperiode {
            return Vedtaksperiode(
                vedtaksperiodeId = vedtaksperiodeId,
                generasjoner = listOf(Generasjon(UUID.randomUUID(), vedtaksperiodeId, fom, tom, skjæringstidspunkt))
            )
        }

        fun gjenopprett(
            vedtaksperiodeId: UUID,
            generasjoner: List<GenerasjonDto>,
        ): Vedtaksperiode {
            check(generasjoner.isNotEmpty()) { "En vedtaksperiode uten generasjoner skal ikke være mulig" }
            return Vedtaksperiode(
                vedtaksperiodeId = vedtaksperiodeId,
                generasjoner = generasjoner.map { it.tilGenerasjon() }
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
                tilstand = when (tilstand) {
                    TilstandDto.Låst -> Generasjon.Låst
                    TilstandDto.Ulåst -> Generasjon.Ulåst
                    TilstandDto.AvsluttetUtenUtbetaling -> Generasjon.AvsluttetUtenUtbetaling
                    TilstandDto.UtenUtbetalingMåVurderes -> Generasjon.UtenUtbetalingMåVurderes
                },
                tags = tags.toList(),
                varsler = varsler.map { varselDto ->
                    Varsel(
                        id = varselDto.id,
                        varselkode = varselDto.varselkode,
                        opprettet = varselDto.opprettet,
                        vedtaksperiodeId = varselDto.vedtaksperiodeId,
                        status = when (varselDto.status) {
                            VarselStatusDto.AKTIV -> Varsel.Status.AKTIV
                            VarselStatusDto.INAKTIV -> Varsel.Status.INAKTIV
                            VarselStatusDto.GODKJENT -> Varsel.Status.GODKJENT
                            VarselStatusDto.VURDERT -> Varsel.Status.VURDERT
                            VarselStatusDto.AVVIST -> Varsel.Status.AVVIST
                            VarselStatusDto.AVVIKLET -> Varsel.Status.AVVIKLET
                        }
                    )
                }.toSet()
            )
        }
    }
}
