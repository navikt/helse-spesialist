package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.FeatureToggles
import no.nav.helse.MeldingPubliserer
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.e2e.DatabaseIntegrationTest
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.spesialist.api.SendIReturResult
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.mutation.Avslag
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData.ApiAnnulleringArsak
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiLovhjemmel
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgrad
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringDag
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.ApiOpphevStans
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.db.DBDaos
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.spesialist.test.lagSaksbehandlerident
import no.nav.helse.spesialist.test.lagSaksbehandlernavn
import no.nav.helse.spesialist.test.lagTilfeldigSaksbehandlerepost
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.util.februar
import no.nav.helse.util.januar
import no.nav.helse.util.testEnv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

internal class SaksbehandlerMediatorTest : DatabaseIntegrationTest() {
    private val tilgangsgrupper = SpeilTilgangsgrupper(testEnv)
    private val testRapid = TestRapid()
    private val meldingPubliserer: MeldingPubliserer = MessageContextMeldingPubliserer(testRapid)
    private val tildelingDbDao = daos.tildelingDao
    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            stansAutomatiskBehandlingDao,
            oppgaveDao,
            notatDao,
            dialogDao,
        )
    private val oppgaveService =
        OppgaveService(
            oppgaveDao = oppgaveDao,
            tildelingDao = tildelingDbDao,
            reservasjonDao = reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
            tilgangsgrupper = tilgangsgrupper,
            daos = daos
        )
    private val apiOppgaveService = ApiOppgaveService(
        oppgaveDao = oppgaveDao,
        tilgangsgrupper = tilgangsgrupper,
        oppgaveService = oppgaveService
    )
    private val featureToggles = object : FeatureToggles {
        var skalBenytteNyTotrinnsløype: Boolean = false
        override fun skalBenytteNyTotrinnsvurderingsløsning(): Boolean {
            return skalBenytteNyTotrinnsløype
        }
    }
    private val mediator =
        SaksbehandlerMediator(
            daos = DBDaos(dataSource),
            versjonAvKode = "versjonAvKode",
            meldingPubliserer = meldingPubliserer,
            oppgaveService = oppgaveService,
            apiOppgaveService = apiOppgaveService,
            tilgangsgrupper = tilgangsgrupper,
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
            annulleringRepository = annulleringRepository,
            env = environment,
            featureToggles = featureToggles,
            sessionFactory = TransactionalSessionFactory(dataSource),
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
        )

    private val AKTØR_ID = lagAktørId()
    private val FØDSELSNUMMER = lagFødselsnummer()
    private val ORGANISASJONSNUMMER = lagOrganisasjonsnummer()
    private val ORGANISASJONSNUMMER_GHOST = lagOrganisasjonsnummer()

    private val saksbehandler = saksbehandler()

    private fun saksbehandler(
        oid: UUID = SAKSBEHANDLER.oid,
        navn: String = SAKSBEHANDLER.navn,
        epost: String = SAKSBEHANDLER.epost,
        ident: String = SAKSBEHANDLER.ident,
        grupper: List<UUID> = emptyList(),
    ): SaksbehandlerFraApi = SaksbehandlerFraApi(oid, navn, epost, ident, grupper)

    @BeforeEach
    internal fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `håndter totrinnsvurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        opprettTotrinnsvurdering(vedtaksperiodeId)
        opprettSaksbehandler()
        val saksbehandler = saksbehandler(grupper = emptyList())
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "VURDERT",
        )

        val result =
            mediator.håndterTotrinnsvurdering(oppgaveId, saksbehandler, ApiVedtakUtfall.INNVILGELSE, "Begrunnelse")

        assertEquals(SendTilGodkjenningResult.Ok, result)
        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finn(vedtaksperiodeId)
        }
        checkNotNull(totrinnsvurdering)
        assertEquals(saksbehandler.oid, totrinnsvurdering.saksbehandler?.value)
        assertTrue(totrinnsvurdering.erBeslutteroppgave)
    }

    @Test
    fun `ny overstyring uten eksisterende totrinnsvurdering lager totrinnsvurdering`() {
        featureToggles.skalBenytteNyTotrinnsløype = true
        val person =
            person {
                arbeidsgivere = arbeidsgivere(2)
            }
        nyPerson(fødselsnummer = person.fødselsnummer, aktørId = person.aktørId, organisasjonsnummer = person { 2.ag })
        opprettSaksbehandler()
        opprettVedtaksperiode()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER.oid,
            SAKSBEHANDLER.navn,
            SAKSBEHANDLER.epost,
            SAKSBEHANDLER.ident,
            emptyList()
        )
        val overstyring =
            ApiTidslinjeOverstyring(
                vedtaksperiodeId = VEDTAKSPERIODE,
                organisasjonsnummer = person { 2.ag },
                fodselsnummer = person.fødselsnummer,
                aktorId = person.aktørId,
                begrunnelse = "En begrunnelse",
                dager =
                    listOf(
                        ApiOverstyringDag(
                            dato = 10.januar,
                            type = "Sykedag",
                            fraType = "Arbeidsdag",
                            grad = null,
                            fraGrad = 100,
                            null,
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finn(VEDTAKSPERIODE)
        }
        checkNotNull(totrinnsvurdering)
        assertEquals(saksbehandler.oid, totrinnsvurdering.saksbehandler?.value)
        assertTrue(totrinnsvurdering.overstyringer.single().opprettet.isAfter(LocalDateTime.now().minusSeconds(5)))
        assertTrue(totrinnsvurdering.erBeslutteroppgave)
    }

    @Test
    fun `ny overstyring med eksisterende totrinnsvurdering legges på eksisterende totrinnsvurdering med opprinnelig saksbehandler`() {
        val person =
            person {
                arbeidsgivere = arbeidsgivere(2)
            }
        nyPerson(fødselsnummer = person.fødselsnummer, aktørId = person.aktørId, organisasjonsnummer = person { 2.ag })
        opprettSaksbehandler()
        opprettVedtaksperiode()
        val saksbehandler2Oid = UUID.randomUUID()
        opprettSaksbehandler(saksbehandler2Oid)
        opprettTotrinnsvurdering(VEDTAKSPERIODE, saksbehandler2Oid)
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER.oid,
            SAKSBEHANDLER.navn,
            SAKSBEHANDLER.epost,
            SAKSBEHANDLER.ident,
            emptyList()
        )
        val overstyring =
            ApiTidslinjeOverstyring(
                vedtaksperiodeId = VEDTAKSPERIODE,
                organisasjonsnummer = person { 2.ag },
                fodselsnummer = person.fødselsnummer,
                aktorId = person.aktørId,
                begrunnelse = "En begrunnelse",
                dager =
                    listOf(
                        ApiOverstyringDag(
                            dato = 10.januar,
                            type = "Sykedag",
                            fraType = "Arbeidsdag",
                            grad = null,
                            fraGrad = 100,
                            null,
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finn(VEDTAKSPERIODE)
        }
        checkNotNull(totrinnsvurdering)
        assertEquals(saksbehandler2Oid, totrinnsvurdering.saksbehandler?.value)
        assertTrue(totrinnsvurdering.overstyringer.single().opprettet.isAfter(LocalDateTime.now().minusSeconds(5)))
        assertTrue(totrinnsvurdering.erBeslutteroppgave)
    }

    @Test
    fun `håndter totrinnsvurdering send i retur`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        opprettTotrinnsvurdering(vedtaksperiodeId)
        opprettSaksbehandler()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER.oid,
            SAKSBEHANDLER.navn,
            SAKSBEHANDLER.epost,
            SAKSBEHANDLER.ident,
            emptyList()
        )
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "VURDERT",
        )

        val result =
            mediator.håndterTotrinnsvurdering(oppgaveId, saksbehandler, ApiVedtakUtfall.INNVILGELSE, "Begrunnelse")

        assertEquals(SendTilGodkjenningResult.Ok, result)

        val beslutter = SaksbehandlerFraApi(
            UUID.randomUUID(),
            lagSaksbehandlernavn(),
            lagTilfeldigSaksbehandlerepost(),
            lagSaksbehandlerident(),
            emptyList()
        )
        opprettSaksbehandler(beslutter.oid, beslutter.navn, beslutter.epost, beslutter.ident)
        val resultRetur = mediator.sendIRetur(oppgaveId, beslutter, "begrunnelse")

        assertEquals(SendIReturResult.Ok, resultRetur)

        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finn(vedtaksperiodeId)
        }
        checkNotNull(totrinnsvurdering)
        assertEquals(saksbehandler.oid, totrinnsvurdering.saksbehandler?.value)
        assertEquals(beslutter.oid, totrinnsvurdering.beslutter?.value)
        assertTrue(totrinnsvurdering.erRetur)
    }

    @Test
    fun `håndter totrinnsvurdering når periode har aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        opprettTotrinnsvurdering(vedtaksperiodeId)
        opprettSaksbehandler()
        val saksbehandler = saksbehandler(grupper = emptyList())
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "AKTIV",
        )

        assertTrue(
            mediator.håndterTotrinnsvurdering(
                oppgaveId,
                saksbehandler,
                ApiVedtakUtfall.INNVILGELSE,
                "Begrunnelse"
            ) is SendTilGodkjenningResult.Feil.ManglerVurderingAvVarsler
        )
    }

    @Test
    fun `håndter totrinnsvurdering når periode ikke har noen varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)

        opprettTotrinnsvurdering(vedtaksperiodeId)
        opprettSaksbehandler()
        val saksbehandler = saksbehandler(grupper = emptyList())
        val result =
            mediator.håndterTotrinnsvurdering(oppgaveId, saksbehandler, ApiVedtakUtfall.INNVILGELSE, "Begrunnelse")

        assertEquals(SendTilGodkjenningResult.Ok, result)
    }

    @Test
    fun `forsøk tildeling av oppgave`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.håndter(TildelOppgave(oppgaveId), saksbehandler)
        val melding = testRapid.inspektør.hendelser().last()
        assertEquals("oppgave_oppdatert", melding)
    }

    @Test
    fun `legg på vent forårsaker publisering av hendelse`() {
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(spleisBehandlingId = spleisBehandlingId)
        val oppgaveId = OPPGAVE_ID
        val frist = LocalDate.now()
        val skalTildeles = true
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                frist,
                skalTildeles,
                "en tekst",
                listOf(ApiPaVentRequest.ApiPaVentArsak("key", "arsak"))
            ), saksbehandler
        )
        val melding = testRapid.inspektør.hendelser("lagt_på_vent").lastOrNull()
        val årsaker = melding?.get("årsaker")?.map { it.get("key").asText() to it.get("årsak").asText() }
        assertNotNull(melding)
        assertEquals("lagt_på_vent", melding?.get("@event_name")?.asText())
        assertEquals("en tekst", melding?.get("notatTekst")?.asText())
        assertEquals(listOf("key" to "arsak"), årsaker)
        assertEquals(spleisBehandlingId, melding?.get("behandlingId")?.asUUID())
        assertEquals(oppgaveId, melding?.get("oppgaveId")?.asLong())
        assertEquals(saksbehandler.oid, melding?.get("saksbehandlerOid")?.asUUID())
        assertEquals(saksbehandler.ident, melding?.get("saksbehandlerIdent")?.asText())
        assertEquals(frist, melding?.get("frist")?.asLocalDate())
        assertEquals(skalTildeles, melding?.get("skalTildeles")?.asBoolean())
    }

    @Test
    fun `endring av påVent forårsaker publisering av hendelse`() {
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(spleisBehandlingId = spleisBehandlingId)
        val oppgaveId = OPPGAVE_ID
        val frist = LocalDate.now()
        val skalTildeles = true
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                frist,
                skalTildeles,
                "en tekst",
                listOf(ApiPaVentRequest.ApiPaVentArsak("key", "arsak"))
            ), saksbehandler
        )
        val melding1 = testRapid.inspektør.hendelser("lagt_på_vent").lastOrNull()
        assertEquals("lagt_på_vent", melding1?.get("@event_name")?.asText())

        val nyFrist = LocalDate.now().plusDays(5)
        mediator.påVent(
            ApiPaVentRequest.ApiEndrePaVent(
                oppgaveId,
                saksbehandler.oid,
                nyFrist,
                skalTildeles,
                "en ny tekst",
                listOf(ApiPaVentRequest.ApiPaVentArsak("key", "arsak"))
            ), saksbehandler
        )


        val melding2 = testRapid.inspektør.hendelser("lagt_på_vent").lastOrNull()
        val årsaker = melding2?.get("årsaker")?.map { it.get("key").asText() to it.get("årsak").asText() }
        assertNotNull(melding2)
        assertEquals("lagt_på_vent", melding2?.get("@event_name")?.asText())
        assertEquals("en ny tekst", melding2?.get("notatTekst")?.asText())
        assertEquals(listOf("key" to "arsak"), årsaker)
        assertEquals(spleisBehandlingId, melding2?.get("behandlingId")?.asUUID())
        assertEquals(oppgaveId, melding2?.get("oppgaveId")?.asLong())
        assertEquals(saksbehandler.oid, melding2?.get("saksbehandlerOid")?.asUUID())
        assertEquals(saksbehandler.ident, melding2?.get("saksbehandlerIdent")?.asText())
        assertEquals(nyFrist, melding2?.get("frist")?.asLocalDate())
        assertEquals(skalTildeles, melding2?.get("skalTildeles")?.asBoolean())
    }

    @Test
    fun `forsøk tildeling av oppgave når den allerede er tildelt`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.håndter(TildelOppgave(oppgaveId), saksbehandler)
        testRapid.reset()
        assertThrows<OppgaveTildeltNoenAndre> {
            mediator.håndter(TildelOppgave(oppgaveId), saksbehandler(UUID.randomUUID()))
        }
        assertEquals(0, testRapid.inspektør.hendelser().size)
    }

    @Test
    fun `forsøk avmelding av oppgave`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.håndter(TildelOppgave(oppgaveId), saksbehandler)
        testRapid.reset()
        mediator.håndter(AvmeldOppgave(oppgaveId), saksbehandler)
        val melding = testRapid.inspektør.hendelser().last()
        assertEquals("oppgave_oppdatert", melding)
    }

    @Test
    fun `forsøk avmelding av oppgave når den ikke er tildelt`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        assertThrows<OppgaveIkkeTildelt> {
            mediator.håndter(AvmeldOppgave(oppgaveId), saksbehandler(UUID.randomUUID()))
        }
        assertEquals(0, testRapid.inspektør.hendelser().size)
    }

    @Test
    fun `legg på vent`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(21),
                true,
                "notat tekst",
                listOf(
                    ApiPaVentRequest.ApiPaVentArsak("key", "arsak"),
                    ApiPaVentRequest.ApiPaVentArsak("key2", "arsak2"),
                ),
            ),
            saksbehandler,
        )
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
        val historikk = periodehistorikkApiDao.finn(UTBETALING_ID)
        assertEquals(PeriodehistorikkType.LEGG_PA_VENT, historikk.first().type)
        assertTrue(melding["egenskaper"].map { it.asText() }.contains("PÅ_VENT"))
    }

    @Test
    fun `endre på vent`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(10),
                true,
                "notat tekst",
                listOf(
                    ApiPaVentRequest.ApiPaVentArsak("key", "arsak"),
                    ApiPaVentRequest.ApiPaVentArsak("key2", "arsak2"),
                ),
            ),
            saksbehandler,
        )
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
        val historikk = periodehistorikkApiDao.finn(UTBETALING_ID)
        assertEquals(PeriodehistorikkType.LEGG_PA_VENT, historikk.first().type)
        assertTrue(melding["egenskaper"].map { it.asText() }.contains("PÅ_VENT"))

        mediator.påVent(
            ApiPaVentRequest.ApiEndrePaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(20),
                true,
                "ny notat tekst",
                listOf(
                    ApiPaVentRequest.ApiPaVentArsak("key", "arsak"),
                ),
            ),
            saksbehandler,
        )
        val melding2 = testRapid.inspektør.hendelser("lagt_på_vent").last()
        val historikk2 = periodehistorikkApiDao.finn(UTBETALING_ID).sortedBy { it.id }
        assertEquals(PeriodehistorikkType.ENDRE_PA_VENT, historikk2.last().type)
        assertEquals("ny notat tekst", melding2["notatTekst"].asText())
    }

    @Test
    fun `fjern på vent`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(21),
                false,
                "notat tekst",
                listOf(
                    ApiPaVentRequest.ApiPaVentArsak("key", "arsak"),
                    ApiPaVentRequest.ApiPaVentArsak("key2", "arsak2"),
                ),
            ),
            saksbehandler,
        )
        mediator.påVent(ApiPaVentRequest.ApiFjernPaVent(oppgaveId), saksbehandler)
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
        val historikk = periodehistorikkApiDao.finn(UTBETALING_ID)
        assertTrue(historikk.map { it.type }
            .containsAll(listOf(PeriodehistorikkType.FJERN_FRA_PA_VENT, PeriodehistorikkType.LEGG_PA_VENT)))
        assertFalse(melding["egenskaper"].map { it.asText() }.contains("PÅ_VENT"))
    }

    @Test
    fun `håndterer annullering`() {
        mediator.håndter(annullering(), saksbehandler)

        assertEquals(1, testRapid.inspektør.size)
        val melding = testRapid.inspektør.message(0)
        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(SAKSBEHANDLER.oid.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals(SAKSBEHANDLER.epost, melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(SAKSBEHANDLER.navn, melding["saksbehandler"]["navn"].asText())
        assertEquals(SAKSBEHANDLER.ident, melding["saksbehandler"]["ident"].asText())

        assertEquals(VEDTAKSPERIODE, melding["vedtaksperiodeId"].asUUID())
        assertEquals(UTBETALING_ID, melding["utbetalingId"].asUUID())
        assertEquals("EN_KOMMENTAR", melding["kommentar"]?.asText())
        assertEquals(1, melding["begrunnelser"].map { it.asText() }.size)
        assertEquals("EN_BEGRUNNELSE", melding["begrunnelser"][0].asText())
        assertEquals("Ferie", melding["arsaker"][0]["arsak"].asText())
        assertEquals("key01", melding["arsaker"][0]["key"].asText())
    }

    @Test
    fun `håndterer annullering uten kommentar, begrunnelser eller årsak`() {
        mediator.håndter(annullering(emptyList(), null, emptyList()), saksbehandler)

        val melding = testRapid.inspektør.message(0)

        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(SAKSBEHANDLER.oid.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals(SAKSBEHANDLER.epost, melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(SAKSBEHANDLER.navn, melding["saksbehandler"]["navn"].asText())
        assertEquals(SAKSBEHANDLER.ident, melding["saksbehandler"]["ident"].asText())

        assertEquals(VEDTAKSPERIODE, melding["vedtaksperiodeId"].asUUID())
        assertEquals(UTBETALING_ID, melding["utbetalingId"].asUUID())
        assertEquals(null, melding["kommentar"]?.asText())
        assertEquals(0, melding["begrunnelser"].map { it.asText() }.size)
        assertEquals("", melding["arsaker"].asText())
    }

    @Test
    fun `godtar ikke å annullere samme utbetaling mer enn 1 gang`() {
        val annullering = annullering(emptyList(), null)
        mediator.håndter(annullering, saksbehandler)
        assertThrows<no.nav.helse.spesialist.api.feilhåndtering.AlleredeAnnullert> {
            mediator.håndter(annullering, saksbehandler)
        }
    }

    // Eksperimentering med DSL for å lage testdata
    // Slett hvis du oppdager denne koden og den ikke er tatt i bruk andre steder 😂
    // Plassert her pga. ren og skjær tilfeldighet
    private data class PERSON(
        val fødselsnummer: String,
        val aktørId: String,
        val arbeidsgiver: List<ARBEIDSGIVER>,
    ) {
        val Int.ag: String
            get() = arbeidsgiver[this - 1].orgnr

        operator fun <T> invoke(func: PERSON.() -> T) = func()
    }

    private data class PERSONBUILDER(
        var fødselsnummer: String,
        var aktørId: String,
        var arbeidsgivere: List<ARBEIDSGIVER>,
    ) {
        fun build() = PERSON(fødselsnummer, aktørId, arbeidsgivere)
    }

    private fun person(init: PERSONBUILDER.() -> Unit): PERSON {
        val builder = PERSONBUILDER(lagFødselsnummer(), lagAktørId(), arbeidsgivere(1))
        builder.init()
        return builder.build()
    }

    private data class ARBEIDSGIVERBUILDER(
        var orgnrs: List<String>,
    ) {
        fun build() = orgnrs.map(SaksbehandlerMediatorTest::ARBEIDSGIVER)
    }

    private data class ARBEIDSGIVER(
        val orgnr: String,
    )

    private fun arbeidsgivere(
        antall: Int,
        init: ARBEIDSGIVERBUILDER.() -> Unit = {},
    ): List<ARBEIDSGIVER> {
        val builder = ARBEIDSGIVERBUILDER(List(antall) { lagOrganisasjonsnummer() }.toList())
        builder.init()
        return builder.build()
    }

    @Test
    fun `håndterer overstyring av tidslinje`() {
        val person =
            person {
                arbeidsgivere = arbeidsgivere(2)
            }
        nyPerson(fødselsnummer = person.fødselsnummer, aktørId = person.aktørId, organisasjonsnummer = person { 2.ag })

        val overstyring =
            ApiTidslinjeOverstyring(
                vedtaksperiodeId = UUID.randomUUID(),
                organisasjonsnummer = person { 2.ag },
                fodselsnummer = person.fødselsnummer,
                aktorId = person.aktørId,
                begrunnelse = "En begrunnelse",
                dager =
                    listOf(
                        ApiOverstyringDag(
                            dato = 10.januar,
                            type = "Sykedag",
                            fraType = "Arbeidsdag",
                            grad = null,
                            fraGrad = 100,
                            null,
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val hendelse = testRapid.inspektør.hendelser("overstyr_tidslinje").first()
        val overstyringId = finnOverstyringId(person.fødselsnummer)

        assertNotNull(overstyringId)
        assertEquals(overstyringId.toString(), hendelse["@id"].asText())
        assertEquals(person.fødselsnummer, hendelse["fødselsnummer"].asText())
        assertEquals(person { 2.ag }, hendelse["organisasjonsnummer"].asText())

        val overstyrtDag = hendelse["dager"].toList().single()
        assertEquals(10.januar, overstyrtDag["dato"].asLocalDate())
        assertEquals("Sykedag", overstyrtDag["type"].asText())
        assertEquals("Arbeidsdag", overstyrtDag["fraType"].asText())
        assertEquals(null, overstyrtDag["grad"]?.textValue())
        assertEquals(100, overstyrtDag["fraGrad"].asInt())
    }

    @Test
    fun `håndterer overstyring av arbeidsforhold`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØR_ID, organisasjonsnummer = ORGANISASJONSNUMMER)
        val overstyring =
            ApiArbeidsforholdOverstyringHandling(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                vedtaksperiodeId = UUID.randomUUID(),
                overstyrteArbeidsforhold =
                    listOf(
                        ApiOverstyringArbeidsforhold(
                            orgnummer = ORGANISASJONSNUMMER_GHOST,
                            deaktivert = true,
                            begrunnelse = "en begrunnelse",
                            forklaring = "en forklaring",
                            lovhjemmel = ApiLovhjemmel("8-15", null, null, "folketrygdloven", "1998-12-18"),
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val hendelse = testRapid.inspektør.hendelser("overstyr_arbeidsforhold").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER.oid, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER.navn, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER.ident, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER.epost, hendelse["saksbehandlerEpost"].asText())
        assertEquals(1.januar, hendelse["skjæringstidspunkt"].asLocalDate())

        val overstyrtArbeidsforhold = hendelse["overstyrteArbeidsforhold"].toList().single()
        assertEquals("en begrunnelse", overstyrtArbeidsforhold["begrunnelse"].asText())
        assertEquals("en forklaring", overstyrtArbeidsforhold["forklaring"].asText())
        assertEquals(ORGANISASJONSNUMMER_GHOST, overstyrtArbeidsforhold["orgnummer"].asText())
        assertEquals(false, overstyrtArbeidsforhold["orgnummer"].asBoolean())
    }

    @Test
    fun `håndterer overstyring av inntekt og refusjon`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØR_ID, organisasjonsnummer = ORGANISASJONSNUMMER)
        val overstyring =
            ApiInntektOgRefusjonOverstyring(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                vedtaksperiodeId = UUID.randomUUID(),
                arbeidsgivere =
                    listOf(
                        ApiOverstyringArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER,
                            manedligInntekt = 25000.0,
                            fraManedligInntekt = 25001.0,
                            refusjonsopplysninger =
                                listOf(
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.januar, 31.januar, 25000.0),
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.februar, null, 24000.0),
                                ),
                            fraRefusjonsopplysninger =
                                listOf(
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.januar, 31.januar, 24000.0),
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.februar, null, 23000.0),
                                ),
                            lovhjemmel = ApiLovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            begrunnelse = "En begrunnelse",
                            forklaring = "En forklaring",
                            fom = null,
                            tom = null,
                        ),
                        ApiOverstyringArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                            manedligInntekt = 21000.0,
                            fraManedligInntekt = 25001.0,
                            refusjonsopplysninger =
                                listOf(
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.januar, 31.januar, 21000.0),
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.februar, null, 22000.0),
                                ),
                            fraRefusjonsopplysninger =
                                listOf(
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.januar, 31.januar, 22000.0),
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.februar, null, 23000.0),
                                ),
                            lovhjemmel = ApiLovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            begrunnelse = "En begrunnelse 2",
                            forklaring = "En forklaring 2",
                            fom = null,
                            tom = null,
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)

        val hendelse = testRapid.inspektør.hendelser("overstyr_inntekt_og_refusjon").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER.oid, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER.navn, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER.ident, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER.epost, hendelse["saksbehandlerEpost"].asText())
        assertEquals(1.januar, hendelse["skjæringstidspunkt"].asLocalDate())
        hendelse["arbeidsgivere"].first().let {
            assertEquals(ORGANISASJONSNUMMER, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelse", it["begrunnelse"].asText())
            assertEquals("En forklaring", it["forklaring"].asText())
            assertEquals(25000.0, it["månedligInntekt"].asDouble())
            assertEquals(2, it["refusjonsopplysninger"].size())
            assertEquals("2018-01-01", it["refusjonsopplysninger"].first()["fom"].asText())
            assertEquals("2018-01-31", it["refusjonsopplysninger"].first()["tom"].asText())
            assertEquals(25000.0, it["refusjonsopplysninger"].first()["beløp"].asDouble())
            assertEquals(24000.0, it["fraRefusjonsopplysninger"].first()["beløp"].asDouble())
        }
        hendelse["arbeidsgivere"].last().let {
            assertEquals(ORGANISASJONSNUMMER_GHOST, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelse 2", it["begrunnelse"].asText())
            assertEquals("En forklaring 2", it["forklaring"].asText())
            assertEquals(21000.0, it["månedligInntekt"].asDouble())
            assertEquals(2, it["refusjonsopplysninger"].size())
            assertEquals("2018-01-01", it["refusjonsopplysninger"].first()["fom"].asText())
            assertEquals("2018-01-31", it["refusjonsopplysninger"].first()["tom"].asText())
            assertEquals(21000.0, it["refusjonsopplysninger"].first()["beløp"].asDouble())
            assertEquals(22000.0, it["fraRefusjonsopplysninger"].first()["beløp"].asDouble())
        }
    }

    @Test
    fun `håndterer skjønnsfastsetting av sykepengegrunnlag`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØR_ID, organisasjonsnummer = ORGANISASJONSNUMMER)
        val skjønnsfastsetting =
            ApiSkjonnsfastsettelse(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                arbeidsgivere =
                    listOf(
                        ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER,
                            arlig = 25000.0,
                            fraArlig = 25001.0,
                            lovhjemmel = ApiLovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            arsak = "En årsak",
                            type = ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.OMREGNET_ARSINNTEKT,
                            begrunnelseMal = "En begrunnelsemal",
                            begrunnelseFritekst = "begrunnelsefritekst",
                            begrunnelseKonklusjon = "begrunnelseKonklusjon",
                            initierendeVedtaksperiodeId = PERIODE.id.toString(),
                        ),
                        ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                            arlig = 21000.0,
                            fraArlig = 25001.0,
                            lovhjemmel = ApiLovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            arsak = "En årsak 2",
                            type = ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.OMREGNET_ARSINNTEKT,
                            begrunnelseMal = "En begrunnelsemal",
                            begrunnelseFritekst = "begrunnelsefritekst",
                            begrunnelseKonklusjon = "begrunnelseKonklusjon",
                            initierendeVedtaksperiodeId = UUID.randomUUID().toString(),
                        ),
                    ),
                vedtaksperiodeId = PERIODE.id,
            )

        mediator.håndter(skjønnsfastsetting, saksbehandler)

        val hendelse = testRapid.inspektør.hendelser("skjønnsmessig_fastsettelse").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER.oid, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER.navn, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER.ident, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER.epost, hendelse["saksbehandlerEpost"].asText())
        assertEquals(1.januar, hendelse["skjæringstidspunkt"].asLocalDate())
        hendelse["arbeidsgivere"].first().let {
            assertEquals(ORGANISASJONSNUMMER, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelsemal", it["begrunnelseMal"].asText())
            assertEquals("begrunnelsefritekst", it["begrunnelseFritekst"].asText())
            assertEquals("begrunnelseKonklusjon", it["begrunnelseKonklusjon"].asText())
            assertEquals("En årsak", it["årsak"].asText())
            assertEquals(25000.0, it["årlig"].asDouble())
            assertEquals(25001.0, it["fraÅrlig"].asDouble())
        }
        hendelse["arbeidsgivere"].last().let {
            assertEquals(ORGANISASJONSNUMMER_GHOST, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelsemal", it["begrunnelseMal"].asText())
            assertEquals("begrunnelsefritekst", it["begrunnelseFritekst"].asText())
            assertEquals("begrunnelseKonklusjon", it["begrunnelseKonklusjon"].asText())
            assertEquals("En årsak 2", it["årsak"].asText())
            assertEquals(21000.0, it["årlig"].asDouble())
            assertEquals(25001.0, it["fraÅrlig"].asDouble())
        }
    }

    @Test
    fun `håndterer vurdering av minimum sykdomsgrad`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, organisasjonsnummer = ORGANISASJONSNUMMER, aktørId = AKTØR_ID)
        val minimumSykdomsgrad =
            ApiMinimumSykdomsgrad(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                perioderVurdertOk = listOf(
                    ApiMinimumSykdomsgrad.ApiPeriode(
                        fom = 1.januar,
                        tom = 15.januar
                    ), ApiMinimumSykdomsgrad.ApiPeriode(
                        fom = 30.januar,
                        tom = 31.januar
                    )
                ),
                perioderVurdertIkkeOk = listOf(
                    ApiMinimumSykdomsgrad.ApiPeriode(
                        fom = 16.januar,
                        tom = 29.januar
                    )
                ),
                begrunnelse = "en begrunnelse",
                arbeidsgivere =
                    listOf(
                        ApiMinimumSykdomsgrad.ApiArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER,
                            berortVedtaksperiodeId = PERIODE.id,
                        ),
                    ),
                initierendeVedtaksperiodeId = PERIODE.id,
            )

        mediator.håndter(minimumSykdomsgrad, saksbehandler)

        val hendelse = testRapid.inspektør.hendelser("minimum_sykdomsgrad_vurdert").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER.oid, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER.navn, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER.ident, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER.epost, hendelse["saksbehandlerEpost"].asText())
        hendelse["perioderMedMinimumSykdomsgradVurdertOk"].first().let {
            assertEquals(1.januar, it["fom"].asLocalDate())
            assertEquals(15.januar, it["tom"].asLocalDate())
        }
        hendelse["perioderMedMinimumSykdomsgradVurdertOk"].last().let {
            assertEquals(30.januar, it["fom"].asLocalDate())
            assertEquals(31.januar, it["tom"].asLocalDate())
        }
        hendelse["perioderMedMinimumSykdomsgradVurdertIkkeOk"].first().let {
            assertEquals(16.januar, it["fom"].asLocalDate())
            assertEquals(29.januar, it["tom"].asLocalDate())
        }
    }

    @Test
    fun `opphev stans`() {
        nyPerson()
        mediator.håndter(ApiOpphevStans(FNR, "EN_ÅRSAK"), saksbehandler)
        assertStansOpphevet(FNR)
    }

    private fun assertStansOpphevet(fødselsnummer: String) {
        val status =
            dbQuery.single(
                "select status from stans_automatisering where fødselsnummer = :fnr",
                "fnr" to fødselsnummer,
            ) { it.string(1) }
        assertEquals("NORMAL", status)
    }

    private fun finnOverstyringId(fødselsnummer: String): UUID {
        return dbQuery.single(
            "select ekstern_hendelse_id from overstyring where person_ref = (select id from person where fødselsnummer = :fodselsnummer)",
            "fodselsnummer" to fødselsnummer
        ) { it.uuid("ekstern_hendelse_id") }
    }

    private fun godkjenning(
        oppgavereferanse: Long,
        godkjent: Boolean,
        ident: String = SAKSBEHANDLER.ident,
        avslag: Avslag? = null,
    ) = GodkjenningDto(
        oppgavereferanse = oppgavereferanse,
        saksbehandlerIdent = ident,
        godkjent = godkjent,
        begrunnelser = emptyList(),
        kommentar = if (!godkjent) "Kommentar" else null,
        årsak = if (!godkjent) "Årsak" else null,
        avslag = avslag,
    )

    private fun annullering(
        begrunnelser: List<String> = listOf("EN_BEGRUNNELSE"),
        kommentar: String? = "EN_KOMMENTAR",
        arsaker: List<ApiAnnulleringArsak> =
            listOf(ApiAnnulleringArsak(_key = "key01", arsak = "Ferie"), ApiAnnulleringArsak(_key = "key02", arsak = "Perm")),
    ) = ApiAnnulleringData(
        aktorId = AKTØR_ID,
        fodselsnummer = FØDSELSNUMMER,
        organisasjonsnummer = ORGANISASJONSNUMMER,
        vedtaksperiodeId = VEDTAKSPERIODE,
        utbetalingId = UTBETALING_ID,
        arbeidsgiverFagsystemId = "EN-FAGSYSTEMID${Random.nextInt(1000)}",
        personFagsystemId = "EN-FAGSYSTEMID${Random.nextInt(1000)}",
        begrunnelser = begrunnelser,
        arsaker = arsaker,
        kommentar = kommentar,
    )
}
