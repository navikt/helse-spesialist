package no.nav.helse.mediator.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.håndter
import org.intellij.lang.annotations.Language
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
        varselRepository.varselOpprettet(vedtaksperiodeId, generasjonId, varselId, "SB_EX_1", opprettet)
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

    @Test
    fun `generasjonRepository blir registrert som observer`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        generasjonRepository.førsteGenerasjonOpprettet(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, 31.januar, 1.januar)
        varselRepository.varselOpprettet(vedtaksperiodeId, generasjonId, varselId, "SB_EX_1", opprettet)
        val builder = VedtaksperiodeBuilder(vedtaksperiodeId)
        val vedtaksperiode = builder.build(generasjonRepository, varselRepository)
        vedtaksperiode.håndterTidslinjeendring(1.mars, 31.mars, 1.mars, UUID.randomUUID())
        assertDatoer(generasjonId, 1.mars, 1.mars, 31.mars)
    }

    @Test
    fun `varselRepository blir registrert som observer`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        generasjonRepository.førsteGenerasjonOpprettet(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, 31.januar, 1.januar)
        val builder = VedtaksperiodeBuilder(vedtaksperiodeId)
        val vedtaksperiode = builder.build(generasjonRepository, varselRepository)
        listOf(vedtaksperiode).håndter(listOf(Varsel(varselId, "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)))
        assertVarsel(generasjonId, "SB_EX_1", vedtaksperiodeId, varselId)
    }

    private fun assertVarsel(generasjonId: UUID, forventetVarselkode: String, forventetVedtaksperiodeId: UUID, forventetVarselId: UUID) {
        @Language("PostgreSQL")
        val query = "SELECT kode, unik_id, vedtaksperiode_id FROM selve_varsel sv WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ?)"
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map {
                assertEquals(forventetVarselkode, it.string("kode"))
                assertEquals(forventetVedtaksperiodeId, it.uuid("vedtaksperiode_id"))
                assertEquals(forventetVarselId, it.uuid("unik_id"))
            }.asSingle)
        }
    }

    private fun assertDatoer(generasjonId: UUID, forventetSkjæringstidspunkt: LocalDate, forventetFom: LocalDate, forventetTom: LocalDate) {
        @Language("PostgreSQL")
        val query = "SELECT fom, tom, skjæringstidspunkt FROM selve_vedtaksperiode_generasjon svg WHERE unik_id = ?"
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map {
                assertEquals(forventetSkjæringstidspunkt, it.localDate("skjæringstidspunkt"))
                assertEquals(forventetFom, it.localDate("fom"))
                assertEquals(forventetTom, it.localDate("tom"))
            }.asSingle)
        }
    }
}