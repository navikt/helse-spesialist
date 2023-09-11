package no.nav.helse.mediator

import AbstractE2ETest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.varsel.Varseldefinisjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HendelseMediatorTest : AbstractE2ETest() {

    @Test
    fun `lagre varseldefinisjon`() {
        val id = UUID.randomUUID()
        val varseldefinisjon = Varseldefinisjon(id, "SB_EX_1", "En tittel", null, null, false, LocalDateTime.now())
        hendelseMediator.håndter(varseldefinisjon)
        assertVarseldefinisjon(id)
    }

    private fun assertVarseldefinisjon(id: UUID) {
        @Language("PostgreSQL")
        val query = """SELECT COUNT(1) FROM api_varseldefinisjon WHERE unik_id = ?"""
        val antall = sessionOf(dataSource).use {
            it.run(queryOf(query, id).map { it.int(1) }.asSingle)
        }
        assertEquals(1, antall)
    }

}
