package no.nav.helse.mediator.builders

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VarselBuilderTest {

    @Test
    fun bygg() {
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val builder = VarselBuilder(generasjonId)
        builder.varselkode("EN_KODE")
        builder.varselId(varselId)
        builder.vedtaksperiodeId(vedtaksperiodeId)
        builder.opprettet(opprettet)
        builder.status(Varsel.Status.AKTIV)

        val varsel = builder.build()

        assertEquals(Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId, Varsel.Status.AKTIV), varsel)
    }

    @Test
    fun `varselkode må være satt`() {
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val builder = VarselBuilder(generasjonId)
        builder.varselId(varselId)
        builder.vedtaksperiodeId(vedtaksperiodeId)
        builder.opprettet(opprettet)
        builder.status(Varsel.Status.AKTIV)

        assertThrows<UninitializedPropertyAccessException> {
            builder.build()
        }
    }

    @Test
    fun `varselId må være satt`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val builder = VarselBuilder(generasjonId)
        builder.varselkode("EN_KODE")
        builder.vedtaksperiodeId(vedtaksperiodeId)
        builder.opprettet(opprettet)
        builder.status(Varsel.Status.AKTIV)

        assertThrows<UninitializedPropertyAccessException> {
            builder.build()
        }
    }

    @Test
    fun `vedtaksperiodeId må være satt`() {
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val builder = VarselBuilder(generasjonId)
        builder.varselkode("EN_KODE")
        builder.varselId(varselId)
        builder.opprettet(opprettet)
        builder.status(Varsel.Status.AKTIV)

        assertThrows<UninitializedPropertyAccessException> {
            builder.build()
        }
    }

    @Test
    fun `opprettet må være satt`() {
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val builder = VarselBuilder(generasjonId)
        builder.varselkode("EN_KODE")
        builder.varselId(varselId)
        builder.vedtaksperiodeId(vedtaksperiodeId)
        builder.status(Varsel.Status.AKTIV)

        assertThrows<UninitializedPropertyAccessException> {
            builder.build()
        }
    }

    @Test
    fun `status må være satt`() {
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val builder = VarselBuilder(generasjonId)
        builder.varselkode("EN_KODE")
        builder.varselId(varselId)
        builder.vedtaksperiodeId(vedtaksperiodeId)
        builder.opprettet(opprettet)

        assertThrows<UninitializedPropertyAccessException> {
            builder.build()
        }
    }
}