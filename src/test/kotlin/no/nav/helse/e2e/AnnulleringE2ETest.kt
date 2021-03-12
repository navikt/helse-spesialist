package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

internal class AnnulleringE2ETest : AbstractE2ETest() {
    val ORGNR = "987654321"
    private val arbeidsgiverId = UUID.randomUUID()
    private val vedtaksperiodeId1: UUID = UUID.randomUUID()
    private val vedtaksperiodeId2: UUID = UUID.randomUUID()
    private val snapshotV1 = """{"aktørId": "$AKTØR", "fødselsnummer": "$UNG_PERSON_FNR_2018", "arbeidsgivere":[{"id":"$arbeidsgiverId", "organisasjonsnummer":"123","vedtaksperioder":[{"id":"$vedtaksperiodeId1"}]}]}"""
    private val snapshotV2 = """{"aktørId": "$AKTØR", "fødselsnummer": "$UNG_PERSON_FNR_2018", "arbeidsgivere":[{"id":"$arbeidsgiverId", "organisasjonsnummer":"123","vedtaksperioder":[{"id":"$vedtaksperiodeId1"}, {"id":"$vedtaksperiodeId2"}]}]}"""
    private val snapshotFinal = """{"nyKey": "nyValueSomSkalLagres", "aktørId": "$AKTØR", "arbeidsgivere":[{"id":"$arbeidsgiverId", "organisasjonsnummer":"123","vedtaksperioder":[{"id":"$vedtaksperiodeId1"}, {"id":"$vedtaksperiodeId2"}]}]}"""

    @Test
    fun `utbetaling annullert oppdaterer alle snapshots på personen`() {
        vedtaksperiode(vedtaksperiodeId1, snapshotV1)
        vedtaksperiode(vedtaksperiodeId2, snapshotV2)

        assertVedtak(vedtaksperiodeId2)
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotFinal
        sendUtbetalingAnnullert()

        assertEquals(
            snapshotFinal,
            snapshotDao.findSpeilSnapshot(vedtakDao.findVedtak(vedtaksperiodeId2)!!.speilSnapshotRef.toInt())
        )
    }

    private fun sendUtbetalingAnnullert() {
        @Language("JSON")
            val json = """
            {
                "@event_name": "utbetaling_annullert",
                "@id": "${UUID.randomUUID()}",
                "fødselsnummer": "$UNG_PERSON_FNR_2018",
                "fagsystemId": "ASDJ12IA312KLS",
                "utbetalingId": "ASDJ12IA312KLW",
                "annullertAvSaksbehandler": "${LocalDate.now()}",
                "saksbehandlerIdent": "saksbehandler_ident"
            }"""

        testRapid.sendTestMessage(json)
    }

    fun vedtaksperiode(vedtaksperiodeId: UUID, snapshot: String) {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshot

        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = Saksbehandleroppgavetype.FORLENGELSE
        )
        sendPersoninfoløsning(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = godkjenningsmeldingId
        )
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId
        )
    }
}
