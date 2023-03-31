package no.nav.helse.mediator.builders

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VedtaksperiodeBuilderTest : AbstractDatabaseTest() {
    private val varselRepository = ActualVarselRepository(dataSource)
    private val generasjonRepository = ActualGenerasjonRepository(dataSource)

    @Test
    fun bygg() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        generasjonRepository.førsteGenerasjonOpprettet(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, 31.januar, 1.januar)
        varselRepository.lagreVarsel(varselId, generasjonId, "SB_EX_1", opprettet, vedtaksperiodeId)
        val builder = VedtaksperiodeBuilder(vedtaksperiodeId)
        val vedtaksperiode = builder.build(generasjonRepository, varselRepository)
        val forventetVedtaksperiode = Vedtaksperiode(
            vedtaksperiodeId,
            Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar).also {
                it.håndterRegelverksvarsel(UUID.randomUUID(), varselId, "SB_EX_1", opprettet, dummyVarselRepository)
            }
        )
        assertEquals(
            forventetVedtaksperiode,
            vedtaksperiode
        )
    }

    private val dummyVarselRepository = object : VarselRepository {
        override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {}
        override fun reaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String) {}
        override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {}
        override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {}
        override fun lagreVarsel(id: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {}
        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime) {}
        override fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {}
    }
}