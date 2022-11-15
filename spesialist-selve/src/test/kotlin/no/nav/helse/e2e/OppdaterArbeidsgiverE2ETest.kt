package no.nav.helse.e2e

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID.randomUUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendUtbetalingEndret
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.februar
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OppdaterArbeidsgiverE2ETest : AbstractE2ETest() {

    @Test
    fun `Etterspør oppdatert navn selv når svar på behovet er mottatt for et annet orgnr`() {
        settOppBruker()

        sendSaksbehandlerløsningFraAPI(testRapid.inspektør.oppgaveId(), "ident", "epost", randomUUID(), false)
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGNR, "EN_FAGSYSTEMID", utbetalingId = UTBETALING_ID)

        // Tiden går, arbeidsgivernavnet blir utdatert
        markerArbeidsgivernavnSomUkjent()

        // godkjenningsbehov for forlengelse inneholder et nytt orgnr
        // Dette trigger at Opp_rett_ArbeidsgiverCommand sender ut behov med (kun) det ukjente orgnummeret
        testRapid.reset()
        val vedtaksperiode2Id = randomUUID()
        val forlengelseBehovId = sendGodkjenningsbehov(
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiode2Id,
            utbetalingId = randomUUID(),
            periodeFom = 1.februar,
            periodeTom = 21.februar,
            orgnummereMedRelevanteArbeidsforhold = listOf(ORGNR_GHOST)
        )

        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = forlengelseBehovId,
            organisasjonsnummer = ORGNR_GHOST,
            navn = "spøkelse fabrikk 👻",
            vedtaksperiodeId = vedtaksperiode2Id,
        )

        assertEquals("En arbeidsgiver", arbeidsgivernavn())

        val arbeidgiverinformasjonBehov = sendteBehov()
        arbeidgiverinformasjonBehov[0].behovInneholderOrgnr(ORGNR_GHOST)

        // Dette behovet kom ikke tidligere pga kommandoen aksepterte ukritisk 👆 som svar på utdatert navn for ORGNR
        arbeidgiverinformasjonBehov[1].behovInneholderOrgnr(ORGNR)

        val nyttAGNavn = "Oppdatert arbeidsgivernavn"
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = forlengelseBehovId,
            organisasjonsnummer = ORGNR,
            navn = nyttAGNavn,
            vedtaksperiodeId = vedtaksperiode2Id,
        )
        assertEquals(nyttAGNavn, arbeidsgivernavn())
    }

    private fun sendteBehov() = testRapid.inspektør.hendelser("behov")
        .filter {
            it.path("@behov").map(JsonNode::asText).any { it == "Arbeidsgiverinformasjon" }
        }

    private fun JsonNode.behovInneholderOrgnr(orgNr: String) =
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

    private fun arbeidsgivernavn(): String {
        @Language("postgresql")
        val query = """
            select navn
            from arbeidsgiver_navn ag_navn
            join arbeidsgiver ag on ag_navn.id = ag.navn_ref
            where ag.orgnummer = $ORGNR
        """.trimIndent()
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query)
                .map { it.string(1) }
                .asSingle
            )!!
        }
    }

}
