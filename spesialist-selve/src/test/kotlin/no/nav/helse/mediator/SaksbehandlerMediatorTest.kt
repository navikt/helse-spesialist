package no.nav.helse.mediator

import DatabaseIntegrationTest
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AnnulleringHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverDto
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverDto.SkjønnsfastsettingstypeDto
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SubsumsjonDto
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class SaksbehandlerMediatorTest: DatabaseIntegrationTest() {
    private val testRapid = TestRapid()
    private val mediator = SaksbehandlerMediator(dataSource, testRapid)

    private val AKTØR_ID = "1234567891011"
    private val FØDSELSNUMMER = "12345678910"
    private val ORGANISASJONSNUMMER = "987654321"
    private val ORGANISASJONSNUMMER_GHOST = "123456789"

    private val SAKSBEHANDLER_OID = UUID.randomUUID()
    private val SAKSBEHANDLER_NAVN = "ET_NAVN"
    private val SAKSBEHANDLER_IDENT = "EN_IDENT"

    private val SAKSBEHANDLER_EPOST = "epost@nav.no"

    private val saksbehandler = SaksbehandlerFraApi(
        oid = SAKSBEHANDLER_OID,
        navn = SAKSBEHANDLER_NAVN,
        epost = SAKSBEHANDLER_EPOST,
        ident = SAKSBEHANDLER_IDENT
    )

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
    fun `håndterer annullering`() {
        mediator.håndter(annullering(), saksbehandler)

        assertEquals(1, testRapid.inspektør.size)
        val melding = testRapid.inspektør.message(0)
        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(SAKSBEHANDLER_OID.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(SAKSBEHANDLER_NAVN, melding["saksbehandler"]["navn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, melding["saksbehandler"]["ident"].asText())

        assertEquals("EN_FAGSYSTEMID", melding["fagsystemId"].asText())
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

        assertEquals("EN_FAGSYSTEMID", melding["fagsystemId"].asText())
        assertEquals(null, melding["kommentar"]?.asText())
        assertEquals(0, melding["begrunnelser"].map { it.asText() }.size)
    }

    @Test
    fun `håndterer overstyring av tidslinje`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, organisasjonsnummer = ORGANISASJONSNUMMER, aktørId = AKTØR_ID)
        val overstyring = OverstyrTidslinjeHandlingFraApi(
            organisasjonsnummer = ORGANISASJONSNUMMER,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            begrunnelse = "En begrunnelse",
            dager = listOf(
                OverstyrTidslinjeHandlingFraApi.OverstyrDagDto(
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
        val overstyringId = finnOverstyringId(FØDSELSNUMMER)

        assertNotNull(overstyringId)
        assertEquals(overstyringId.toString(), hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(ORGANISASJONSNUMMER, hendelse["organisasjonsnummer"].asText())

        val overstyrtDag = hendelse["dager"].toList().single()
        assertEquals(10.januar, overstyrtDag["dato"].asLocalDate())
        assertEquals("Sykedag", overstyrtDag["type"].asText())
        assertEquals("Arbeidsdag", overstyrtDag["fraType"].asText())
        assertEquals(null, overstyrtDag["grad"]?.textValue())
        assertEquals(100, overstyrtDag["fraGrad"].asInt())
    }

    private fun finnOverstyringId(fødselsnummer: String): UUID? {
        @Language("PostgreSQL")
        val query = " select ekstern_hendelse_id from overstyring where person_ref = (select id from person where fodselsnummer = :fodselsnummer) "

        return sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer.toLong())).map { it.uuid("ekstern_hendelse_id") }.asSingle)
        }
    }

    @Test
    fun `håndterer overstyring av arbeidsforhold`() {
        val overstyring = OverstyrArbeidsforholdHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            overstyrteArbeidsforhold = listOf(
                OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdDto(
                    orgnummer = ORGANISASJONSNUMMER_GHOST,
                    deaktivert = true,
                    begrunnelse = "en begrunnelse",
                    forklaring = "en forklaring"
                )
            ),
        )

        mediator.håndter(overstyring, saksbehandler)
        val hendelse = testRapid.inspektør.hendelser("saksbehandler_overstyrer_arbeidsforhold").first()

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
        val overstyring = OverstyrInntektOgRefusjonHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            arbeidsgivere = listOf(
                OverstyrArbeidsgiverDto(
                    organisasjonsnummer = ORGANISASJONSNUMMER,
                    månedligInntekt = 25000.0,
                    fraMånedligInntekt = 25001.0,
                    refusjonsopplysninger = listOf(
                        OverstyrArbeidsgiverDto.RefusjonselementDto(1.januar, 31.januar, 25000.0),
                        OverstyrArbeidsgiverDto.RefusjonselementDto(1.februar, null, 24000.0),
                    ),
                    fraRefusjonsopplysninger = listOf(
                        OverstyrArbeidsgiverDto.RefusjonselementDto(1.januar, 31.januar, 24000.0),
                        OverstyrArbeidsgiverDto.RefusjonselementDto(1.februar, null, 23000.0),
                    ),
                    subsumsjon = SubsumsjonDto("8-28", "3", null),
                    begrunnelse = "En begrunnelse",
                    forklaring = "En forklaring"
                ),
                OverstyrArbeidsgiverDto(
                    organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                    månedligInntekt = 21000.0,
                    fraMånedligInntekt = 25001.0,
                    refusjonsopplysninger = listOf(
                        OverstyrArbeidsgiverDto.RefusjonselementDto(1.januar, 31.januar, 21000.0),
                        OverstyrArbeidsgiverDto.RefusjonselementDto(1.februar, null, 22000.0),
                    ),
                    fraRefusjonsopplysninger = listOf(
                        OverstyrArbeidsgiverDto.RefusjonselementDto(1.januar, 31.januar, 22000.0),
                        OverstyrArbeidsgiverDto.RefusjonselementDto(1.februar, null, 23000.0),
                    ),
                    subsumsjon = SubsumsjonDto("8-28", "3", null),
                    begrunnelse = "En begrunnelse 2",
                    forklaring = "En forklaring 2"
                ),
            )
        )

        mediator.håndter(overstyring, saksbehandler)

        val hendelse = testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt_og_refusjon").first()

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
            assertEquals("8-28", it["subsumsjon"]["paragraf"].asText())
            assertEquals("3", it["subsumsjon"]["ledd"].asText())
            Assertions.assertTrue(it["subsumsjon"]["bokstav"].isNull)
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
            assertEquals("8-28", it["subsumsjon"]["paragraf"].asText())
            assertEquals("3", it["subsumsjon"]["ledd"].asText())
            Assertions.assertTrue(it["subsumsjon"]["bokstav"].isNull)
            assertEquals(2, it["refusjonsopplysninger"].size())
            assertEquals("2018-01-01", it["refusjonsopplysninger"].first()["fom"].asText())
            assertEquals("2018-01-31", it["refusjonsopplysninger"].first()["tom"].asText())
            assertEquals(21000.0, it["refusjonsopplysninger"].first()["beløp"].asDouble())
            assertEquals(22000.0, it["fraRefusjonsopplysninger"].first()["beløp"].asDouble())
        }
    }

    @Test
    fun `håndterer skjønnsfastsetting av sykepengegrunnlag`() {
        val skjønnsfastsetting = SkjønnsfastsettSykepengegrunnlagHandlingFraApi(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            arbeidsgivere = listOf(
                SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverDto(
                    organisasjonsnummer = ORGANISASJONSNUMMER,
                    årlig = 25000.0,
                    fraÅrlig = 25001.0,
                    subsumsjon = SubsumsjonDto("8-28", "3", null),
                    årsak = "En årsak",
                    type = SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT,
                    begrunnelseMal = "En begrunnelsemal",
                    begrunnelseFritekst = "begrunnelsefritekst",
                    begrunnelseKonklusjon = "begrunnelseKonklusjon",
                    initierendeVedtaksperiodeId = PERIODE.id.toString()
                ),
                SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverDto(
                    organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                    årlig = 21000.0,
                    fraÅrlig = 25001.0,
                    subsumsjon = SubsumsjonDto("8-28", "3", null),
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

        val hendelse = testRapid.inspektør.hendelser("saksbehandler_skjonnsfastsetter_sykepengegrunnlag").first()

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
            assertEquals("8-28", it["subsumsjon"]["paragraf"].asText())
            assertEquals("3", it["subsumsjon"]["ledd"].asText())
            Assertions.assertTrue(it["subsumsjon"]["bokstav"].isNull)
        }
        hendelse["arbeidsgivere"].last().let {
            assertEquals(ORGANISASJONSNUMMER_GHOST, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelsemal", it["begrunnelseMal"].asText())
            assertEquals("begrunnelsefritekst", it["begrunnelseFritekst"].asText())
            assertEquals("begrunnelseKonklusjon", it["begrunnelseKonklusjon"].asText())
            assertEquals("En årsak 2", it["årsak"].asText())
            assertEquals(21000.0, it["årlig"].asDouble())
            assertEquals(25001.0, it["fraÅrlig"].asDouble())
            assertEquals("8-28", it["subsumsjon"]["paragraf"].asText())
            assertEquals("3", it["subsumsjon"]["ledd"].asText())
            Assertions.assertTrue(it["subsumsjon"]["bokstav"].isNull)
        }
    }

    private fun godkjenning(
        oppgavereferanse: Long,
        godkjent: Boolean,
        ident: String = SAKSBEHANDLER_IDENT,
    ) = GodkjenningDto(
        oppgavereferanse = oppgavereferanse,
        saksbehandlerIdent = ident,
        godkjent = godkjent,
        begrunnelser = emptyList(),
        kommentar = if (!godkjent) "Kommentar" else null,
        årsak = if (!godkjent) "Årsak" else null
    )

    private fun annullering(
        begrunnelser: List<String> = listOf("EN_BEGRUNNELSE"),
        kommentar: String? = "EN_KOMMENTAR",
    ) = AnnulleringHandlingFraApi(
        aktørId = AKTØR_ID,
        fødselsnummer = FØDSELSNUMMER,
        organisasjonsnummer = ORGANISASJONSNUMMER,
        fagsystemId = "EN_FAGSYSTEMID",
        begrunnelser = begrunnelser,
        kommentar = kommentar
    )
}