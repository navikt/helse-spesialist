package no.nav.helse.mediator

import DatabaseIntegrationTest
import TilgangskontrollForTestHarIkkeTilgang
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.mutation.Avslag
import no.nav.helse.spesialist.api.graphql.mutation.Avslagstype
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AnnulleringHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.FjernPåVent
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LovhjemmelFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi.SkjønnsfastsettingstypeDto
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
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


internal class SaksbehandlerMediatorTest: DatabaseIntegrationTest() {
    private val tilgangsgrupper = Tilgangsgrupper(testEnv)
    private val testRapid = TestRapid()
    private val tildelingDbDao = no.nav.helse.db.TildelingDao(dataSource)
    private val saksbehandlerRepository = SaksbehandlerDao(dataSource)
    private val oppgaveMediator = OppgaveMediator(meldingDao, oppgaveDao, tildelingDbDao, reservasjonDao, opptegnelseDao, totrinnsvurderingDao, saksbehandlerRepository, testRapid, TilgangskontrollForTestHarIkkeTilgang, tilgangsgrupper)
    private val mediator = SaksbehandlerMediator(dataSource, "versjonAvKode", testRapid, oppgaveMediator, tilgangsgrupper)

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
        grupper: List<UUID> = emptyList()
    ): SaksbehandlerFraApi = SaksbehandlerFraApi(oid, navn, epost, ident, grupper)

    @BeforeEach
    internal fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `håndter godkjenning`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId, status = "VURDERT", definisjonRef = definisjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId, status = "VURDERT", definisjonRef = definisjonRef)
        assertDoesNotThrow {
            mediator.håndter(godkjenning(oppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(generasjonId, 2)
    }

    @Test
    fun `håndter godkjenning når periode har aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)

        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId, status = "AKTIV", definisjonRef = definisjonRef)
        assertThrows<ManglerVurderingAvVarsler> {
            mediator.håndter(godkjenning(oppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(generasjonId, 0)
    }

    @Test
    fun `håndter godkjenning når periode ikke har noen varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)

        assertDoesNotThrow {
            mediator.håndter(godkjenning(OPPGAVE_ID, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(generasjonId, 0)
    }

    @Test
    fun `invalider eksisterende oppgave ved overstyring`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)
        mediator.håndter(OverstyrTidslinjeHandlingFraApi(VEDTAKSPERIODE, ORGANISASJONSNUMMER, FNR, AKTØR, "", dager = emptyList()), saksbehandler)
        assertOppgave(OPPGAVE_ID, "Invalidert")
    }

    @Test
    fun `håndter godkjenning når godkjenning er avvist`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)

        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId, status = "AKTIV", definisjonRef = definisjonRef)
        mediator.håndter(godkjenning(oppgaveId, false), UUID.randomUUID(), saksbehandler)
        assertGodkjenteVarsler(generasjonId, 0)
        assertAvvisteVarsler(generasjonId, 1)
    }

    @Test
    fun `håndter totrinnsvurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId, status = "VURDERT", definisjonRef = definisjonRef)
        assertDoesNotThrow {
            mediator.håndterTotrinnsvurdering(oppgaveId)
        }
    }

    @Test
    fun `håndter totrinnsvurdering når periode har aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId, status = "AKTIV", definisjonRef = definisjonRef)
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
        val generasjonId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)

        assertDoesNotThrow {
            mediator.håndter(godkjenning(oppgavereferanse = oppgaveId, godkjent = true, avslag = Avslag(Avslagstype.AVSLAG, "En individuell begrunnelse")), UUID.randomUUID(), saksbehandler)
        }
    }

    @Test
    fun `håndterer på vent`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)
        påVentDao.lagrePåVent(oppgaveId, saksbehandler.oid, LocalDate.now(), "")
        assertDoesNotThrow {
            mediator.håndter(godkjenning(oppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertFalse(påVentDao.erPåVent(vedtaksperiodeId))
    }

    @Test
    fun `sender ut varsel_endret ved godkjenning av varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)
        val definisjonRef = opprettVarseldefinisjon(tittel = "EN_TITTEL")
        nyttVarsel(id = varselId, vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId, kode = "EN_KODE", status = "VURDERT", definisjonRef = definisjonRef)
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
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)
        val definisjonRef = opprettVarseldefinisjon(tittel = "EN_TITTEL")
        nyttVarsel(id = varselId, vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId, kode = "EN_KODE", status = "AKTIV", definisjonRef = definisjonRef)
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
        mediator.håndter(LeggPåVent(oppgaveId, saksbehandler.oid, LocalDate.now().plusDays(21), true, ""), saksbehandler)
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
        assertTrue(melding["egenskaper"].map { it.asText() }.contains("PÅ_VENT"))
    }

    @Test
    fun `fjern på vent`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.håndter(LeggPåVent(oppgaveId, saksbehandler.oid, LocalDate.now().plusDays(21), false,""), saksbehandler)
        mediator.håndter(FjernPåVent(oppgaveId), saksbehandler)
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
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
    }

    @Test
    fun `håndterer annullering uten kommentar og begrunnelser`() {

        mediator.håndter(annullering(emptyList(), null), saksbehandler)

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
    }

    // Eksperimentering med DSL for å lage testdata
    // Slett hvis du oppdager denne koden og den ikke er tatt i bruk andre steder 😂
    // Plassert her pga. ren og skjær tilfeldighet
    private data class PERSON(val fødselsnummer: String, val aktørId: String, val arbeidsgiver: List<ARBEIDSGIVER>) {
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

    private data class ARBEIDSGIVERBUILDER(var orgnrs: List<String>) {
        fun build() = orgnrs.map(::ARBEIDSGIVER)
    }

    private data class ARBEIDSGIVER(val orgnr: String)

    private fun arbeidsgivere(antall: Int, init: ARBEIDSGIVERBUILDER.() -> Unit = {}): List<ARBEIDSGIVER> {
        val builder = ARBEIDSGIVERBUILDER(List(antall) { lagOrganisasjonsnummer() }.toList())
        builder.init()
        return builder.build()
    }

    @Test
    fun `håndterer overstyring av tidslinje`() {
        val person = person {
            arbeidsgivere = arbeidsgivere(2)
        }
        nyPerson(fødselsnummer = person.fødselsnummer, aktørId = person.aktørId, organisasjonsnummer = person { 2.ag } )

        val overstyring = OverstyrTidslinjeHandlingFraApi(
            vedtaksperiodeId = UUID.randomUUID(),
            organisasjonsnummer = person { 2.ag },
            fødselsnummer = person.fødselsnummer,
            aktørId = person.aktørId,
            begrunnelse = "En begrunnelse",
            dager = listOf(
                OverstyrTidslinjeHandlingFraApi.OverstyrDagFraApi(
                    dato = 10.januar,
                    type = "Sykedag",
                    fraType = "Arbeidsdag",
                    grad = null,
                    fraGrad = 100,
                    null,
                )
            )
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
        nyPerson(fødselsnummer = FØDSELSNUMMER, organisasjonsnummer = ORGANISASJONSNUMMER, aktørId = AKTØR_ID)
        val overstyring = OverstyrArbeidsforholdHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            overstyrteArbeidsforhold = listOf(
                OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi(
                    orgnummer = ORGANISASJONSNUMMER_GHOST,
                    deaktivert = true,
                    begrunnelse = "en begrunnelse",
                    forklaring = "en forklaring",
                    lovhjemmel = LovhjemmelFraApi("8-15", null, null, "folketrygdloven", "1998-12-18"),
                )
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
        nyPerson(fødselsnummer = FØDSELSNUMMER, organisasjonsnummer = ORGANISASJONSNUMMER, aktørId = AKTØR_ID)
        val overstyring = OverstyrInntektOgRefusjonHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            arbeidsgivere = listOf(
                OverstyrArbeidsgiverFraApi(
                    organisasjonsnummer = ORGANISASJONSNUMMER,
                    månedligInntekt = 25000.0,
                    fraMånedligInntekt = 25001.0,
                    refusjonsopplysninger = listOf(
                        OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(1.januar, 31.januar, 25000.0),
                        OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(1.februar, null, 24000.0),
                    ),
                    fraRefusjonsopplysninger = listOf(
                        OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(1.januar, 31.januar, 24000.0),
                        OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(1.februar, null, 23000.0),
                    ),
                    lovhjemmel = LovhjemmelFraApi("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                    begrunnelse = "En begrunnelse",
                    forklaring = "En forklaring"
                ),
                OverstyrArbeidsgiverFraApi(
                    organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                    månedligInntekt = 21000.0,
                    fraMånedligInntekt = 25001.0,
                    refusjonsopplysninger = listOf(
                        OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(1.januar, 31.januar, 21000.0),
                        OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(1.februar, null, 22000.0),
                    ),
                    fraRefusjonsopplysninger = listOf(
                        OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(1.januar, 31.januar, 22000.0),
                        OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(1.februar, null, 23000.0),
                    ),
                    lovhjemmel = LovhjemmelFraApi("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                    begrunnelse = "En begrunnelse 2",
                    forklaring = "En forklaring 2"
                ),
            )
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
        nyPerson(fødselsnummer = FØDSELSNUMMER, organisasjonsnummer = ORGANISASJONSNUMMER, aktørId = AKTØR_ID)
        val skjønnsfastsetting = SkjønnsfastsettSykepengegrunnlagHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            arbeidsgivere = listOf(
                SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi(
                    organisasjonsnummer = ORGANISASJONSNUMMER,
                    årlig = 25000.0,
                    fraÅrlig = 25001.0,
                    lovhjemmel = LovhjemmelFraApi("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                    årsak = "En årsak",
                    type = SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT,
                    begrunnelseMal = "En begrunnelsemal",
                    begrunnelseFritekst = "begrunnelsefritekst",
                    begrunnelseKonklusjon = "begrunnelseKonklusjon",
                    initierendeVedtaksperiodeId = PERIODE.id.toString()
                ),
                SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi(
                    organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                    årlig = 21000.0,
                    fraÅrlig = 25001.0,
                    lovhjemmel = LovhjemmelFraApi("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                    årsak = "En årsak 2",
                    type = SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT,
                    begrunnelseMal = "En begrunnelsemal",
                    begrunnelseFritekst = "begrunnelsefritekst",
                    begrunnelseKonklusjon = "begrunnelseKonklusjon",
                    initierendeVedtaksperiodeId = UUID.randomUUID().toString()
                ),
            )
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

    private fun finnOverstyringId(fødselsnummer: String): UUID? {
        @Language("PostgreSQL")
        val query = " select ekstern_hendelse_id from overstyring where person_ref = (select id from person where fodselsnummer = :fodselsnummer) "

        return sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer.toLong())).map { it.uuid("ekstern_hendelse_id") }.asSingle)
        }
    }

    private fun assertOppgave(oppgaveId: Long, forventetStatus: String) {
        @Language("PostgreSQL")
        val query = "SELECT status FROM oppgave WHERE id = ?"
        val status = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, oppgaveId).map { it.string(1) }.asSingle)
        }
        assertEquals(forventetStatus, status)
    }

    private fun godkjenning(
        oppgavereferanse: Long,
        godkjent: Boolean,
        ident: String = SAKSBEHANDLER_IDENT,
        avslag: Avslag? = null
    ) = GodkjenningDto(
        oppgavereferanse = oppgavereferanse,
        saksbehandlerIdent = ident,
        godkjent = godkjent,
        begrunnelser = emptyList(),
        kommentar = if (!godkjent) "Kommentar" else null,
        årsak = if (!godkjent) "Årsak" else null,
        avslag = avslag
    )

    private fun annullering(
        begrunnelser: List<String> = listOf("EN_BEGRUNNELSE"),
        kommentar: String? = "EN_KOMMENTAR",
    ) = AnnulleringHandlingFraApi(
        aktørId = AKTØR_ID,
        fødselsnummer = FØDSELSNUMMER,
        organisasjonsnummer = ORGANISASJONSNUMMER,
        vedtaksperiodeId = VEDTAKSPERIODE,
        utbetalingId = UTBETALING_ID,
        begrunnelser = begrunnelser,
        kommentar = kommentar
    )
}
