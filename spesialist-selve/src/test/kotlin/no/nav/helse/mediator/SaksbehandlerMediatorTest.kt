package no.nav.helse.mediator

import DatabaseIntegrationTest
import TilgangskontrollForTestHarIkkeTilgang
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.SpeilTilgangsgrupper
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.mutation.Avslag
import no.nav.helse.spesialist.api.graphql.mutation.Avslagsdata
import no.nav.helse.spesialist.api.graphql.mutation.Avslagshandling
import no.nav.helse.spesialist.api.graphql.mutation.Avslagstype
import no.nav.helse.spesialist.api.graphql.schema.AnnulleringArsak
import no.nav.helse.spesialist.api.graphql.schema.AnnulleringData
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.InntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.Lovhjemmel
import no.nav.helse.spesialist.api.graphql.schema.MinimumSykdomsgrad
import no.nav.helse.spesialist.api.graphql.schema.OverstyringArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.OverstyringArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.OverstyringDag
import no.nav.helse.spesialist.api.graphql.schema.PaVentRequest
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.TidslinjeOverstyring
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OpphevStans
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.testEnv
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

internal class SaksbehandlerMediatorTest : DatabaseIntegrationTest() {
    private val tilgangsgrupper = SpeilTilgangsgrupper(testEnv)
    private val testRapid = TestRapid()
    private val tildelingDbDao =
        no.nav.helse.db
            .TildelingDao(dataSource)
    private val saksbehandlerRepository = SaksbehandlerDao(dataSource)
    private val stansAutomatiskBehandlingMediator =
        StansAutomatiskBehandlingMediator(
            stansAutomatiskBehandlingDao,
            pgHistorikkinnslagRepository,
            oppgaveDao,
            notatDao,
        ) { Subsumsjonsmelder("versjonAvKode", testRapid) }
    private val oppgaveService =
        OppgaveService(
            meldingDao,
            oppgaveDao,
            tildelingDbDao,
            reservasjonDao,
            opptegnelseDao,
            totrinnsvurderingDao,
            saksbehandlerRepository,
            testRapid,
            TilgangskontrollForTestHarIkkeTilgang,
            tilgangsgrupper,
        )
    private val mediator =
        SaksbehandlerMediator(dataSource, "versjonAvKode", testRapid, oppgaveService, tilgangsgrupper, stansAutomatiskBehandlingMediator)

    private val AKTØR_ID = lagAktørId()
    private val FØDSELSNUMMER = lagFødselsnummer()
    private val ORGANISASJONSNUMMER = lagOrganisasjonsnummer()
    private val ORGANISASJONSNUMMER_GHOST = lagOrganisasjonsnummer()

    override val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    override val SAKSBEHANDLER_NAVN = "ET_NAVN"
    override val SAKSBEHANDLER_IDENT = "EN_IDENT"
    override val SAKSBEHANDLER_EPOST = "epost@nav.no"

    private val saksbehandler = saksbehandler()

    private fun saksbehandler(
        oid: UUID = SAKSBEHANDLER_OID,
        navn: String = SAKSBEHANDLER_NAVN,
        epost: String = SAKSBEHANDLER_EPOST,
        ident: String = SAKSBEHANDLER_IDENT,
        grupper: List<UUID> = emptyList(),
    ): SaksbehandlerFraApi = SaksbehandlerFraApi(oid, navn, epost, ident, grupper)

