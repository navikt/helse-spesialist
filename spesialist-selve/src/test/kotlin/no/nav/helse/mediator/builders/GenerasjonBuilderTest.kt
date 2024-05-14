package no.nav.helse.mediator.builders

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterNyttVarsel
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Periode.Companion.til
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class GenerasjonBuilderTest : AbstractDatabaseTest() {
    private val varselRepository = VarselRepository(dataSource)
    private val generasjonRepository = GenerasjonRepository(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)

    @Test
    fun bygg() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()

        generasjonDao.opprettFor(
            generasjonId,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            1.januar til 31.januar,
            Generasjon.VidereBehandlingAvklares,
            null,
        )
        varselRepository.varselOpprettet(varselId, vedtaksperiodeId, generasjonId, "SB_EX_1", opprettet)
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        val vedtaksperiode = builder.build(generasjonRepository, varselRepository)
        val forventetVedtaksperiode =
            Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar).also {
                it.håndterNyttVarsel(Varsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId), UUID.randomUUID())
            }
        assertEquals(forventetVedtaksperiode, vedtaksperiode)
    }

    @Test
    fun `varselRepository blir registrert som observer`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            1.januar til 31.januar,
            Generasjon.VidereBehandlingAvklares,
            null,
        )
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        val generasjon = builder.build(generasjonRepository, varselRepository)
        listOf(generasjon).håndterNyttVarsel(
            listOf(Varsel(varselId, "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)),
            UUID.randomUUID(),
        )
        assertVarsel(generasjonId, "SB_EX_1", vedtaksperiodeId, varselId)
    }

    private fun assertVarsel(
        generasjonId: UUID,
        forventetVarselkode: String,
        forventetVedtaksperiodeId: UUID,
        forventetVarselId: UUID,
    ) {
        @Language("PostgreSQL")
        val query = "SELECT kode, unik_id, vedtaksperiode_id FROM selve_varsel sv WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ?)"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, generasjonId).map {
                    assertEquals(forventetVarselkode, it.string("kode"))
                    assertEquals(forventetVedtaksperiodeId, it.uuid("vedtaksperiode_id"))
                    assertEquals(forventetVarselId, it.uuid("unik_id"))
                }.asSingle,
            )
        }
    }
}
