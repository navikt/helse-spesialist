package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.VeilederStans
import no.nav.helse.spesialist.domain.oppgave.Inntektsforhold
import no.nav.helse.spesialist.domain.oppgave.Mottaker
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.oppgave.Oppgavetype
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.mar
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
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `Får feilmelding hvis man forsøker å opprette veileder-stans fra speil`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

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

        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        lagVedtaksperiode(
            id = vedtaksperiodeId,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        lagBehandling(vedtaksperiodeId = vedtaksperiodeId)

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

    @Test
    fun `Lagrer notat på vedtaksperioden med oppgave når det finnes nyere behandlinger`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId1 = lagVedtaksperiodeId()
        val vedtaksperiodeId2 = lagVedtaksperiodeId()
        val vedtaksperiodeId3 = lagVedtaksperiodeId()
        val originalMeldingId = UUID.randomUUID()
        val opprettet = Instant.now()
        val årsaker = setOf(VeilederStans.StansÅrsak.AKTIVITETSKRAV)

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        lagVedtaksperiode(
            id = vedtaksperiodeId1,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        lagVedtaksperiode(
            id = vedtaksperiodeId2,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        lagVedtaksperiode(
            id = vedtaksperiodeId3,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        lagBehandling(vedtaksperiodeId = vedtaksperiodeId1, fom = 1 jan 2023, tom = 31 jan 2023, tilstand = Behandling.Tilstand.VedtakFattet).also(sessionContext.behandlingRepository::lagre)
        lagBehandling(vedtaksperiodeId = vedtaksperiodeId2, fom = 1 feb 2023, tom = 28 feb 2023, tilstand = Behandling.Tilstand.KlarTilBehandling).also(sessionContext.behandlingRepository::lagre)
        lagBehandling(vedtaksperiodeId = vedtaksperiodeId3, fom = 1 mar 2023, tom = 30 mar 2023, tilstand = Behandling.Tilstand.VidereBehandlingAvklares).also(sessionContext.behandlingRepository::lagre)

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
                vedtaksperiodeId = vedtaksperiodeId2,
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

        val notater = sessionContext.notatRepository.finnAlleForVedtaksperiode(vedtaksperiodeId2.value)
        assertEquals(1, notater.size)
        val notat = notater.first()
        assertEquals(vedtaksperiodeId2.value, notat.vedtaksperiodeId)
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

    @Test
    fun `Lagrer notat på nyeste vedtaksperiode når personen ikke har noen oppgaver`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId1 = lagVedtaksperiodeId()
        val vedtaksperiodeId2 = lagVedtaksperiodeId()
        val originalMeldingId = UUID.randomUUID()
        val opprettet = Instant.now()
        val årsaker = setOf(VeilederStans.StansÅrsak.AKTIVITETSKRAV)

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        lagVedtaksperiode(
            id = vedtaksperiodeId1,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        lagVedtaksperiode(
            id = vedtaksperiodeId2,
            identitetsnummer = person.id,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        lagBehandling(vedtaksperiodeId = vedtaksperiodeId1, fom = 1 jan 2023, tom = 31 jan 2023, tilstand = Behandling.Tilstand.VedtakFattet).also(sessionContext.behandlingRepository::lagre)
        lagBehandling(vedtaksperiodeId = vedtaksperiodeId2, fom = 1 feb 2023, tom = 28 feb 2023, tilstand = Behandling.Tilstand.VedtakFattet).also(sessionContext.behandlingRepository::lagre)

        sessionContext.veilederStansRepository.lagre(
            VeilederStans.ny(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                årsaker = årsaker,
                opprettet = opprettet,
                originalMeldingId = originalMeldingId,
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

        val notater = sessionContext.notatRepository.finnAlleForVedtaksperiode(vedtaksperiodeId2.value)
        assertEquals(1, notater.size)
        val notat = notater.first()
        assertEquals(vedtaksperiodeId2.value, notat.vedtaksperiodeId)
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

    @Test
    fun `Får feil hvis personen ikke har noen behandlinger`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val saksbehandler = lagSaksbehandler()
        val originalMeldingId = UUID.randomUUID()
        val opprettet = Instant.now()
        val årsaker = setOf(VeilederStans.StansÅrsak.AKTIVITETSKRAV)

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        sessionContext.veilederStansRepository.lagre(
            VeilederStans.ny(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                årsaker = årsaker,
                opprettet = opprettet,
                originalMeldingId = originalMeldingId,
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
        assertEquals(500, response.status)
        assertEquals("""{"type":"about:blank","status":500,"title":"Internal Server Error"}""", response.bodyAsText)
    }
}
