package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.håndterOppdateringer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VedtaksperiodeTest {

    @Test
    fun `kan registrere observer`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(vedtaksperiodeId, generasjon(UUID.randomUUID(), vedtaksperiodeId))
        val observer = object : IVedtaksperiodeObserver {
            var tidslinjeOppdatert = false
            override fun tidslinjeOppdatert(
                generasjonId: UUID,
                fom: LocalDate,
                tom: LocalDate,
                skjæringstidspunkt: LocalDate
            ) {
                tidslinjeOppdatert = true
            }
        }
        vedtaksperiode.registrer(observer)
        vedtaksperiode.håndterTidslinjeendring(15.januar, 30.januar, 15.januar)
        assertTrue(observer.tidslinjeOppdatert)
    }

    @Test
    fun `håndterer oppdateringer`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiode1 = Vedtaksperiode(vedtaksperiodeId1, generasjon(generasjonId1, vedtaksperiodeId1))
        val vedtaksperiode2 = Vedtaksperiode(vedtaksperiodeId2, generasjon(generasjonId2, vedtaksperiodeId2))

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        vedtaksperiode1.registrer(observer)
        vedtaksperiode2.registrer(observer)

        listOf(vedtaksperiode1, vedtaksperiode2).håndterOppdateringer(
            listOf(
                VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId1),
                VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId2),
            )
        )
        assertEquals(2, observer.oppdaterteGenerasjoner.size)
        assertEquals(generasjonId1, observer.oppdaterteGenerasjoner[0])
        assertEquals(generasjonId2, observer.oppdaterteGenerasjoner[1])
    }

    @Test
    fun `håndterer oppdateringer for kun noen av vedtaksperiodene`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiode1 = Vedtaksperiode(vedtaksperiodeId1, generasjon(generasjonId1, vedtaksperiodeId1))
        val vedtaksperiode2 = Vedtaksperiode(vedtaksperiodeId2, generasjon(generasjonId2, vedtaksperiodeId2))

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        vedtaksperiode1.registrer(observer)
        vedtaksperiode2.registrer(observer)

        listOf(vedtaksperiode1, vedtaksperiode2).håndterOppdateringer(listOf(VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId2)))
        assertEquals(1, observer.oppdaterteGenerasjoner.size)
        assertEquals(generasjonId2, observer.oppdaterteGenerasjoner[0])
    }

    @Test
    fun `håndterer ikke oppdateringer for noen av vedtaksperiodene`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiode1 = Vedtaksperiode(vedtaksperiodeId1, generasjon(generasjonId1, vedtaksperiodeId1))
        val vedtaksperiode2 = Vedtaksperiode(vedtaksperiodeId2, generasjon(generasjonId2, vedtaksperiodeId2))

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        vedtaksperiode1.registrer(observer)
        vedtaksperiode2.registrer(observer)

        listOf(vedtaksperiode1, vedtaksperiode2).håndterOppdateringer(listOf(VedtaksperiodeOppdatering(1.januar, 31.januar, 1.januar, UUID.randomUUID())))
        assertEquals(0, observer.oppdaterteGenerasjoner.size)
    }

    @Test
    fun `referential equals`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(vedtaksperiodeId, generasjon(UUID.randomUUID(), vedtaksperiodeId))
        assertEquals(vedtaksperiode, vedtaksperiode)
    }

    @Test
    fun `structural equals`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val vedtaksperiode1 = Vedtaksperiode(vedtaksperiodeId, generasjon(generasjonId, vedtaksperiodeId))
        val vedtaksperiode2 = Vedtaksperiode(vedtaksperiodeId, generasjon(generasjonId, vedtaksperiodeId))
        assertEquals(vedtaksperiode1, vedtaksperiode2)
    }

    @Test
    fun `not equals - vedtaksperiodeId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val vedtaksperiode1 = Vedtaksperiode(UUID.randomUUID(), generasjon(generasjonId, vedtaksperiodeId))
        val vedtaksperiode2 = Vedtaksperiode(vedtaksperiodeId, generasjon(generasjonId, vedtaksperiodeId))
        assertNotEquals(vedtaksperiode1, vedtaksperiode2)
    }

    @Test
    fun `not equals - gjeldendeGenerasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode1 = Vedtaksperiode(vedtaksperiodeId, generasjon(UUID.randomUUID(), vedtaksperiodeId))
        val vedtaksperiode2 = Vedtaksperiode(vedtaksperiodeId, generasjon(UUID.randomUUID(), vedtaksperiodeId))
        assertNotEquals(vedtaksperiode1, vedtaksperiode2)
    }

    private fun generasjon(generasjonId: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )
}