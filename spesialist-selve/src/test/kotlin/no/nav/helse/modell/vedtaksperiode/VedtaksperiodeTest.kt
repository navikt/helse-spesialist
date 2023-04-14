package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterOppdateringer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VedtaksperiodeTest {

    @Test
    fun `håndterer oppdateringer`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonV1 = generasjon(generasjonId1, vedtaksperiodeId1)
        val generasjonV2 = generasjon(generasjonId2, vedtaksperiodeId2)

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        generasjonV1.registrer(observer)
        generasjonV2.registrer(observer)

        listOf(generasjonV1, generasjonV2).håndterOppdateringer(
            listOf(
                VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId1),
                VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId2),
            ),
            UUID.randomUUID()
        )
        assertEquals(2, observer.oppdaterteGenerasjoner.size)
        assertEquals(generasjonId1, observer.oppdaterteGenerasjoner[0])
        assertEquals(generasjonId2, observer.oppdaterteGenerasjoner[1])
    }

    @Test
    fun `håndterer oppdateringer for kun noen av vedtaksperiodene`() {
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonV1 = generasjon(generasjonId1, UUID.randomUUID())
        val generasjonV2 = generasjon(generasjonId2, vedtaksperiodeId2)

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        generasjonV1.registrer(observer)
        generasjonV2.registrer(observer)

        listOf(generasjonV1, generasjonV2).håndterOppdateringer(
            listOf(VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId2)),
            UUID.randomUUID()
        )
        assertEquals(1, observer.oppdaterteGenerasjoner.size)
        assertEquals(generasjonId2, observer.oppdaterteGenerasjoner[0])
    }

    @Test
    fun `håndterer ikke oppdateringer for noen av vedtaksperiodene`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonV1 = generasjon(generasjonId1, UUID.randomUUID())
        val generasjonV2 = generasjon(generasjonId2, UUID.randomUUID())

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        generasjonV1.registrer(observer)
        generasjonV2.registrer(observer)

        listOf(generasjonV1, generasjonV2).håndterOppdateringer(
            listOf(VedtaksperiodeOppdatering(1.januar, 31.januar, 1.januar, UUID.randomUUID())),
            UUID.randomUUID()
        )
        assertEquals(0, observer.oppdaterteGenerasjoner.size)
    }

    private fun generasjon(generasjonId: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )
}