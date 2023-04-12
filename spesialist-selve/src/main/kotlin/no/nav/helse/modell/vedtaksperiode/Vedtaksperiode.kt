package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel

internal class Vedtaksperiode(
    private val vedtaksperiodeId: UUID,
    private val gjeldendeGenerasjon: Generasjon,
) {

    private val observers = mutableListOf<IVedtaksperiodeObserver>()

    internal fun registrer(vararg observer: IVedtaksperiodeObserver) {
        observers.addAll(observer)
        gjeldendeGenerasjon.registrer(*observer)
    }

    internal fun håndterTidslinjeendring(fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate, hendelseId: UUID) {
        gjeldendeGenerasjon.håndterTidslinjeendring(fom, tom, skjæringstidspunkt, hendelseId)
    }

    private fun tilhører(dato: LocalDate): Boolean {
        return gjeldendeGenerasjon.tilhører(dato)
    }

    private fun harAktiveVarsler(): Boolean {
        return gjeldendeGenerasjon.harAktiveVarsler()
    }

    private fun håndter(varsler: List<Varsel>) {
        varsler
            .filter { it.erRelevantFor(vedtaksperiodeId) }
            .forEach { gjeldendeGenerasjon.håndter(it) }
    }

    private fun deaktiver(varsel: Varsel) {
        gjeldendeGenerasjon.håndterDeaktivertVarsel(varsel)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Vedtaksperiode
                && javaClass == other.javaClass
                && vedtaksperiodeId == other.vedtaksperiodeId
                && gjeldendeGenerasjon == other.gjeldendeGenerasjon
                )

    override fun hashCode(): Int {
        var result = vedtaksperiodeId.hashCode()
        result = 31 * result + gjeldendeGenerasjon.hashCode()
        return result
    }

    internal companion object {
        internal fun List<Vedtaksperiode>.håndterOppdateringer(
            vedtaksperiodeoppdateringer: List<VedtaksperiodeOppdatering>,
            hendelseId: UUID
        ) {
            forEach { vedtaksperiode ->
                val oppdatering = vedtaksperiodeoppdateringer.find { it.vedtaksperiodeId == vedtaksperiode.vedtaksperiodeId } ?: return@forEach
                vedtaksperiode.håndterTidslinjeendring(oppdatering.fom, oppdatering.tom, oppdatering.skjæringstidspunkt, hendelseId)
            }
        }

        internal fun List<Vedtaksperiode>.harAktiveVarsler(tilOgMed: LocalDate): Boolean {
            return this.filter { it.tilhører(tilOgMed) }.any { it.harAktiveVarsler() }
        }

        internal fun List<Vedtaksperiode>.håndter(varsler: List<Varsel>) {
            forEach { it.håndter(varsler) }
        }

        internal fun List<Vedtaksperiode>.deaktiver(varsel: Varsel) {
            find { varsel.erRelevantFor(it.vedtaksperiodeId) }?.deaktiver(varsel)
        }
    }
}