package no.nav.helse.e2e

import AbstractE2ETest
import kotliquery.sessionOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
internal class InnhentArbeidsgivernavnE2ETest : AbstractE2ETest() {

    @Test
    fun `Etterspør arbeidsgiverinformasjon for AG vi mangler navnet på`() {
        vedtaksløsningenMottarNySøknad()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        opprettArbeidsgiverSomManglerNavn(organisasjonsnummer)
        håndterInnhentArbeidsgivernavn()
        assertSisteEtterspurteBehov("Arbeidsgiverinformasjon")

        håndterArbeidsgiverinformasjonløsning(fødselsnummer = null)
        assertEquals("Navn for $organisasjonsnummer", finnArbeidsgiver(organisasjonsnummer))
    }

    private fun opprettArbeidsgiverSomManglerNavn(organisasjonsnummer: String) {
        sessionOf(dataSource).use { session ->
            session.run(
                asSQL(
                    "insert into arbeidsgiver (organisasjonsnummer) values (:organisasjonsnummer)",
                    "organisasjonsnummer" to organisasjonsnummer
                ).asUpdate
            )
        }
    }

    private fun finnArbeidsgiver(organisasjonsnummer: String) = sessionOf(dataSource).use { session ->
        session.run(
            asSQL(
                """
                select navn from arbeidsgiver
                join arbeidsgiver_navn on navn_ref = arbeidsgiver_navn.id
                where organisasjonsnummer = :organisasjonsnummer""".trimIndent(),
                "organisasjonsnummer" to organisasjonsnummer
            ).map {
                it.string("navn")
            }.asSingle
        )
    }
}
