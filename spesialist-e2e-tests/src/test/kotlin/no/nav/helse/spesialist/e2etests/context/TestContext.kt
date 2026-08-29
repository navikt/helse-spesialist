package no.nav.helse.spesialist.e2etests.context

import no.nav.helse.spesialist.domain.testfixtures.jan
import java.time.LocalDate
import java.util.UUID

data class TestContext(
    val person: Person = Person(),
    val arbeidsgivere: MutableList<Arbeidsgiver> = mutableListOf(Arbeidsgiver()),
    val vilkårsgrunnlagId: UUID = UUID.randomUUID(),
    val vedtaksperioder: MutableList<Vedtaksperiode> =
        mutableListOf(Vedtaksperiode(arbeidsgiver = arbeidsgivere.first())),
) {
    /** Snarvei for tester som kun forholder seg til én (den første) arbeidsgiveren. */
    val arbeidsgiver: Arbeidsgiver get() = arbeidsgivere.first()

    fun leggTilArbeidsgiver(arbeidsgiver: Arbeidsgiver = Arbeidsgiver()): Arbeidsgiver = arbeidsgiver.also(arbeidsgivere::add)

    fun leggTilVedtaksperiode(
        arbeidsgiver: Arbeidsgiver = this.arbeidsgiver,
        fom: LocalDate = 1 jan 2018,
        tom: LocalDate = fom.withDayOfMonth(fom.lengthOfMonth()),
        skjæringstidspunkt: LocalDate = fom,
    ): Vedtaksperiode =
        Vedtaksperiode(
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiver = arbeidsgiver,
        ).also(vedtaksperioder::add)

    fun vedtaksperioderFor(arbeidsgiver: Arbeidsgiver): List<Vedtaksperiode> = vedtaksperioder.filter { it.arbeidsgiver == arbeidsgiver }

    /** Arbeidsgivere uten egne vedtaksperioder ("ghost"-arbeidsgivere). */
    fun ghostArbeidsgivere(): List<Arbeidsgiver> = arbeidsgivere.filter { vedtaksperioderFor(it).isEmpty() }
}
