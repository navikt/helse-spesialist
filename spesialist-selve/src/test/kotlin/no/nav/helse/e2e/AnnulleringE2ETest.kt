package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.mediator.api.AnnulleringDto
import no.nav.helse.mediator.api.modell.Saksbehandler
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.SENDT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNotNull

internal class AnnulleringE2ETest : AbstractE2ETest() {
    val ORGNR = "987654321"
    private val vedtaksperiodeId1: UUID = UUID.randomUUID()
    private val vedtaksperiodeId2: UUID = UUID.randomUUID()
    private val snapshotV1 = snapshot(1)
    private val snapshotV2 = snapshot(2)
    private val snapshotFinal = snapshot(3)

    @Test
    fun `utbetaling annullert oppdaterer alle snapshots på personen`() {
        val (oid, navn, epost) = Triple(UUID.randomUUID(), "en saksbehandler", "saksbehandler_epost")
        saksbehandlerDao.opprettSaksbehandler(oid, navn, epost)
        vedtaksperiode(vedtaksperiodeId = vedtaksperiodeId1, snapshot = snapshotV1, utbetalingId = UUID.randomUUID())
        vedtaksperiode(vedtaksperiodeId = vedtaksperiodeId2, snapshot = snapshotV2, utbetalingId = UUID.randomUUID())

        assertVedtak(vedtaksperiodeId2)
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns snapshotFinal
        sendUtbetalingAnnullert(saksbehandlerEpost = epost)

        assertSnapshot(snapshotFinal, vedtaksperiodeId1)
        assertSnapshot(snapshotFinal, vedtaksperiodeId2)
    }

    @Test
    fun `Annullert av saksbehandler mappes til speil`() {
        vedtaksperiode(vedtaksperiodeId = vedtaksperiodeId1, snapshot = snapshotV1, utbetalingId = UUID.randomUUID())

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
        assertEquals("Kevders Chilleby", annullerAvSaksbehandler?.saksbehandlerNavn)
    }
}