    @BeforeEach
    internal fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `håndter godkjenning`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "VURDERT",
        )
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            kode = "EN_ANNEN_KODE",
            definisjonRef = definisjonRef,
            status = "VURDERT",
        )
        assertDoesNotThrow {
            mediator.håndter(godkjenning(oppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(vedtaksperiodeId, 2)
    }

    @Test
    fun `håndter godkjenning når periode har aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)

        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "AKTIV",
        )
        assertThrows<ManglerVurderingAvVarsler> {
            mediator.håndter(godkjenning(oppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(vedtaksperiodeId, 0)
    }

    @Test
    fun `håndter godkjenning når periode ikke har noen varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)

        assertDoesNotThrow {
            mediator.håndter(godkjenning(OPPGAVE_ID, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(vedtaksperiodeId, 0)
    }

    @Test
    fun `invalider eksisterende oppgave ved overstyring`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        mediator.håndter(
            TidslinjeOverstyring(VEDTAKSPERIODE, ORGANISASJONSNUMMER, FNR, AKTØR, "", dager = emptyList()),
            saksbehandler,
        )
        assertOppgave(OPPGAVE_ID, "Invalidert")
    }

    @Test
    fun `håndter godkjenning når godkjenning er avvist`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)

        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "AKTIV",
        )
        mediator.håndter(godkjenning(oppgaveId, false), UUID.randomUUID(), saksbehandler)
        assertGodkjenteVarsler(vedtaksperiodeId, 0)
        assertAvvisteVarsler(vedtaksperiodeId, 1)
    }

    @Test
    fun `håndter totrinnsvurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "VURDERT",
        )
        assertDoesNotThrow {
            mediator.håndterTotrinnsvurdering(oppgaveId)
        }
    }

    @Test
    fun `håndter totrinnsvurdering når periode har aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "AKTIV",
        )
        assertThrows<ManglerVurderingAvVarsler> {
            mediator.håndterTotrinnsvurdering(oppgaveId)
        }
    }

    @Test
    fun `håndter totrinnsvurdering når periode ikke har noen varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        assertDoesNotThrow {
            mediator.håndterTotrinnsvurdering(oppgaveId)
        }
    }

    @Test
    fun `håndterer godkjenning med avslag`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)

        assertDoesNotThrow {
            mediator.håndter(
                godkjenning(
                    oppgavereferanse = oppgaveId,
                    godkjent = true,
                    avslag =
                        Avslag(
                            handling = Avslagshandling.OPPRETT,
                            data = Avslagsdata(Avslagstype.AVSLAG, "En individuell begrunnelse"),
                        ),
                ),
                UUID.randomUUID(),
                saksbehandler,
            )
        }
    }

    @Test
    fun `håndterer på vent`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        påVentDao.lagrePåVent(oppgaveId, saksbehandler.oid, LocalDate.now())
        assertDoesNotThrow {
            mediator.håndter(godkjenning(oppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertFalse(påVentDao.erPåVent(vedtaksperiodeId))
    }

    @Test
    fun `sender ut varsel_endret ved godkjenning av varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon(tittel = "EN_TITTEL")
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = vedtaksperiodeId,
            kode = "EN_KODE",
            definisjonRef = definisjonRef,
            status = "VURDERT",
        )
        mediator.håndter(godkjenning(oppgaveId, true), behandlingId, saksbehandler)

        assertEquals(1, testRapid.inspektør.size)
        val melding = testRapid.inspektør.message(0)
        assertEquals("varsel_endret", melding["@event_name"].asText())
        assertEquals(vedtaksperiodeId.toString(), melding["vedtaksperiode_id"].asText())
        assertEquals(behandlingId.toString(), melding["behandling_id"].asText())
        assertEquals(varselId.toString(), melding["varsel_id"].asText())
        assertEquals("EN_TITTEL", melding["varseltittel"].asText())
        assertEquals("EN_KODE", melding["varselkode"].asText())
        assertEquals("GODKJENT", melding["gjeldende_status"].asText())
        assertEquals("VURDERT", melding["forrige_status"].asText())
    }

    @Test
    fun `sender ut varsel_endret ved avvisning av varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon(tittel = "EN_TITTEL")
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = vedtaksperiodeId,
            kode = "EN_KODE",
            definisjonRef = definisjonRef,
            status = "AKTIV",
        )
        mediator.håndter(godkjenning(oppgaveId, false), behandlingId, saksbehandler)

        assertEquals(1, testRapid.inspektør.size)
        val melding = testRapid.inspektør.message(0)
        assertEquals("varsel_endret", melding["@event_name"].asText())
        assertEquals(vedtaksperiodeId.toString(), melding["vedtaksperiode_id"].asText())
        assertEquals(behandlingId.toString(), melding["behandling_id"].asText())
        assertEquals(varselId.toString(), melding["varsel_id"].asText())
        assertEquals("EN_TITTEL", melding["varseltittel"].asText())
        assertEquals("EN_KODE", melding["varselkode"].asText())
        assertEquals("AVVIST", melding["gjeldende_status"].asText())
        assertEquals("AKTIV", melding["forrige_status"].asText())
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
            PaVentRequest.LeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(21),
                true,
                "notat tekst",
                listOf(
                    PaVentRequest.PaVentArsak("key", "arsak"),
                    PaVentRequest.PaVentArsak("key2", "arsak2"),
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
    fun `fjern på vent`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.påVent(
            PaVentRequest.LeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(21),
                false,
                "notat tekst",
                listOf(
                    PaVentRequest.PaVentArsak("key", "arsak"),
                    PaVentRequest.PaVentArsak("key2", "arsak2"),
                ),
            ),
            saksbehandler,
        )
        mediator.påVent(PaVentRequest.FjernPaVent(oppgaveId), saksbehandler)
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
        val historikk = periodehistorikkApiDao.finn(UTBETALING_ID)
        assertTrue(historikk.map { it.type }.containsAll(listOf(PeriodehistorikkType.FJERN_FRA_PA_VENT, PeriodehistorikkType.LEGG_PA_VENT)))
        assertFalse(melding["egenskaper"].map { it.asText() }.contains("PÅ_VENT"))
    }

    @Test
    fun `håndterer annullering`() {
        mediator.håndter(annullering(), saksbehandler)

        assertEquals(1, testRapid.inspektør.size)
        val melding = testRapid.inspektør.message(0)
        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(SAKSBEHANDLER_OID.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(SAKSBEHANDLER_NAVN, melding["saksbehandler"]["navn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, melding["saksbehandler"]["ident"].asText())

        assertEquals(VEDTAKSPERIODE, melding["vedtaksperiodeId"].asUUID())
        assertEquals(UTBETALING_ID, melding["utbetalingId"].asUUID())
        assertEquals("EN_KOMMENTAR", melding["kommentar"]?.asText())
        assertEquals(1, melding["begrunnelser"].map { it.asText() }.size)
        assertEquals("EN_BEGRUNNELSE", melding["begrunnelser"][0].asText())
        melding["arsaker"].let { årsaker ->
            assertEquals(2, årsaker.size())
            årsaker.first().let { node ->
                assertEquals("Ferie", node["arsak"].asText())
                assertEquals("key01", node["key"].asText())
            }
        }
    }

    @Test
    fun `håndterer annullering uten kommentar, begrunnelser eller årsak`() {
        mediator.håndter(annullering(emptyList(), null, emptyList()), saksbehandler)

        val melding = testRapid.inspektør.message(0)

        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(SAKSBEHANDLER_OID.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(SAKSBEHANDLER_NAVN, melding["saksbehandler"]["navn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, melding["saksbehandler"]["ident"].asText())

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
        fun build() = orgnrs.map(::ARBEIDSGIVER)
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
            TidslinjeOverstyring(
                vedtaksperiodeId = UUID.randomUUID(),
                organisasjonsnummer = person { 2.ag },
                fodselsnummer = person.fødselsnummer,
                aktorId = person.aktørId,
                begrunnelse = "En begrunnelse",
                dager =
                    listOf(
                        OverstyringDag(
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
            ArbeidsforholdOverstyringHandling(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                vedtaksperiodeId = UUID.randomUUID(),
                overstyrteArbeidsforhold =
                    listOf(
                        OverstyringArbeidsforhold(
                            orgnummer = ORGANISASJONSNUMMER_GHOST,
                            deaktivert = true,
                            begrunnelse = "en begrunnelse",
                            forklaring = "en forklaring",
                            lovhjemmel = Lovhjemmel("8-15", null, null, "folketrygdloven", "1998-12-18"),
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val hendelse = testRapid.inspektør.hendelser("overstyr_arbeidsforhold").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
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
            InntektOgRefusjonOverstyring(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                vedtaksperiodeId = UUID.randomUUID(),
                arbeidsgivere =
                    listOf(
                        OverstyringArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER,
                            manedligInntekt = 25000.0,
                            fraManedligInntekt = 25001.0,
                            refusjonsopplysninger =
                                listOf(
                                    OverstyringArbeidsgiver.OverstyringRefusjonselement(1.januar, 31.januar, 25000.0),
                                    OverstyringArbeidsgiver.OverstyringRefusjonselement(1.februar, null, 24000.0),
                                ),
                            fraRefusjonsopplysninger =
                                listOf(
                                    OverstyringArbeidsgiver.OverstyringRefusjonselement(1.januar, 31.januar, 24000.0),
                                    OverstyringArbeidsgiver.OverstyringRefusjonselement(1.februar, null, 23000.0),
                                ),
                            lovhjemmel = Lovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            begrunnelse = "En begrunnelse",
                            forklaring = "En forklaring",
                            fom = null,
                            tom = null,
                        ),
                        OverstyringArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                            manedligInntekt = 21000.0,
                            fraManedligInntekt = 25001.0,
                            refusjonsopplysninger =
                                listOf(
                                    OverstyringArbeidsgiver.OverstyringRefusjonselement(1.januar, 31.januar, 21000.0),
                                    OverstyringArbeidsgiver.OverstyringRefusjonselement(1.februar, null, 22000.0),
                                ),
                            fraRefusjonsopplysninger =
                                listOf(
                                    OverstyringArbeidsgiver.OverstyringRefusjonselement(1.januar, 31.januar, 22000.0),
                                    OverstyringArbeidsgiver.OverstyringRefusjonselement(1.februar, null, 23000.0),
                                ),
                            lovhjemmel = Lovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
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
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
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
            Skjonnsfastsettelse(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                arbeidsgivere =
                    listOf(
                        Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER,
                            arlig = 25000.0,
                            fraArlig = 25001.0,
                            lovhjemmel = Lovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            arsak = "En årsak",
                            type = Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.OMREGNET_ARSINNTEKT,
                            begrunnelseMal = "En begrunnelsemal",
                            begrunnelseFritekst = "begrunnelsefritekst",
                            begrunnelseKonklusjon = "begrunnelseKonklusjon",
                            initierendeVedtaksperiodeId = PERIODE.id.toString(),
                        ),
                        Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                            arlig = 21000.0,
                            fraArlig = 25001.0,
                            lovhjemmel = Lovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            arsak = "En årsak 2",
                            type = Skjonnsfastsettelse.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.OMREGNET_ARSINNTEKT,
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
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
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
    fun `håndterer vurdering ok av minimum sykdomsgrad`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, organisasjonsnummer = ORGANISASJONSNUMMER, aktørId = AKTØR_ID)
        val minimumSykdomsgrad =
            MinimumSykdomsgrad(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                fom = 1.januar,
                tom = 31.januar,
                vurdering = true,
                begrunnelse = "en begrunnelse",
                arbeidsgivere =
                    listOf(
                        MinimumSykdomsgrad.Arbeidsgiver(
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
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
        hendelse["perioderMedMinimumSykdomsgradVurdertOk"].first().let {
            assertEquals(1.januar, it["fom"].asLocalDate())
            assertEquals(31.januar, it["tom"].asLocalDate())
        }
        assertTrue(hendelse["perioderMedMinimumSykdomsgradVurdertIkkeOk"].isEmpty)
    }

    @Test
    fun `håndterer vurdering ikke ok av minimum sykdomsgrad`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, organisasjonsnummer = ORGANISASJONSNUMMER, aktørId = AKTØR_ID)
        val minimumSykdomsgrad =
            MinimumSykdomsgrad(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                fom = 1.januar,
                tom = 31.januar,
                vurdering = false,
                begrunnelse = "en begrunnelse",
                arbeidsgivere =
                    listOf(
                        MinimumSykdomsgrad.Arbeidsgiver(
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
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
        hendelse["perioderMedMinimumSykdomsgradVurdertIkkeOk"].first().let {
            assertEquals(1.januar, it["fom"].asLocalDate())
            assertEquals(31.januar, it["tom"].asLocalDate())
        }
        assertTrue(hendelse["perioderMedMinimumSykdomsgradVurdertOk"].isEmpty)
    }

    @Test
    fun `opphev stans`() {
        nyPerson()
        mediator.håndter(OpphevStans(FNR, "EN_ÅRSAK"), saksbehandler)
        assertStansOpphevet(FNR)
    }

    private fun assertStansOpphevet(fødselsnummer: String) {
        val status =
            query(
                "select status from stans_automatisering where fødselsnummer = :fnr",
                "fnr" to fødselsnummer,
            ).single { it.string(1) }
        assertEquals("NORMAL", status)
    }

    private fun finnOverstyringId(fødselsnummer: String): UUID? {
        @Language("PostgreSQL")
        val query =
            " select ekstern_hendelse_id from overstyring where person_ref = (select id from person where fodselsnummer = :fodselsnummer) "

        return sessionOf(dataSource).use {
            it.run(
                queryOf(
                    query,
                    mapOf("fodselsnummer" to fødselsnummer.toLong()),
                ).map { it.uuid("ekstern_hendelse_id") }.asSingle,
            )
        }
    }

    private fun assertOppgave(
        oppgaveId: Long,
        forventetStatus: String,
    ) {
        @Language("PostgreSQL")
        val query = "SELECT status FROM oppgave WHERE id = ?"
        val status =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, oppgaveId).map { it.string(1) }.asSingle)
            }
        assertEquals(forventetStatus, status)
    }

    private fun godkjenning(
        oppgavereferanse: Long,
        godkjent: Boolean,
        ident: String = SAKSBEHANDLER_IDENT,
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
        arsaker: List<AnnulleringArsak> =
            listOf(AnnulleringArsak(_key = "key01", arsak = "Ferie"), AnnulleringArsak(_key = "key02", arsak = "Perm")),
    ) = AnnulleringData(
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
