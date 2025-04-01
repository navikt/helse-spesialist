package no.nav.helse.spesialist.e2etests.context

data class TestContext(
    val person: Person = Person(),
    val arbeidsgiver: Arbeidsgiver = Arbeidsgiver(),
    val vedtaksperioder: MutableList<Vedtaksperiode> = mutableListOf(Vedtaksperiode())
) {
    fun leggTilVedtaksperiode() {
        vedtaksperioder.add(Vedtaksperiode())
    }
}
