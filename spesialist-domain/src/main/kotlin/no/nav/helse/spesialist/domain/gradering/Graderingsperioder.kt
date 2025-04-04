package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.gradering.TilkommenInntekt.Companion.overlapperMed

class GraderingsperioderId(val fødselsnummer: String, val organisasjonsnummer: String)

class GraderingEndretEvent(
    val graderingsdifferanse: Graderingsdifferanse,
)

class Graderingsperioder private constructor(
    id: GraderingsperioderId,
    gjeldendeGraderingsperioder: List<TilkommenInntekt>,
) : AggregateRoot<GraderingsperioderId>(id) {
    private val _gjeldendeGraderingsperioder: MutableList<TilkommenInntekt> = gjeldendeGraderingsperioder.toMutableList()
    private val gjeldendeGraderingsperioder: List<TilkommenInntekt> get() = _gjeldendeGraderingsperioder.toList()

    private val hendelser = mutableListOf<GraderingEndretEvent>()

    fun konsumerDomenehendelser(): List<GraderingEndretEvent> = hendelser.toList().also { hendelser.clear() }

    fun endreGraderingsperiode(
        gammel: TilkommenInntekt,
        ny: TilkommenInntekt,
    ) {
        val kopi = _gjeldendeGraderingsperioder.toMutableList()
        kopi.forsøkÅFjernePeriode(gammel)
        kopi.forsøkÅLeggeTilPeriode(ny)
        _gjeldendeGraderingsperioder.erstattMed(kopi)

        hendelser.add(GraderingEndretEvent(ny differanseFra gammel))
    }

    fun leggTilGraderingsperiode(tilkommenInntekt: TilkommenInntekt) {
        _gjeldendeGraderingsperioder.forsøkÅLeggeTilPeriode(tilkommenInntekt)
        hendelser.add(
            GraderingEndretEvent(
                graderingsdifferanse =
                    Graderingsdifferanse(
                        nyeEllerEndredeInntekter = tilkommenInntekt.dagerTilPerioderMedBeløp(),
                        fjernedeInntekter = emptyList(),
                    ),
            ),
        )
    }

    fun fjernGraderingsperiode(tilkommenInntekt: TilkommenInntekt) {
        _gjeldendeGraderingsperioder.forsøkÅFjernePeriode(tilkommenInntekt)
        hendelser.add(
            GraderingEndretEvent(
                graderingsdifferanse =
                    Graderingsdifferanse(
                        nyeEllerEndredeInntekter = emptyList(),
                        fjernedeInntekter = tilkommenInntekt.dager.tilPerioder(),
                    ),
            ),
        )
    }

    companion object {
        fun ny(
            fødselsnummer: String,
            organisasjonsnummer: String,
        ) = Graderingsperioder(
            id = GraderingsperioderId(fødselsnummer, organisasjonsnummer),
            gjeldendeGraderingsperioder = emptyList(),
        )

        private fun MutableList<TilkommenInntekt>.erstattMed(ny: List<TilkommenInntekt>) {
            this.clear()
            this.addAll(ny)
        }

        private fun MutableList<TilkommenInntekt>.forsøkÅLeggeTilPeriode(tilkommenInntekt: TilkommenInntekt) {
            if (this overlapperMed tilkommenInntekt) error("Kan ikke overlappe med andre graderingsperioder for dette arbeidsforholdet")
            add(tilkommenInntekt)
        }

        private fun MutableList<TilkommenInntekt>.forsøkÅFjernePeriode(tilkommenInntekt: TilkommenInntekt) {
            if (!remove(tilkommenInntekt)) error("Graderingsperioden finnes ikke")
        }
    }
}
