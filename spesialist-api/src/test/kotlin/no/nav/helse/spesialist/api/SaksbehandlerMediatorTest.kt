package no.nav.helse.spesialist.api

import java.util.UUID
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.TestRapidHelpers.hendelser
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsgiverDto
import no.nav.helse.spesialist.api.overstyring.OverstyrInntektOgRefusjonDto
import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.SubsumsjonDto
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.utbetaling.AnnulleringDto
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
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

    private val saksbehandler = Saksbehandler(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT)

    @BeforeEach
    internal fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `håndter godkjenning`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), periode = Periode(vedtaksperiodeId, 1.januar, 31.januar))
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "VURDERT", definisjonRef = definisjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "VURDERT", definisjonRef = definisjonRef)
        assertDoesNotThrow {
            mediator.håndter(godkjenning(sisteOppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(generasjonRef, 2)
    }

    @Test
    fun `håndter godkjenning når periode har aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), periode = Periode(vedtaksperiodeId, 1.januar, 31.januar))
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "AKTIV", definisjonRef = definisjonRef)
        assertThrows<ManglerVurderingAvVarsler> {
            mediator.håndter(godkjenning(sisteOppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(generasjonRef, 0)
    }

    @Test
    fun `håndter godkjenning når periode ikke har noen varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), periode = Periode(vedtaksperiodeId, 1.januar, 31.januar))
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        assertDoesNotThrow {
            mediator.håndter(godkjenning(sisteOppgaveId, true), UUID.randomUUID(), saksbehandler)
        }
        assertGodkjenteVarsler(generasjonRef, 0)
    }



    @Test
    fun `håndter godkjenning når godkjenning er avvist`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), periode = Periode(vedtaksperiodeId, 1.januar, 31.januar))
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "AKTIV", definisjonRef = definisjonRef)
        mediator.håndter(godkjenning(sisteOppgaveId, false), UUID.randomUUID(), saksbehandler)
        assertGodkjenteVarsler(generasjonRef, 0)
        assertAvvisteVarsler(generasjonRef, 1)
    }

    @Test
    fun `håndter totrinnsvurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), periode = Periode(vedtaksperiodeId, 1.januar, 31.januar))
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "VURDERT", definisjonRef = definisjonRef)
        assertDoesNotThrow {
            mediator.håndterTotrinnsvurdering(1L)
        }
    }

    @Test
    fun `håndter totrinnsvurdering når periode har aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), periode = Periode(vedtaksperiodeId, 1.januar, 31.januar))
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "AKTIV", definisjonRef = definisjonRef)
        assertThrows<ManglerVurderingAvVarsler> {
            mediator.håndterTotrinnsvurdering(1L)
        }
    }

    @Test
    fun `håndter totrinnsvurdering når periode ikke har noen varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), periode = Periode(vedtaksperiodeId, 1.januar, 31.januar))
        assertDoesNotThrow {
            mediator.håndterTotrinnsvurdering(1L)
        }
    }

    @Test
    fun `sender ut varsel_endret ved godkjenning av varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), periode = Periode(vedtaksperiodeId, 1.januar, 31.januar))
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        val definisjonRef = opprettVarseldefinisjon(tittel = "EN_TITTEL")
        nyttVarsel(id = varselId, vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, kode = "EN_KODE", status = "VURDERT", definisjonRef = definisjonRef)
        mediator.håndter(godkjenning(sisteOppgaveId, true), behandlingId, saksbehandler)

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
        val overstyring = OverstyrTidslinjeDto(
            organisasjonsnummer = ORGANISASJONSNUMMER,
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            begrunnelse = "En begrunnelse",
            dager = listOf(
                OverstyrTidslinjeDto.OverstyrDagDto(
                    dato = 10.januar,
                    type = "Sykedag",
                    fraType = "Arbeidsdag",
                    grad = null,
                    fraGrad = 100
                )
            )
        )

        mediator.håndter(overstyring, saksbehandler)
        val hendelse = testRapid.inspektør.hendelser("saksbehandler_overstyrer_tidslinje").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(ORGANISASJONSNUMMER, hendelse["organisasjonsnummer"].asText())
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())

        assertEquals("En begrunnelse", hendelse["begrunnelse"].asText())

        val overstyrtDag = hendelse["dager"].toList().single()
        assertEquals(10.januar, overstyrtDag["dato"].asLocalDate())
        assertEquals("Sykedag", overstyrtDag["type"].asText())
        assertEquals("Arbeidsdag", overstyrtDag["fraType"].asText())
        assertEquals(null, overstyrtDag["grad"]?.textValue())
        assertEquals(100, overstyrtDag["fraGrad"].asInt())
    }

    @Test
    fun `håndterer overstyring av arbeidsforhold`() {
        val overstyring = OverstyrArbeidsforholdDto(
            fødselsnummer = FØDSELSNUMMER,
            aktørId = AKTØR_ID,
            skjæringstidspunkt = 1.januar,
            overstyrteArbeidsforhold = listOf(
                OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
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
        val overstyring = OverstyrInntektOgRefusjonDto(
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
    ) = AnnulleringDto(
        aktørId = AKTØR_ID,
        fødselsnummer = FØDSELSNUMMER,
        organisasjonsnummer = ORGANISASJONSNUMMER,
        fagsystemId = "EN_FAGSYSTEMID",
        begrunnelser = begrunnelser,
        kommentar = kommentar
    )
}