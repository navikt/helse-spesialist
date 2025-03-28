package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.gradering.Graderingsperiode.Companion.overlapperMed

class GraderingsperioderId(val fødselsnummer: String, val organisasjonsnummer: String)

class GraderingEndretEvent(
    val graderingsdifferanse: Graderingsdifferanse,
)

class Graderingsperioder private constructor(
    id: GraderingsperioderId,
    gjeldendeGraderingsperioder: List<Graderingsperiode>,
) : AggregateRoot<GraderingsperioderId>(id) {
    private val _gjeldendeGraderingsperioder: MutableList<Graderingsperiode> = gjeldendeGraderingsperioder.toMutableList()
    private val gjeldendeGraderingsperioder: List<Graderingsperiode> get() = _gjeldendeGraderingsperioder.toList()

    private val domenehendelser = mutableListOf<GraderingEndretEvent>()

    fun hendelser(): List<GraderingEndretEvent> = domenehendelser.toList().also { domenehendelser.clear() }

    fun endreGraderingsperiode(
        gammel: Graderingsperiode,
        ny: Graderingsperiode,
    ) {
        val kopi = _gjeldendeGraderingsperioder.toMutableList()
        kopi.forsøkÅFjernePeriode(gammel)
        kopi.forsøkÅLeggeTilPeriode(ny)
        _gjeldendeGraderingsperioder.erstattMed(kopi)

        domenehendelser.add(GraderingEndretEvent(ny differanseFra gammel))
    }

    fun leggTilGraderingsperiode(graderingsperiode: Graderingsperiode) {
        _gjeldendeGraderingsperioder.forsøkÅLeggeTilPeriode(graderingsperiode)
        domenehendelser.add(
            GraderingEndretEvent(
                graderingsdifferanse =
                    Graderingsdifferanse(
                        nyeEllerEndredeInntekter = graderingsperiode.dagerTilPerioderMedBeløp(),
                        fjernedeInntekter = emptyList(),
                    ),
            ),
        )
    }

    fun fjernGraderingsperiode(graderingsperiode: Graderingsperiode) {
        _gjeldendeGraderingsperioder.forsøkÅFjernePeriode(graderingsperiode)
        domenehendelser.add(
            GraderingEndretEvent(
                graderingsdifferanse =
                    Graderingsdifferanse(
                        nyeEllerEndredeInntekter = emptyList(),
                        fjernedeInntekter = graderingsperiode.graderteDager.tilPerioder(),
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

        private fun MutableList<Graderingsperiode>.erstattMed(ny: List<Graderingsperiode>) {
            this.clear()
            this.addAll(ny)
        }

        private fun MutableList<Graderingsperiode>.forsøkÅLeggeTilPeriode(graderingsperiode: Graderingsperiode) {
            if (this overlapperMed graderingsperiode) error("Kan ikke overlappe med andre graderingsperioder for dette arbeidsforholdet")
            add(graderingsperiode)
        }

        private fun MutableList<Graderingsperiode>.forsøkÅFjernePeriode(graderingsperiode: Graderingsperiode) {
            if (!remove(graderingsperiode)) error("Graderingsperioden finnes ikke")
        }
    }
}
