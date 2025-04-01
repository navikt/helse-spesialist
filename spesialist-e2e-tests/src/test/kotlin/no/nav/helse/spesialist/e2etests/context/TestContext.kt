package no.nav.helse.spesialist.e2etests.context

import java.util.UUID

data class TestContext(
    val person: Person = Person(),
    val arbeidsgiver: Arbeidsgiver = Arbeidsgiver(),
    val vilkårsgrunnlagId: UUID = UUID.randomUUID(),
    val vedtaksperioder: MutableList<Vedtaksperiode> = mutableListOf(Vedtaksperiode()),
) {
    fun leggTilVedtaksperiode() {
        vedtaksperioder.add(Vedtaksperiode())
    }
}
