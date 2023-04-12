package no.nav.helse.mediator.builders

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
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
        varselRepository.varselOpprettet( vedtaksperiodeId, generasjonId, varselId, "SB_EX_1", opprettet)
        val builder = VedtaksperiodeBuilder(vedtaksperiodeId)
        val vedtaksperiode = builder.build(generasjonRepository, varselRepository)
        val forventetVedtaksperiode = Vedtaksperiode(
            vedtaksperiodeId,
            Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar).also {
                it.håndter(Varsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId))
            }
        )
        assertEquals(
            forventetVedtaksperiode,
            vedtaksperiode
        )
    }
}