package no.nav.helse.mediator.builders

import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GenerasjonBuilderTest {

    @Test
    fun bygg() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = now()
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        builder.generasjonId(generasjonId)
        builder.låst(false)
        builder.periode(1.januar, 31.januar)
        builder.skjæringstidspunkt(1.januar)
        builder.utbetalingId(utbetalingId)
        builder.varsler(listOf(Varsel(varselId, "EN_KODE", varselOpprettet, vedtaksperiodeId)))
        val generasjon = builder.build()
        val forventetGenerasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        forventetGenerasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        forventetGenerasjon.håndter(Varsel(varselId, "EN_KODE", varselOpprettet, vedtaksperiodeId))

        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `generasjonId må være satt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = now()
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        builder.låst(false)
        builder.periode(1.januar, 31.januar)
        builder.skjæringstidspunkt(1.januar)
        builder.utbetalingId(utbetalingId)
        builder.varsler(listOf(Varsel(varselId, "EN_KODE", varselOpprettet, vedtaksperiodeId)))

        assertThrows<UninitializedPropertyAccessException> {
            builder.build()
        }
    }

    @Test
    fun `låst må være satt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = now()
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        builder.generasjonId(generasjonId)
        builder.periode(1.januar, 31.januar)
        builder.skjæringstidspunkt(1.januar)
        builder.utbetalingId(utbetalingId)
        builder.varsler(listOf(Varsel(varselId, "EN_KODE", varselOpprettet, vedtaksperiodeId)))

        assertThrows<IllegalStateException> {
            builder.build()
        }
    }

    @Test
    fun `periode må være satt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = now()
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        builder.generasjonId(generasjonId)
        builder.låst(false)
        builder.skjæringstidspunkt(1.januar)
        builder.utbetalingId(utbetalingId)
        builder.varsler(listOf(Varsel(varselId, "EN_KODE", varselOpprettet, vedtaksperiodeId)))

        assertThrows<UninitializedPropertyAccessException> {
            builder.build()
        }
    }

    @Test
    fun `skjæringstidspunkt må være satt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = now()
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        builder.generasjonId(generasjonId)
        builder.låst(false)
        builder.periode(1.januar, 31.januar)
        builder.utbetalingId(utbetalingId)
        builder.varsler(listOf(Varsel(varselId, "EN_KODE", varselOpprettet, vedtaksperiodeId)))

        assertThrows<UninitializedPropertyAccessException> {
            builder.build()
        }
    }

    @Test
    fun `varsler trenger _ikke_ være satt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        builder.generasjonId(generasjonId)
        builder.låst(false)
        builder.periode(1.januar, 31.januar)
        builder.skjæringstidspunkt(1.januar)
        builder.utbetalingId(utbetalingId)

        assertDoesNotThrow {
            builder.build()
        }
    }

    @Test
    fun `utbetalingId trenger _ikke_ være satt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = now()
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        builder.generasjonId(generasjonId)
        builder.låst(false)
        builder.periode(1.januar, 31.januar)
        builder.skjæringstidspunkt(1.januar)
        builder.varsler(listOf(Varsel(varselId, "EN_KODE", varselOpprettet, vedtaksperiodeId)))

        assertDoesNotThrow {
            builder.build()
        }
    }

    private val varselRepository = object : VarselRepository {
        override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {}
        override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {}
        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime) {}
        override fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {}
    }
}