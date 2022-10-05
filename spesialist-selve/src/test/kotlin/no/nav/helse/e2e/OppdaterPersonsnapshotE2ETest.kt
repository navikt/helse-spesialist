package no.nav.helse.e2e

import AbstractE2ETest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.every
import java.time.LocalDate
import java.util.*
import java.util.UUID.randomUUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Meldingssender.sendArbeidsforholdløsning
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsning
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsning
import no.nav.helse.Meldingssender.sendEgenAnsattløsning
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsning
import no.nav.helse.Meldingssender.sendRisikovurderingløsning
import no.nav.helse.Meldingssender.sendVergemålløsning
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsning
import no.nav.helse.TestRapidHelpers.contextId
import no.nav.helse.TestRapidHelpers.hendelseId
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.snapshot
import no.nav.helse.mediator.graphql.HentSnapshot
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class OppdaterPersonsnapshotE2ETest : AbstractE2ETest() {
    private val vedtaksperiodeId1: UUID = randomUUID()
    private val vedtaksperiodeId2: UUID = randomUUID()
    private val utbetalingId1: UUID = randomUUID()
    private val utbetalingId2: UUID = randomUUID()
    private val snapshotV1 = snapshot(1)
    private val snapshotV2 = snapshot(2)
    private val snapshotFinal = snapshot(3)

    @Test
    fun `Oppdater personsnapshot oppdaterer alle snapshots på personen`() {
        vedtaksperiode(vedtaksperiodeId1, snapshotV1, utbetalingId1, Periodetype.FØRSTEGANGSBEHANDLING)
        vedtaksperiode(vedtaksperiodeId2, snapshotV2, utbetalingId2, Periodetype.FORLENGELSE)

        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotFinal
        sendOppdaterPersonsnapshot()

        assertSnapshot(snapshotFinal, vedtaksperiodeId1)
        assertSnapshot(snapshotFinal, vedtaksperiodeId2)
    }

    @Test
    fun `Oppdaterer også Infotrygd-utbetalinger`() {
        vedtaksperiode(vedtaksperiodeId1, snapshotV1, utbetalingId1, Periodetype.FØRSTEGANGSBEHANDLING)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotFinal
        sendOppdaterPersonsnapshot()

        assertInfotrygdutbetalingerOppdatert(FØDSELSNUMMER)
        settInfotrygdutbetalingerUtdatert(FØDSELSNUMMER)
        assertInfotrygdutbetalingerOppdatert(FØDSELSNUMMER, forventetDato = LocalDate.now().minusDays(7))

        sendInfotrygdløsning()

        assertInfotrygdutbetalingerOppdatert(FØDSELSNUMMER)
    }

    private fun settInfotrygdutbetalingerUtdatert(fødselsnummer: String, antallDager: Int = 7) =
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "update person set infotrygdutbetalinger_oppdatert = now() - interval '$antallDager days' where fodselsnummer=:fnr",
                    mapOf("fnr" to fødselsnummer.toLong()),
                ).asUpdate
            )
        }

    private fun assertInfotrygdutbetalingerOppdatert(
        fødselsnummer: String,
        forventetDato: LocalDate = LocalDate.now()
    ) {
        val dato = sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "select infotrygdutbetalinger_oppdatert from person where fodselsnummer=:fnr",
                    mapOf("fnr" to fødselsnummer.toLong()),
                ).map { row -> row.localDate(1) }.asSingle
            )
        }
        assertEquals(forventetDato, dato)
    }

    private fun sendInfotrygdløsning() {
        val testmeldingfabrikk = Testmeldingfabrikk(FØDSELSNUMMER, AKTØR)
        testRapid.sendTestMessage(
            testmeldingfabrikk.lagHentInfotrygdutbetalingerløsning(
                hendelseId = testRapid.inspektør.hendelseId(),
                contextId = testRapid.inspektør.contextId()
            )
        )
    }

    private fun sendOppdaterPersonsnapshot() {
        @Language("JSON")
        val json = """
{
    "@event_name": "oppdater_personsnapshot",
    "@id": "${randomUUID()}",
    "fødselsnummer": "$FØDSELSNUMMER"
}"""

        testRapid.sendTestMessage(json)
    }

    fun vedtaksperiode(
        vedtaksperiodeId: UUID,
        snapshot: GraphQLClientResponse<HentSnapshot.Result>,
        utbetalingId: UUID,
        periodetype: Periodetype
    ) {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            periodetype = periodetype
        )

        if (periodetype == Periodetype.FØRSTEGANGSBEHANDLING) {
            sendPersoninfoløsning(
                orgnr = ORGNR,
                vedtaksperiodeId = vedtaksperiodeId,
                hendelseId = godkjenningsmeldingId
            )
            sendArbeidsgiverinformasjonløsning(
                hendelseId = godkjenningsmeldingId,
                orgnummer = ORGNR,
                vedtaksperiodeId = vedtaksperiodeId
            )
            sendArbeidsforholdløsning(
                hendelseId = godkjenningsmeldingId,
                orgnr = ORGNR,
                vedtaksperiodeId = vedtaksperiodeId
            )
        }

        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(godkjenningsmeldingId = godkjenningsmeldingId)
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
