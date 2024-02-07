package no.nav.helse.e2e

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID.randomUUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.ArbeidsgiverinformasjonJson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OppdaterArbeidsgiverE2ETest : AbstractE2ETest() {

    @Test
    fun `Etterspør oppdatert navn selv når svar på behovet er mottatt for et annet orgnr`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()

        // Tiden går, arbeidsgivernavnet blir utdatert
        markerArbeidsgivernavnSomUkjent()

        // Dette trigger at Opp_rett_ArbeidsgiverCommand sender ut behov med (kun) det ukjente orgnummeret
        val vedtaksperiode2Id = randomUUID()

        // godkjenningsbehov for forlengelse inneholder et nytt orgnr
        håndterVedtaksperiodeOpprettet(vedtaksperiodeId = vedtaksperiode2Id)
        håndterGodkjenningsbehov(
            harOppdatertMetainfo = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(
                vedtaksperiodeId = vedtaksperiode2Id,
                utbetalingId = randomUUID(),
                orgnummereMedRelevanteArbeidsforhold = listOf(ORGNR_GHOST),
            ),
        )

        assertInnholdIBehov(behov = "Arbeidsgiverinformasjon") { jsonNode ->
            jsonNode.behovInneholderOrganisasjonsnummer(ORGNR_GHOST)
        }

        håndterArbeidsgiverinformasjonløsning(
            organisasjonsnummer = ORGNR_GHOST,
        )

        assertEquals("Navn for $ORGNR", arbeidsgivernavn(ORGNR))
        assertEquals("Navn for $ORGNR_GHOST", arbeidsgivernavn(ORGNR_GHOST))

//        Dette Arbeidsgiverinformasjon-behovet ble tidligere ikke sendt ut fordi løsningen på
//        Arbeidsgiverinformasjon-behovet som gikk ut ifbm. godkjenningsbehov på vedtaksperiode #2 ble akseptert som gyldig
//        løsning selv om det ikke inneholdt løsning for samtlige relevante arbeidsgivere
        assertInnholdIBehov(behov = "Arbeidsgiverinformasjon") { jsonNode ->
            jsonNode.behovInneholderOrganisasjonsnummer(ORGNR)
        }

        val nyttAGNavn = "Oppdatert arbeidsgivernavn"

        håndterArbeidsgiverinformasjonløsning(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiode2Id,
            arbeidsgiverinformasjonJson = listOf(
                ArbeidsgiverinformasjonJson(
                    ORGNR,
                    nyttAGNavn,
                    listOf("En bransje")
                )
            )
        )

        assertEquals(nyttAGNavn, arbeidsgivernavn(ORGNR))
    }

    private fun JsonNode.behovInneholderOrganisasjonsnummer(orgNr: String) =
        path("Arbeidsgiverinformasjon")
            .path("organisasjonsnummer")
            .map(JsonNode::asText)
            .let { assertTrue(it.containsAll(listOf(orgNr))) }

    private fun markerArbeidsgivernavnSomUkjent() {
        @Language("postgresql")
        val query = """
            update arbeidsgiver_navn ag_navn
            set navn_oppdatert = 'now'::timestamp - '1 month'::interval
            from arbeidsgiver ag
            where ag.orgnummer = $ORGNR
        """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query).asExecute)
        }
    }

    private fun arbeidsgivernavn(organisasjonsnummer: String = ORGNR): String {
        @Language("postgresql")
        val query = """
            select navn
            from arbeidsgiver_navn ag_navn
            join arbeidsgiver ag on ag_navn.id = ag.navn_ref
            where ag.orgnummer = ?
        """.trimIndent()
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, organisasjonsnummer.toInt())
                .map { it.string(1) }
                .asSingle
            )!!
        }
    }

}
