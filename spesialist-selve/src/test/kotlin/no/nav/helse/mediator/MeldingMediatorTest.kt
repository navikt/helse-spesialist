package no.nav.helse.mediator

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.db.AvslagDao
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class MeldingMediatorTest : AbstractDatabaseTest() {
    private val fødselsnummer = lagFødselsnummer()

    private val testRapid = TestRapid()

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val avviksvurderingDao = mockk<AvviksvurderingDao>()
    private val generasjonDao = mockk<GenerasjonDao>()
    private val avslagDao = mockk<AvslagDao>()
    private val kommandofabrikk = mockk<Kommandofabrikk>(relaxed = true)
    private val stansAutomatiskBehandlingMediator = mockk<StansAutomatiskBehandlingMediator>(relaxed = true)

    private val meldingMediator =
        MeldingMediator(
            dataSource = dataSource,
            rapidsConnection = testRapid,
            oppgaveDao = oppgaveDao,
            kommandofabrikk = kommandofabrikk,
            avviksvurderingDao = avviksvurderingDao,
            generasjonDao = generasjonDao,
            avslagDao = avslagDao,
            stansAutomatiskBehandlingMediator = stansAutomatiskBehandlingMediator,
        )

    @BeforeEach
    internal fun resetTestSetup() {
        testRapid.reset()
        lagVarseldefinisjoner()
    }

    @Test
    fun `lagre varseldefinisjon`() {
        val id = UUID.randomUUID()
        val varseldefinisjon = Varseldefinisjon(id, "SB_EX_1", "En tittel", null, null, false, LocalDateTime.now())
        meldingMediator.håndter(varseldefinisjon)
        assertVarseldefinisjon(id)
    }

    @Test
    fun `Utfører kommando ved GosysOppgaveEndret`() {
        val event = mockk<GosysOppgaveEndret>(relaxed = true)
        every { event.fødselsnummer() } returns fødselsnummer
        every { event.toJson() } returns "{}"
        meldingMediator.gosysOppgaveEndret(fødselsnummer, event, testRapid)
        verify(exactly = 1) { kommandofabrikk.gosysOppgaveEndret(fødselsnummer, event) }
    }

    @Test
    fun `Returnerer tidlig ved GosysOppgaveEndret hvis oppgave ikke er til_godkjenning`() {
        val event = mockk<GosysOppgaveEndret>(relaxed = true)
        every { event.fødselsnummer() } returns fødselsnummer
        every { event.toJson() } returns "{}"
        every { oppgaveDao.finnOppgaveId(fødselsnummer) } returns null
        meldingMediator.gosysOppgaveEndret(fødselsnummer, event, testRapid)
        verify(exactly = 0) { kommandofabrikk.gosysOppgaveEndret(any(), any()) }
    }

    @Test
    fun `Returnerer tidlig ved GosysOppgaveEndret hvis vi ikke har commanddata for oppgave`() {
        val event = mockk<GosysOppgaveEndret>(relaxed = true)
        every { event.fødselsnummer() } returns fødselsnummer
        every { event.toJson() } returns "{}"
        every { oppgaveDao.oppgaveDataForAutomatisering(any()) } returns null
        meldingMediator.gosysOppgaveEndret(fødselsnummer, event, testRapid)
        verify(exactly = 0) { kommandofabrikk.gosysOppgaveEndret(any(), any()) }
    }

    @Test
    fun `Utfører kommando ved TilbakedateringBehandlet`() {
        val event =
            mockk<TilbakedateringBehandlet>(relaxed = true) {
                every { fødselsnummer() } returns fødselsnummer
                every { toJson() } returns "{}"
            }
        every { oppgaveDao.oppgaveDataForAutomatisering(any()) } returns
            mockk(relaxed = true) {
                every { periodeOverlapperMed(any()) } returns true
            }
        every { kommandofabrikk.sykefraværstilfelle(any(), any()) } returns
            mockk(relaxed = true) {
                every { erTilbakedatert(any()) } returns true
            }
        meldingMediator.tilbakedateringBehandlet(fødselsnummer, event, testRapid)
        verify(exactly = 1) { kommandofabrikk.tilbakedateringGodkjent(fødselsnummer) }
    }

    @Test
    fun `Returnerer tidlig ved TilbakedateringBehandlet hvis oppgave ikke er til godkjenning`() {
        val event =
            mockk<TilbakedateringBehandlet>(relaxed = true) {
                every { fødselsnummer() } returns fødselsnummer
                every { toJson() } returns "{}"
            }
        every { oppgaveDao.oppgaveDataForAutomatisering(any()) } returns
            mockk(relaxed = true) {
                every { periodeOverlapperMed(any()) } returns true
            }
        every { oppgaveDao.finnOppgaveId(fødselsnummer) } returns null
        meldingMediator.tilbakedateringBehandlet(fødselsnummer, event, testRapid)

        verify(exactly = 1) { oppgaveDao.finnOppgaveId(fødselsnummer) }
        verify(exactly = 0) { kommandofabrikk.tilbakedateringGodkjent(any()) }
        verify(exactly = 0) { oppgaveDao.oppgaveDataForAutomatisering(any()) }
    }

    @Test
    fun `Returnerer tidlig ved TilbakedateringBehandlet hvis vi ikke har commanddata for oppgave`() {
        val event =
            mockk<TilbakedateringBehandlet>(relaxed = true) {
                every { fødselsnummer() } returns fødselsnummer
                every { toJson() } returns "{}"
            }
        every { oppgaveDao.oppgaveDataForAutomatisering(any()) } returns null
        meldingMediator.tilbakedateringBehandlet(fødselsnummer, event, testRapid)
        verify(exactly = 1) { oppgaveDao.finnOppgaveId(fødselsnummer) }
        verify(exactly = 1) { oppgaveDao.oppgaveDataForAutomatisering(any()) }
        verify(exactly = 0) { kommandofabrikk.tilbakedateringGodkjent(any()) }
    }

    @Test
    fun `Returnerer tidlig ved TilbakedateringBehandlet hvis sykmeldingen ikke overlapper med perioden med oppgave`() {
        val event =
            mockk<TilbakedateringBehandlet>(relaxed = true) {
                every { fødselsnummer() } returns fødselsnummer
                every { toJson() } returns "{}"
            }
        val oppgaveDataForAutomatiseringMock =
            mockk<OppgaveDataForAutomatisering>(relaxed = true) {
                every { periodeOverlapperMed(any()) } returns false
            }
        every { oppgaveDao.oppgaveDataForAutomatisering(any()) } returns oppgaveDataForAutomatiseringMock
        meldingMediator.tilbakedateringBehandlet(fødselsnummer, event, testRapid)
        verify(exactly = 1) { oppgaveDao.finnOppgaveId(fødselsnummer) }
        verify(exactly = 1) { oppgaveDao.oppgaveDataForAutomatisering(any()) }
        verify(exactly = 1) { oppgaveDataForAutomatiseringMock.periodeOverlapperMed(any()) }
        verify(exactly = 0) { kommandofabrikk.tilbakedateringGodkjent(any()) }
    }

    private fun assertVarseldefinisjon(id: UUID) {
        @Language("PostgreSQL")
        val query = " SELECT COUNT(1) FROM api_varseldefinisjon WHERE unik_id = ? "
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, id).map { it.int(1) }.asSingle)
            }
        assertEquals(1, antall)
    }

    private fun lagVarseldefinisjoner() {
        Varselkode.entries.forEach { varselkode ->
            lagVarseldefinisjon(varselkode.name)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet)
            VALUES (:unik_id, :kode, :tittel, :forklaring, :handling, :avviklet, :opprettet)
            ON CONFLICT (unik_id) DO NOTHING
            """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "unik_id" to UUID.nameUUIDFromBytes(varselkode.toByteArray()),
                        "kode" to varselkode,
                        "tittel" to "En tittel for varselkode=$varselkode",
                        "forklaring" to "En forklaring for varselkode=$varselkode",
                        "handling" to "En handling for varselkode=$varselkode",
                        "avviklet" to false,
                        "opprettet" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }
    }
}
