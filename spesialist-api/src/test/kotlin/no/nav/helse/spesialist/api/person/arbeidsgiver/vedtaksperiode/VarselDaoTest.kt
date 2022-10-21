package no.nav.helse.spesialist.api.person.arbeidsgiver.vedtaksperiode

import java.time.LocalDateTime
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VarselDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `Tom liste ved manglende varsler`() {
        assertTrue(varselDao.finnAktiveVarsler(PERIODE.id).isEmpty())
    }

    @Test
    fun `Finner varsler`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        nyttVarsel()
        assertTrue(varselDao.finnAktiveVarsler(PERIODE.id).isNotEmpty())
    }

    private fun nyttVarsel(varseltekst: String = "Et varsel") = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO warning(melding, vedtak_ref, kilde, opprettet) VALUES (?, ?, ?::warning_kilde, ?)"
        session.run(queryOf(statement, varseltekst, vedtakId(), "Spesialist", LocalDateTime.now()).asExecute)
    }
}
