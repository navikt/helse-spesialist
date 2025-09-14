package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.api.testfixtures.mutation.opphevStansMutation
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class OpphevStansMutationIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()

    private val notatRepository = integrationTestFixture.sessionFactory.sessionContext.notatRepository
    private val oppgaveRepository = integrationTestFixture.sessionFactory.sessionContext.oppgaveRepository
    private val stansAutomatiskBehandlingDao = integrationTestFixture.sessionFactory.sessionContext.stansAutomatiskBehandlingDao
    private val vedtaksperiodeRepository = integrationTestFixture.sessionFactory.sessionContext.vedtaksperiodeRepository

    @Test
    fun `opphev stans`() {
        val responseJson = integrationTestFixture.executeQuery(
            query = opphevStansMutation(lagFødselsnummer(), "EN_BEGRUNNELSE"),
        )

        assertEquals(true, responseJson.get("data")?.get("opphevStans")?.asBoolean())
    }

    @Test
    fun `Lagrer melding og notat når stans oppheves fra speil`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()
        vedtaksperiodeRepository.lagreVedtaksperioder(
            fødselsnummer = fødselsnummer,
            vedtaksperioder = listOf(
                VedtaksperiodeDto(
                    organisasjonsnummer = lagOrganisasjonsnummer(),
                    vedtaksperiodeId = vedtaksperiodeId,
                    forkastet = false,
                    behandlinger = emptyList(),
                )
            )
        )
        oppgaveRepository.lagre(Oppgave.ny(
            id = 1,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = UUID.randomUUID(),
            utbetalingId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            kanAvvises = true,
            egenskaper = emptySet(),
        ))

        // When:
        val responseJson = integrationTestFixture.executeQuery(
            saksbehandler = saksbehandler,
            query = opphevStansMutation(fødselsnummer, "begrunnelse"),
        )

        // then:
        assertEquals(true, responseJson.get("data")?.get("opphevStans")?.asBoolean())

        val lagredeStans = stansAutomatiskBehandlingDao.hentFor(fødselsnummer)
        assertEquals(1, lagredeStans.size)
        val lagretStans = lagredeStans.first()
        assertEquals(fødselsnummer, lagretStans.fødselsnummer)
        assertEquals("NORMAL", lagretStans.status)
        assertEquals(emptySet<StoppknappÅrsak>(), lagretStans.årsaker)
        assertEquals(null, lagretStans.meldingId)

        val notater = notatRepository.finnAlleForVedtaksperiode(vedtaksperiodeId)
        assertEquals(1, notater.size)
        val notat = notater.first()
        assertEquals(vedtaksperiodeId, notat.vedtaksperiodeId)
        assertEquals(NotatType.OpphevStans, notat.type)
        assertEquals("begrunnelse", notat.tekst)
        assertEquals(saksbehandler.id(), notat.saksbehandlerOid)
        assertEquals(false, notat.feilregistrert)
        assertEquals(null, notat.feilregistrertTidspunkt)
    }
}
