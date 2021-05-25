package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.mediator.api.AnnulleringDto
import no.nav.helse.mediator.api.modell.Saksbehandler
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.SENDT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNotNull

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
        val (oid, navn, epost) = Triple(UUID.randomUUID(), "en saksbehandler", "saksbehandler_epost")
        saksbehandlerDao.opprettSaksbehandler(oid, navn, epost)
        vedtaksperiode(vedtaksperiodeId1, snapshotV1)
        vedtaksperiode(vedtaksperiodeId2, snapshotV2)

        assertVedtak(vedtaksperiodeId2)
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotFinal
        sendUtbetalingAnnullert(saksbehandlerEpost = epost)

        assertSnapshot(snapshotFinal, vedtaksperiodeId1)
        assertSnapshot(snapshotFinal, vedtaksperiodeId2)
    }

    @Test
    fun `Annullert av saksbehandler mappes til speil`() {
        vedtaksperiode(vedtaksperiodeId1, snapshotV1)

        sendUtbetalingEndret(
            type = "UTBETALING",
            status = UTBETALT,
            orgnr = ORGNR,
            arbeidsgiverFagsystemId = "arbeidsgiver_fagsystem_id",
            forrigeStatus = SENDT,
            utbetalingId = UTBETALING_ID
        )

        val annulleringDto = AnnulleringDto(AKTØR, UNG_PERSON_FNR_2018, ORGNR, "ASJKLD90283JKLHAS3JKLF", "123")
        val saksbehandler = Saksbehandler("kevders.chilleby@nav.no", UUID.randomUUID(), "Kevders Chilleby")
        håndterAnnullering(annulleringDto, saksbehandler)

        sendUtbetalingAnnullert(saksbehandlerEpost = "kevders.chilleby@nav.no")

        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(UNG_PERSON_FNR_2018))
        val annullerAvSaksbehandler = speilSnapshot.utbetalinger.first().annullertAvSaksbehandler

        assertNotNull(annullerAvSaksbehandler?.annullertTidspunkt)
        Assertions.assertEquals("Kevders Chilleby", annullerAvSaksbehandler?.saksbehandlerNavn)
    }

    fun vedtaksperiode(vedtaksperiodeId: UUID, snapshot: String, utbetalingId: UUID = UUID.randomUUID()) {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshot

        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = utbetalingId
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
