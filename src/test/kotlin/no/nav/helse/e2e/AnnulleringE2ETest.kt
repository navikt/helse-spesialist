package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class AnnulleringE2ETest : AbstractE2ETest() {
    val ORGNR = "987654321"
    private val vedtaksperiodeId1: UUID = UUID.randomUUID()
    private val vedtaksperiodeId2: UUID = UUID.randomUUID()
    private val snapshotV1 = """{"arbeidsgivere":[{"perioder":[{"id":"$vedtaksperiodeId1"}]}]}"""
    private val snapshotV2 = """{"arbeidsgivere":[{"perioder":[{"id":"$vedtaksperiodeId1"}, {"id":"$vedtaksperiodeId2"}]}]}"""
    private val snapshotFinal = """{"arbeidsgivere":[{"something": "value", "perioder":[{"id":"$vedtaksperiodeId1"}, {"id":"$vedtaksperiodeId2"}]}]}"""
    private val snapshotDao = SnapshotDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)

    @Test
    fun `utbetaling annullert oppdaterer alle snapshots på personen`() {
        vedtaksperiode(vedtaksperiodeId1, snapshotV1)
        vedtaksperiode(vedtaksperiodeId2, snapshotV2)

        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotFinal
        sendUtbetalingAnnullert()

        assertEquals(
            snapshotFinal,
            snapshotDao.findSpeilSnapshot(vedtakDao.findVedtak(vedtaksperiodeId2)?.speilSnapshotRef?.toInt() ?: 0)
        )
    }

    private fun sendUtbetalingAnnullert() {
        @Language("JSON")
        val json = """
{
    "@event_name": "utbetaling_annullert",
    "@id": "${UUID.randomUUID()}",
    "fødselsnummer": "$UNG_PERSON_FNR_2018",
    "fagsystemId": "ASDJ12IA312KLS"
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
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            navn = "En Arbeidsgiver",
            bransjer = "En eller flere bransjer"
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
