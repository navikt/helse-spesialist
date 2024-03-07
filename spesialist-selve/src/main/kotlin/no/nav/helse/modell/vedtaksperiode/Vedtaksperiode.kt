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

    internal fun toDto(): VedtaksperiodeDto {
        return VedtaksperiodeDto(vedtaksperiodeId, generasjoner.map { it.toDto() })
    }

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
                skjæringstidspunkt = skjæringstidspunkt,
                fom = fom,
                tom = tom,
                tilstand = when (tilstand) {
                    TilstandDto.Låst -> Generasjon.Låst
                    TilstandDto.Ulåst -> Generasjon.Ulåst
                    TilstandDto.AvsluttetUtenUtbetaling -> Generasjon.AvsluttetUtenUtbetaling
                    TilstandDto.UtenUtbetalingMåVurderes -> Generasjon.UtenUtbetalingMåVurderes
                },
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