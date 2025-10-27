package no.nav.helse.spesialist.api

import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.testfixtures.fødselsdato
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

abstract class OpphevStansIntegrationTest {
    protected val integrationTestFixture = IntegrationTestFixture()

    private val notatRepository = integrationTestFixture.sessionFactory.sessionContext.notatRepository
    private val oppgaveRepository = integrationTestFixture.sessionFactory.sessionContext.oppgaveRepository
    private val stansAutomatiskBehandlingDao =
        integrationTestFixture.sessionFactory.sessionContext.stansAutomatiskBehandlingDao
    private val vedtaksperiodeRepository = integrationTestFixture.sessionFactory.sessionContext.legacyVedtaksperiodeRepository
    private val personDao = integrationTestFixture.sessionFactory.sessionContext.personDao

    @Test
    fun `Lagrer melding og notat når stans oppheves fra speil`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()

        personDao.upsertPersoninfo(
            fødselsnummer = fødselsnummer,
            fornavn = lagFornavn(),
            mellomnavn = null,
            etternavn = lagEtternavn(),
            fødselsdato = fødselsdato(),
            kjønn = Kjønn.Kvinne,
            adressebeskyttelse = Adressebeskyttelse.Ugradert
        )

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
        oppgaveRepository.lagre(
            Oppgave.ny(
                id = 1,
                førsteOpprettet = null,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = UUID.randomUUID(),
                utbetalingId = UUID.randomUUID(),
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = emptySet(),
            )
        )
        val begrunnelse = "begrunnelse"

        // When:
        postAndAssertSuccess(saksbehandler, fødselsnummer, begrunnelse)

        // Then:
        // Sjekk persistert data
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
        assertEquals(begrunnelse, notat.tekst)
        assertEquals(saksbehandler.id(), notat.saksbehandlerOid)
        assertEquals(false, notat.feilregistrert)
        assertEquals(null, notat.feilregistrertTidspunkt)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser()
    }

    abstract fun postAndAssertSuccess(
        saksbehandler: Saksbehandler,
        fødselsnummer: String,
        begrunnelse: String
    )
}
