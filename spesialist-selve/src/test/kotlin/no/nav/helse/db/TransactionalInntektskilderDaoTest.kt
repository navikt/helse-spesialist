package no.nav.helse.db

import DatabaseIntegrationTest
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.spesialist.test.lagOrganisasjonsnavn
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TransactionalInntektskilderDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `Kan lagre komplette inntektskilder`() {
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val navn1 = lagOrganisasjonsnavn()
        val navn2 = lagOrganisasjonsnavn()
        val bransjer1 = listOf("Uteliv", "Reise")
        val bransjer2 = listOf("Hotell")

        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { transaction ->
                val dao = TransactionalInntektskilderDao(transaction)
                dao.lagreInntektskilder(
                    listOf(
                        KomplettInntektskildeDto(
                            organisasjonsnummer = organisasjonsnummer1,
                            type = InntektskildetypeDto.ORDINÆR,
                            navn = navn1,
                            bransjer = bransjer1,
                            sistOppdatert = LocalDate.now(),
                        ),
                        KomplettInntektskildeDto(
                            organisasjonsnummer = organisasjonsnummer2,
                            type = InntektskildetypeDto.ORDINÆR,
                            navn = navn2,
                            bransjer = bransjer2,
                            sistOppdatert = LocalDate.now(),
                        ),
                    ),
                )

                val første = transaction.finnInntektskilde(organisasjonsnummer1)
                val andre = transaction.finnInntektskilde(organisasjonsnummer2)
                assertEquals(organisasjonsnummer1, første?.orgnummer)
                assertEquals(organisasjonsnummer2, andre?.orgnummer)
            }
        }
    }

    private fun TransactionalSession.finnInntektskilde(orgnummer: String): ArbeidsgiverDto? {
        @Language("PostgreSQL")
        val query = "select orgnummer, navn_ref, bransjer_ref from arbeidsgiver where orgnummer = :orgnummer"
        return run(
            queryOf(query, mapOf("orgnummer" to orgnummer.toLong()))
                .map { row ->
                    ArbeidsgiverDto(
                        orgnummer = row.string("orgnummer"),
                        navnRef = row.long("navn_ref"),
                        bransjerRef = row.long("bransjer_ref"),
                    )
                }.asSingle
        )
    }

    private data class ArbeidsgiverDto(
        val orgnummer: String,
        val navnRef: Long,
        val bransjerRef: Long,
    )
}
