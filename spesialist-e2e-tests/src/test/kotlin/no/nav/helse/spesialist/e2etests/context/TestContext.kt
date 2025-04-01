package no.nav.helse.spesialist.e2etests.context

data class TestContext(
    val person: VårTestPerson = VårTestPerson(),
    val arbeidsgiver: VårArbeidsgiver = VårArbeidsgiver(),
    val vedtaksperioder: MutableList<VårVedtaksperiode> = mutableListOf(VårVedtaksperiode())
) {
    fun leggTilVedtaksperiode() {
        vedtaksperioder.add(VårVedtaksperiode())
    }
}
