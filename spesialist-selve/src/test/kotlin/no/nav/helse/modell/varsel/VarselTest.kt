package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Status.VURDERT
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VarselTest {

    private val varsler = mutableListOf<String>()
    private val godkjenteVarsler = mutableListOf<String>()
    private val avvisteVarsler = mutableListOf<String>()
    private val deaktiverteVarsler = mutableListOf<String>()
    private val reaktiverteVarsler = mutableListOf<String>()
    private val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), generasjonRepository)

    @BeforeEach
    internal fun beforeEach() {
        varsler.clear()
        godkjenteVarsler.clear()
        avvisteVarsler.clear()
        deaktiverteVarsler.clear()
    }

    @Test
    fun lagre() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        varsel.lagre(generasjon, varselRepository)
        assertEquals(1, varsler.size)
        assertEquals("EN_KODE", varsler.single())
    }

    @Test
    fun `kan godkjenne aktivt varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        varsel.lagre(generasjon, varselRepository)
        varsel.godkjennFor(UUID.randomUUID(), "EN_IDENT", varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, godkjenteVarsler.size)
    }

    @Test
    fun `kan godkjenne vurdert varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID(), status = VURDERT)
        varsel.lagre(generasjon, varselRepository)
        varsel.godkjennFor(UUID.randomUUID(), "EN_IDENT", varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, godkjenteVarsler.size)
    }

    @Test
    fun `kan avvise aktivt varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        varsel.lagre(generasjon, varselRepository)
        varsel.avvisFor(UUID.randomUUID(), "EN_IDENT", varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, avvisteVarsler.size)
    }

    @Test
    fun `kan deaktivere aktivt varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        varsel.lagre(generasjon, varselRepository)
        varsel.deaktiverFor(UUID.randomUUID(), varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, deaktiverteVarsler.size)
    }

    @Test
    fun `kan ikke avvise godkjent varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.lagre(generasjon, varselRepository)
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, godkjenteVarsler.size)
        assertEquals(0, avvisteVarsler.size)
    }

    @Test
    fun `kan ikke deaktivere godkjent varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.lagre(generasjon, varselRepository)
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, godkjenteVarsler.size)
        assertEquals(0, deaktiverteVarsler.size)
    }

    @Test
    fun `kan ikke godkjenne avvist varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.lagre(generasjon, varselRepository)
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, avvisteVarsler.size)
        assertEquals(0, godkjenteVarsler.size)
    }

    @Test
    fun `kan ikke deaktivere avvist varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.lagre(generasjon, varselRepository)
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, avvisteVarsler.size)
        assertEquals(0, deaktiverteVarsler.size)
    }

    @Test
    fun `kan ikke godkjenne deaktivert varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.lagre(generasjon, varselRepository)
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, deaktiverteVarsler.size)
        assertEquals(0, godkjenteVarsler.size)
    }

    @Test
    fun `kan ikke avvise deaktivert varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.lagre(generasjon, varselRepository)
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, deaktiverteVarsler.size)
        assertEquals(0, avvisteVarsler.size)
    }

    @Test
    fun `kan reaktivere deaktivert varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.lagre(generasjon, varselRepository)
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        varsel.reaktiverFor(enGenerasjonId, varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(1, deaktiverteVarsler.size)
        assertEquals(1, reaktiverteVarsler.size)
    }

    @Test
    fun `kan ikke reaktivere varsel som ikke er inaktivt`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.lagre(generasjon, varselRepository)
        varsel.reaktiverFor(enGenerasjonId, varselRepository)
        assertEquals(1, varsler.size)
        assertEquals(0, deaktiverteVarsler.size)
        assertEquals(0, reaktiverteVarsler.size)
    }

    @Test
    fun equals() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        assertEquals(varsel, varsel)
        assertEquals(varsel.hashCode(), varsel.hashCode())
        assertEquals(Varsel(varselId,"EN_KODE", opprettet, vedtaksperiodeId), varsel)
        assertEquals(Varsel(varselId,"EN_KODE", opprettet, vedtaksperiodeId).hashCode(), varsel.hashCode())
    }

    @Test
    fun `not equals`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        assertNotEquals(Varsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), UUID.randomUUID()), varsel)
        assertNotEquals(Varsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), UUID.randomUUID()).hashCode(), varsel.hashCode())
    }

    private val varselRepository = object : VarselRepository {
        override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {
            deaktiverteVarsler.add(varselkode)
        }

        override fun reaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String) {
            reaktiverteVarsler.add(varselkode)
        }

        override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            godkjenteVarsler.add(varselkode)
        }

        override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            avvisteVarsler.add(varselkode)
        }
        override fun lagreVarsel(id: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
            varsler.add(varselkode)
        }

        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime): Unit = TODO("Not yet implemented")
        override fun oppdaterOpprettetTidspunkt(id: UUID, opprettet: LocalDateTime):Unit = TODO("Not yet implemented")
    }

    private val generasjonRepository get() = object : GenerasjonRepository {
        override fun opprettFørste(vedtaksperiodeId: UUID, hendelseId: UUID, id: UUID): Generasjon = TODO("Not yet implemented")
        override fun opprettNeste(id: UUID, vedtaksperiodeId: UUID, hendelseId: UUID): Generasjon = TODO("Not yet implemented")
        override fun låsFor(generasjonId: UUID, hendelseId: UUID): Unit = TODO("Not yet implemented")
        override fun utbetalingFor(generasjonId: UUID, utbetalingId: UUID): Unit = TODO("Not yet implemented")
        override fun sisteFor(vedtaksperiodeId: UUID): Generasjon = TODO("Not yet implemented")
        override fun tilhørendeFor(utbetalingId: UUID): List<Generasjon> = TODO("Not yet implemented")
        override fun fjernUtbetalingFor(generasjonId: UUID):Unit = TODO("Not yet implemented")
    }
}