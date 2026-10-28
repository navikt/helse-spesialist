package no.nav.helse.spesialist.api.saksbilde

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.navikt.tbd_libs.rapids_and_rivers.asYearMonth
import no.nav.helse.mediator.asBigDecimal
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.api.DatabaseIntegrationTest.Periode.Companion.til
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.rest.saksbilde.ApiAvslagstype
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettBeregnetPeriode
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettSnapshotArbeidsgiver
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettSnapshotGenerasjon
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettSnapshotHendelse
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettUberegnetPeriode
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.IndividuellBegrunnelse
import no.nav.helse.spesialist.domain.testfixtures.apr
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.mar
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagDNummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.treeToValue
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertContains

class SaksbildeHenterTest : AbstractSaksbildeTest() {
    @Test
    fun `får personens fødselsnummer-identer når hen har flere`() {
        mockSnapshot()
        val dNummer = lagDNummer()
        opprettPerson(fødselsnummer = dNummer, aktørId = AKTØRID)
        opprettVedtaksperiode(opprettPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØRID))

        val body = hentSaksbilde()

        assertEquals(1, body["andreFodselsnummer"].size())
        assertEquals(dNummer, body["andreFodselsnummer"][0]["fodselsnummer"].asString())
    }

    @Test
    fun `Uberegnet periode tilstøtende en periode med oppgave tar med seg varsler`() {
        val personRef = opprettPerson()
        opprettArbeidsgiver()
        val uberegnetPeriode = 2 jan 2023 til (3 jan 2023)
        val periodeMedOppgave = 4 jan 2023 til (5 jan 2023)
        opprettVedtaksperiode(personRef, periode = periodeMedOppgave, skjæringstidspunkt = 2 jan 2018)
        opprettVedtak(personId = personRef, periode = uberegnetPeriode, skjæringstidspunkt = 2 jan 2018)
        val behandlingId = UUID.randomUUID()
        val behandlingRef =
            nyBehandling(
                vedtaksperiodeId = uberegnetPeriode.id,
                behandlingId = behandlingId,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2 jan 2018,
            )
        val uberegnetPeriodeSnapshot = opprettUberegnetPeriode(2 jan 2023, 3 jan 2023, uberegnetPeriode.id)
        val periodeMedOppgaveSnapshot = opprettBeregnetPeriode(4 jan 2023, 5 jan 2023, periodeMedOppgave.id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(uberegnetPeriodeSnapshot, periodeMedOppgaveSnapshot))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, behandlingRef = behandlingRef, status = "AKTIV")
        val periode = hentSaksbilde().plukkUtPeriodeMed(uberegnetPeriode.id)
        val forventetVarsel = setOf(PersonQueryTestVarsel(behandlingId, "EN_KODE"))

        assertEquals(forventetVarsel, periode.varsler)
    }

    @Test
    fun `Uberegnet periode som ikke er tilstøtende en periode med oppgave tar ikke med seg varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingRef = nyBehandling(vedtaksperiodeId = vedtaksperiodeId)
        val uberegnetPeriode = opprettUberegnetPeriode(2 jan 2023, 3 jan 2023, vedtaksperiodeId)
        val beregnetPeriode = opprettBeregnetPeriode(5 jan 2023, 6 jan 2023, PERIODE.id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(beregnetPeriode, uberegnetPeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVedtaksperiode(opprettPerson())
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, behandlingRef = behandlingRef, status = "AKTIV")

        val periode = hentSaksbilde().plukkUtPeriodeMed(vedtaksperiodeId)

        assertTrue(periode.varsler.isEmpty())
    }

    @Test
    fun `Bare siste behandling for en uberegnet periode skal ta med seg aktive varsler`() {
        val snapshotBehandlingId1 = UUID.randomUUID()
        val snapshotBehandlingId2 = UUID.randomUUID()
        val personRef = opprettPerson()
        opprettArbeidsgiver()
        val uberegnetPeriode = 2 jan 2023 til (3 jan 2023)
        val periodeMedOppgave = 4 jan 2023 til (5 jan 2023)
        opprettVedtaksperiode(personRef, periode = periodeMedOppgave, skjæringstidspunkt = 2 jan 2018)
        opprettVedtak(personId = personRef, periode = uberegnetPeriode, skjæringstidspunkt = 2 jan 2018)
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val behandlingRef1 =
            nyBehandling(
                vedtaksperiodeId = uberegnetPeriode.id,
                behandlingId = behandlingId1,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2 jan 2018,
            )
        val behandlingRef2 =
            nyBehandling(
                vedtaksperiodeId = uberegnetPeriode.id,
                behandlingId = behandlingId2,
                periode = uberegnetPeriode,
                skjæringstidspunkt = 2 jan 2018,
            )
        val uberegnetPeriodeSnapshot = opprettUberegnetPeriode(2 jan 2023, 3 jan 2023, uberegnetPeriode.id)
        val periodeMedOppgaveSnapshot = opprettBeregnetPeriode(4 jan 2023, 5 jan 2023, periodeMedOppgave.id)
        val snapshotBehandling1 =
            opprettSnapshotGenerasjon(listOf(uberegnetPeriodeSnapshot, periodeMedOppgaveSnapshot), snapshotBehandlingId1)
        val snapshotBehandling2 =
            opprettSnapshotGenerasjon(listOf(uberegnetPeriodeSnapshot, periodeMedOppgaveSnapshot), snapshotBehandlingId2)
        val arbeidsgiver =
            opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling1, snapshotBehandling2))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))
        opprettVarseldefinisjon()
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, behandlingRef = behandlingRef1, status = "AKTIV")
        nyttVarsel(vedtaksperiodeId = uberegnetPeriode.id, behandlingRef = behandlingRef2, status = "AKTIV")
        val førsteBehandlingForPeriode = hentSaksbilde().plukkUtPeriodeMed(uberegnetPeriode.id, snapshotBehandlingId2)
        val sisteBehandlingForPeriode = hentSaksbilde().plukkUtPeriodeMed(uberegnetPeriode.id, snapshotBehandlingId1)
        val forventedeVarsler =
            setOf(PersonQueryTestVarsel(behandlingId1, "EN_KODE"), PersonQueryTestVarsel(behandlingId2, "EN_KODE"))

        assertTrue(førsteBehandlingForPeriode.varsler.isEmpty())
        assertEquals(forventedeVarsler, sisteBehandlingForPeriode.varsler)
    }

    @Test
    fun `periode med hendelse`() {
        val personRef = opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(personRef)
        val (id, fom, tom) = PERIODE
        val eksternDokumentId = UUID.randomUUID()
        val hendelseSnapshot = opprettSnapshotHendelse(eksternDokumentId)
        val periodeMedOppgaveSnapshot = opprettBeregnetPeriode(fom, tom, id, hendelser = listOf(hendelseSnapshot))
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(periodeMedOppgaveSnapshot))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = hentSaksbilde()

        val hendelse =
            body["arbeidsgivere"]
                .first()["behandlinger"]
                .first()["perioder"]
                .first()["hendelser"]
                .first()
        assertNotNull(hendelse)
        assertEquals(eksternDokumentId.toString(), hendelse["eksternDokumentId"].stringValue())
    }

    @Test
    fun `periode med grunnbeløpsjustert gjennomsnitt`() {
        val personRef = opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(personRef)
        val (id, fom, tom) = PERIODE
        val grunnbeløpsjustertGjennomsnitt = BigDecimal("500000.0")
        val beregnetPeriode =
            opprettBeregnetPeriode(fom, tom, id).copy(
                grunnbeløpsjustertGjennomsnitt = grunnbeløpsjustertGjennomsnitt,
            )
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(beregnetPeriode))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = hentSaksbilde()

        val periode = body["arbeidsgivere"].first()["behandlinger"].first()["perioder"].first()
        assertEquals(grunnbeløpsjustertGjennomsnitt, periode["grunnbelopsjustertGjennomsnitt"].asBigDecimal())
    }

    @Test
    fun `periode med avslag`() {
        val avslagsbegrunnelse = "En individuell begrunnelse"
        val person =
            lagPerson()
                .also { sessionFactory.transactionalSessionScope { transaction -> transaction.personRepository.lagre(it) } }
        val organisasjonsnummer = lagOrganisasjonsnummer()
        Arbeidsgiver.Factory
            .ny(
                id = ArbeidsgiverIdentifikator.Organisasjonsnummer(organisasjonsnummer),
                navnString = lagOrganisasjonsnavn(),
            ).also {
                sessionFactory.transactionalSessionScope { transaction ->
                    transaction.arbeidsgiverRepository.lagre(
                        it,
                    )
                }
            }
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id, organisasjonsnummer = organisasjonsnummer)
                .also {
                    sessionFactory.transactionalSessionScope { transaction ->
                        transaction.vedtaksperiodeRepository.lagre(
                            it,
                        )
                    }
                }
        val behandling =
            lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
                .also {
                    sessionFactory.transactionalSessionScope { transaction ->
                        transaction.behandlingRepository.lagre(
                            it,
                        )
                    }
                }
        val saksbehandler =
            lagSaksbehandler().also {
                sessionFactory.transactionalSessionScope { transaction ->
                    transaction.saksbehandlerRepository.lagre(it)
                }
            }
        IndividuellBegrunnelse
            .ny(
                spleisBehandlingId = behandling.spleisBehandlingId!!,
                tekst = avslagsbegrunnelse,
                utfall = Utfall.AVSLAG,
                saksbehandlerOid = saksbehandler.id,
            ).also {
                sessionFactory.transactionalSessionScope { transaction ->
                    transaction.individuellBegrunnelseRepository.lagre(it)
                }
            }

        mockSnapshot(
            fødselsnummer = person.id.value,
            arbeidsgivere =
                listOf(
                    opprettSnapshotArbeidsgiver(
                        organisasjonsnummer = organisasjonsnummer,
                        behandlinger =
                            listOf(
                                opprettSnapshotGenerasjon(
                                    perioder =
                                        listOf(
                                            opprettBeregnetPeriode(
                                                fom = behandling.fom,
                                                tom = behandling.tom,
                                                vedtaksperiodeId = vedtaksperiode.id.value,
                                                utbetalingId = behandling.utbetalingId!!.value,
                                            ),
                                        ),
                                    id = behandling.spleisBehandlingId!!.value,
                                ),
                            ),
                    ),
                ),
        )

        val body = hentSaksbilde(person.id)

        val avslag =
            body["arbeidsgivere"]
                .first()["behandlinger"]
                .first()["perioder"]
                .first()["avslag"]
                .first()
        assertNotNull(avslag)
        assertEquals(ApiAvslagstype.AVSLAG, enumValueOf<ApiAvslagstype>(avslag["type"].stringValue()))
        assertEquals(avslagsbegrunnelse, avslag["begrunnelse"].stringValue())
        assertEquals(saksbehandler.ident.value, avslag["saksbehandlerIdent"].stringValue())
        assertFalse(avslag["invalidert"].booleanValue())
    }

    @Test
    fun `andre fødselsnumre inkluderes i saksbildet`() {
        val annetFødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = annetFødselsnummer, aktørId = AKTØRID)
        opprettVedtaksperiode(opprettPerson(aktørId = AKTØRID))
        val (id, fom, tom) = PERIODE
        val periodeMedOppgaveSnapshot = opprettBeregnetPeriode(fom, tom, id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(periodeMedOppgaveSnapshot))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
        mockSnapshot(arbeidsgivere = listOf(arbeidsgiver))

        val body = hentSaksbilde()

        assertContains(
            body["andreFodselsnummer"].toList().map { it["fodselsnummer"].asString() },
            annetFødselsnummer,
        )
    }

    @Test
    fun `henter refusjonsopplysninger`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettVedtaksperiode(opprettPerson())
        opprettAvviksvurdering(vilkårsgrunnlagId = vilkårsgrunnlagId)

        val body =
            hentSaksbilde()
        val refusjonsopplysning =
            body["vilkarsgrunnlagV2"]
                .first()["arbeidsgiverrefusjoner"]
                .first()
                .get("refusjonsopplysninger")
                .first()
        assertEquals("2020-01-01", refusjonsopplysning["fom"].asString())
        assertTrue(refusjonsopplysning["tom"].isNull)
        assertEquals(30000.0, refusjonsopplysning["belop"].asDouble())
    }

    @Test
    fun `beriker vilkårsgrunnlag med data fra avviksvurdering`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettAvviksvurdering(avviksprosent = 26.0, vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettVedtaksperiode(opprettPerson())

        val body =
            hentSaksbilde()

        val vilkårsgrunnlag = body["vilkarsgrunnlagV2"].first()
        assertEquals("10000", vilkårsgrunnlag["avviksvurdering"]["beregningsgrunnlag"].asString())
        assertEquals("10000", vilkårsgrunnlag["avviksvurdering"]["sammenligningsgrunnlag"].asString())
        assertEquals("26", vilkårsgrunnlag["avviksvurdering"]["avviksprosent"].asString())
        assertEquals(2, vilkårsgrunnlag["inntekter"].size())

        val sammenligningsgrunnlag1 = vilkårsgrunnlag["inntekter"][0]["sammenligningsgrunnlag"]
        assertEquals(4000.0, sammenligningsgrunnlag1["belop"].asDouble())
        assertEquals(2, sammenligningsgrunnlag1["inntektFraAOrdningen"].size())

        val inntekt1_1 = sammenligningsgrunnlag1["inntektFraAOrdningen"][0]
        assertEquals(YearMonth.from(1 jan 2018), inntekt1_1["maned"].asYearMonth())
        assertEquals(2000.0, inntekt1_1["sum"].asDouble())
        val inntekt1_2 = sammenligningsgrunnlag1["inntektFraAOrdningen"][1]
        assertEquals(YearMonth.from(1 feb 2018), inntekt1_2["maned"].asYearMonth())
        assertEquals(2000.0, inntekt1_2["sum"].asDouble())

        val sammenligningsgrunnlag2 = vilkårsgrunnlag["inntekter"][1]["sammenligningsgrunnlag"]
        assertEquals(6000.0, sammenligningsgrunnlag2["belop"].asDouble())
        assertEquals(4, sammenligningsgrunnlag2["inntektFraAOrdningen"].size())

        val inntekt2_1 = sammenligningsgrunnlag2["inntektFraAOrdningen"][0]
        assertEquals(YearMonth.from(1 jan 2018), inntekt2_1["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_1["sum"].asDouble())
        val inntekt2_2 = sammenligningsgrunnlag2["inntektFraAOrdningen"][1]
        assertEquals(YearMonth.from(1 feb 2018), inntekt2_2["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_2["sum"].asDouble())
        val inntekt2_3 = sammenligningsgrunnlag2["inntektFraAOrdningen"][2]
        assertEquals(YearMonth.from(1 mar 2018), inntekt2_3["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_3["sum"].asDouble())
        val inntekt2_4 = sammenligningsgrunnlag2["inntektFraAOrdningen"][3]
        assertEquals(YearMonth.from(1 apr 2018), inntekt2_4["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_4["sum"].asDouble())
    }

    @Test
    fun `henter sykepengegrunnlagsgrense`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettAvviksvurdering(avviksprosent = 0.0, vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettVedtaksperiode(opprettPerson())

        val body =
            hentSaksbilde()
        val sykepengegrunnlagsgrense =
            body["vilkarsgrunnlagV2"].first()["sykepengegrunnlagsgrense"]

        assertEquals(100_000, sykepengegrunnlagsgrense["grunnbelop"].asInt())
        assertEquals(600_000, sykepengegrunnlagsgrense["grense"].asInt())
        assertEquals("2020-01-01", sykepengegrunnlagsgrense["virkningstidspunkt"].asString())
    }

    private fun JsonNode.plukkUtPeriodeMed(vedtaksperiodeId: UUID): PersonQueryTestPeriode {
        val jsonNode = this["arbeidsgivere"].first()["behandlinger"].first()["perioder"]
        val perioder = objectMapper.treeToValue<List<PersonQueryTestPeriode>>(jsonNode)
        return perioder.first { periode -> vedtaksperiodeId == periode.vedtaksperiodeId }
    }

    private fun JsonNode.plukkUtPeriodeMed(
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
    ): PersonQueryTestPeriode {
        val jsonNode = this["arbeidsgivere"].first()["behandlinger"]
        val behandlinger = objectMapper.treeToValue<List<PersonQueryTestBehandling>>(jsonNode)
        val behandling = behandlinger.first { behandling -> behandlingId == behandling.id }
        return behandling.perioder.first { periode -> vedtaksperiodeId == periode.vedtaksperiodeId }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class PersonQueryTestBehandling(
        val id: UUID,
        val perioder: List<PersonQueryTestPeriode>,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class PersonQueryTestPeriode(
        val vedtaksperiodeId: UUID,
        val varsler: Set<PersonQueryTestVarsel>,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class PersonQueryTestVarsel(
        val behandlingId: UUID,
        val kode: String,
    )
}
