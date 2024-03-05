package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GenerasjonVisitorTest {
    private val visitor = object: GenerasjonVisitor {
        lateinit var vedtaksperiodeId: UUID
        lateinit var id: UUID
        var utbetalingId: UUID? = null
        lateinit var skjæringstidspunkt: LocalDate
        lateinit var fom: LocalDate
        lateinit var tom: LocalDate
        lateinit var tilstand: Generasjon.Tilstand
        override fun visitGenerasjon(
            vedtaksperiodeId: UUID,
            id: UUID,
            utbetalingId: UUID?,
            skjæringstidspunkt: LocalDate,
            fom: LocalDate,
            tom: LocalDate,
            tilstand: Generasjon.Tilstand
        ) {
            this.vedtaksperiodeId = vedtaksperiodeId
            this.id = id
            this.utbetalingId = utbetalingId
            this.skjæringstidspunkt = skjæringstidspunkt
            this.fom = fom
            this.tom = tom
            this.tilstand = tilstand
        }
    }

    @Test
    fun `felter sendes ut riktig av generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val skjæringstidspunkt = 1.januar
        val fom = 2.januar
        val tom = 31.januar
        val generasjon = Generasjon.nyVedtaksperiode(id, vedtaksperiodeId, fom, tom, skjæringstidspunkt)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjon.accept(visitor)

        assertEquals(vedtaksperiodeId, visitor.vedtaksperiodeId)
        assertEquals(id, visitor.id)
        assertEquals(utbetalingId, visitor.utbetalingId)
        assertEquals(fom, visitor.fom)
        assertEquals(tom, visitor.tom)
        assertEquals(skjæringstidspunkt, visitor.skjæringstidspunkt)
        assertEquals(Generasjon.Ulåst, visitor.tilstand)
    }
}