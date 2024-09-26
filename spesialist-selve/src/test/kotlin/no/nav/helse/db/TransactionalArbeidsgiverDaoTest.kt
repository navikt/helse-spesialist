package no.nav.helse.db

import DatabaseIntegrationTest
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class TransactionalArbeidsgiverDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `Oppretter minimal arbeidsgiver`() {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                val dao = TransactionalArbeidsgiverDao(transaction)
                dao.insertMinimalArbeidsgiver(ORGNUMMER)
                assertNotNull(dao.findArbeidsgiverByOrgnummer(ORGNUMMER))
            }
        }
    }
}
