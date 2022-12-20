package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.VarselRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GenerasjonTest {
    private val varsler = mutableListOf<String>()
    private val godkjenteVarsler = mutableMapOf<UUID, MutableList<String>>()
    private val avvisteVarsler = mutableListOf<String>()
    private val deaktiverteVarsler = mutableListOf<String>()
    private val generasjonerMedUtbetaling = mutableMapOf<UUID, UUID>()
    private val låsteGenerasjoner = mutableMapOf<UUID, UUID>()
    private val generasjoner = mutableMapOf<UUID, Generasjon>()
    private lateinit var generasjonId: UUID

    @BeforeEach
    internal fun beforeEach() {
        varsler.clear()
        godkjenteVarsler.clear()
        avvisteVarsler.clear()
        deaktiverteVarsler.clear()
    }

    @Test
    fun `godkjenner enkelt varsel`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterGodkjentVarsel("SB_EX_1", "EN_IDENT", varselRepository)
        assertEquals(1, godkjenteVarsler.size)
        assertEquals("SB_EX_1", godkjenteVarsler[generasjonId]?.get(0))
    }

    @Test
    fun `deaktiverer enkelt varsel`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterDeaktivertVarsel("SB_EX_1", varselRepository)
        assertEquals(1, deaktiverteVarsler.size)
        assertEquals("SB_EX_1", deaktiverteVarsler[0])
    }
    @Test
    fun `godkjenner alle varsler når generasjonen blir godkjent`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterNyUtbetaling(UUID.randomUUID())
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), varselRepository)
        generasjon.håndterGodkjentAvSaksbehandler("EN_IDENT", varselRepository)
        assertEquals(2, godkjenteVarsler[generasjonId]?.size)
        assertEquals("SB_EX_1", godkjenteVarsler[generasjonId]?.get(0))
        assertEquals("SB_EX_2", godkjenteVarsler[generasjonId]?.get(1))
    }

    @Test
    fun `avviser alle varsler når generasjonen blir avvist`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), varselRepository)
        generasjon.håndterAvvistAvSaksbehandler("EN_IDENT", varselRepository)
        assertEquals(2, avvisteVarsler.size)
        assertEquals("SB_EX_1", avvisteVarsler[0])
        assertEquals("SB_EX_2", avvisteVarsler[1])
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselRepository)
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselRepository)

        assertEquals(1, varsler.size)
    }

    @Test
    fun `Generasjon kan motta ny utbetalingId`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(utbetalingId)
        assertEquals(utbetalingId, generasjonerMedUtbetaling[generasjonId])
    }

    @Test
    fun `Generasjon kan motta ny utbetalingId så lenge generasjonen ikke er låst`() {
        val generasjon = nyGenerasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(gammelUtbetalingId)
        generasjon.håndterNyUtbetaling(nyUtbetalingId)
        assertEquals(nyUtbetalingId, generasjonerMedUtbetaling[generasjonId])
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som har utbetalingId fra før`() {
        val generasjon = nyGenerasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(gammelUtbetalingId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(nyUtbetalingId)
        assertEquals(gammelUtbetalingId, generasjonerMedUtbetaling[generasjonId])
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som ikke har utbetalingId fra før`() {
        val generasjon = nyGenerasjon()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(nyUtbetalingId)
        assertEquals(null, generasjonerMedUtbetaling[generasjonId])
    }

    @Test
    fun `kan fjerne utbetalingId fra ulåst generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(utbetalingId)
        assertEquals(utbetalingId, generasjonerMedUtbetaling[generasjonId])
        generasjon.invaliderUtbetaling(utbetalingId)
        assertEquals(null, generasjonerMedUtbetaling[generasjonId])
    }

    @Test
    fun `kan ikke fjerne utbetalingId fra låst generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(utbetalingId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(utbetalingId, generasjonerMedUtbetaling[generasjonId])
        generasjon.invaliderUtbetaling(utbetalingId)
        assertEquals(utbetalingId, generasjonerMedUtbetaling[generasjonId])
    }

    @Test
    fun `godkjenner varsler for alle generasjoner som hører til samme utbetaling`() {
        val generasjonIdV1 = UUID.randomUUID()
        val generasjonIdV2 = UUID.randomUUID()
        val generasjonV1 = nyGenerasjon(generasjonIdV1)
        val generasjonV2 = nyGenerasjon(generasjonIdV2)
        val utbetalingId = UUID.randomUUID()
        generasjonV1.håndterNyUtbetaling(utbetalingId)
        generasjonV2.håndterNyUtbetaling(utbetalingId)
        generasjonV1.håndterNyttVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselRepository)
        generasjonV2.håndterNyttVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), varselRepository)

        generasjonV2.håndterGodkjentAvSaksbehandler("EN_IDENT", varselRepository)
        assertEquals(utbetalingId, generasjonerMedUtbetaling[generasjonIdV1])
        assertEquals(utbetalingId, generasjonerMedUtbetaling[generasjonIdV2])

        assertEquals("EN_KODE", godkjenteVarsler[generasjonIdV1]?.get(0))
        assertEquals("EN_ANNEN_KODE", godkjenteVarsler[generasjonIdV2]?.get(0))
    }

    @Test
    fun `referential equals`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), generasjonRepository)
        assertEquals(generasjon, generasjon)
        assertEquals(generasjon.hashCode(), generasjon.hashCode())
    }

    @Test
    fun `structural equals`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        assertEquals(generasjon1, generasjon2)
        assertEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig generasjonIder`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId1, vedtaksperiodeId, generasjonRepository)
        val generasjon2 = Generasjon(generasjonId2, vedtaksperiodeId, generasjonRepository)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellige vedtaksperiodeIder`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId1, generasjonRepository)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId2, generasjonRepository)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig låst`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        generasjon1.håndterVedtakFattet(UUID.randomUUID())
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    private fun nyGenerasjon(id: UUID = UUID.randomUUID()): Generasjon {
        generasjonId = id
        return Generasjon(generasjonId, UUID.randomUUID(), generasjonRepository).also {
            generasjoner[id] = it
        }
    }

    private val varselRepository = object : VarselRepository {
        override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {
            deaktiverteVarsler.add(varselkode)
        }

        override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            godkjenteVarsler.getOrPut(generasjonId) { mutableListOf() }.add(varselkode)
        }

        override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            avvisteVarsler.add(varselkode)
        }

        override fun lagreVarsel(id: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
            varsler.add(varselkode)
        }

        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime): Unit = TODO("Not yet implemented")
    }

    private val generasjonRepository = object : GenerasjonRepository {
        override fun opprettFørste(vedtaksperiodeId: UUID, hendelseId: UUID, id: UUID): Generasjon = TODO("Not yet implemented")
        override fun opprettNeste(id: UUID, vedtaksperiodeId: UUID, hendelseId: UUID): Generasjon = TODO("Not yet implemented")
        override fun låsFor(generasjonId: UUID, hendelseId: UUID) {
            låsteGenerasjoner[generasjonId] = hendelseId
        }
        override fun utbetalingFor(generasjonId: UUID, utbetalingId: UUID) {
            generasjonerMedUtbetaling[generasjonId] = utbetalingId
        }
        override fun sisteFor(vedtaksperiodeId: UUID): Generasjon = TODO("Not yet implemented")
        override fun tilhørendeFor(utbetalingId: UUID): List<Generasjon> {
            return generasjonerMedUtbetaling
                .filterValues { it == utbetalingId }
                .mapNotNull { (key, _) -> generasjoner[key] }
        }

        override fun fjernUtbetalingFor(generasjonId: UUID) {
            generasjonerMedUtbetaling.remove(generasjonId)
        }
    }
}