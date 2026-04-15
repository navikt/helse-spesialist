package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.VeilederStans
import no.nav.helse.spesialist.domain.oppgave.Inntektsforhold
import no.nav.helse.spesialist.domain.oppgave.Mottaker
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.oppgave.Oppgavetype
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PatchVeilederStansBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `Får feilmelding hvis man forsøker å opprette veileder-stans fra speil`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response =
            integrationTestFixture.patch(
                "/api/personer/${personPseudoId.value}/stans/veileder",
                body = """{ "begrunnelse": "begrunnelse", "stans": true }""",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Skriv),
            )

        // Then:
        assertEquals(400, response.status)
    }

    @Test
    fun `Lagrer melding og notat når saksbehandler opphever veileder-stans fra speil`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = lagVedtaksperiodeId()
        val originalMeldingId = UUID.randomUUID()
        val opprettet = Instant.now()
        val årsaker = setOf(VeilederStans.StansÅrsak.AKTIVITETSKRAV)

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id)

        lagVedtaksperiode(
            id = vedtaksperiodeId,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        sessionContext.veilederStansRepository.lagre(
            VeilederStans.ny(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                årsaker = årsaker,
                opprettet = opprettet,
                originalMeldingId = originalMeldingId,
            ),
        )

        sessionContext.oppgaveRepository.lagre(
            Oppgave.ny(
                id = 1,
                førsteOpprettet = null,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = lagSpleisBehandlingId(),
                utbetalingId = UUID.randomUUID(),
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = emptySet(),
                mottaker = Mottaker.UtbetalingTilArbeidsgiver,
                oppgavetype = Oppgavetype.Søknad,
                inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
                inntektsforhold = Inntektsforhold.Arbeidstaker,
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            ),
        )
        val begrunnelse = "begrunnelse"

        // When:
        val response =
            integrationTestFixture.patch(
                "/api/personer/${personPseudoId.value}/stans/veileder",
                body = """{ "begrunnelse": "$begrunnelse", "stans": false }""",
                saksbehandler = saksbehandler,
                tilganger = setOf(Tilgang.Skriv),
            )
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        // Then:
        // Sjekk persistert data
        val lagredeStans = sessionContext.veilederStansRepository.finnAlle(Identitetsnummer.fraString(fødselsnummer))
        assertEquals(1, lagredeStans.size)
        val lagretStans = lagredeStans.first()
        assertEquals(fødselsnummer, lagretStans.identitetsnummer.value)
        assertEquals(årsaker, lagretStans.årsaker)
        assertEquals(originalMeldingId, lagretStans.originalMeldingId)
        assertEquals(opprettet, lagretStans.opprettet)

        val notater = sessionContext.notatRepository.finnAlleForVedtaksperiode(vedtaksperiodeId.value)
        assertEquals(1, notater.size)
        val notat = notater.first()
        assertEquals(vedtaksperiodeId.value, notat.vedtaksperiodeId)
        assertEquals(NotatType.OpphevStans, notat.type)
        assertEquals(begrunnelse, notat.tekst)
        assertEquals(saksbehandler.id, notat.saksbehandlerOid)
        assertEquals(false, notat.feilregistrert)
        assertEquals(null, notat.feilregistrertTidspunkt)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }
}
