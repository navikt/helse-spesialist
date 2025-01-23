package no.nav.helse.modell

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.UUID

internal class PgMeldingDaoTest : DatabaseIntegrationTest() {
    private val godkjenningsbehov: Godkjenningsbehov = mockGodkjenningsbehov()

    private val saksbehandlerløsning: Saksbehandlerløsning = mockSaksbehandlerløsning()

    @Test
    fun `finn siste igangsatte overstyring om den er korrigert søknad`() {
        val fødselsnummer = FNR
        val overstyringIgangsatt = mockOverstyringIgangsatt(fødselsnummer, listOf(VEDTAKSPERIODE), "KORRIGERT_SØKNAD")

        val overstyringIgangsattForAnnenVedtaksperiode = mockOverstyringIgangsatt(fødselsnummer, listOf(VEDTAKSPERIODE), "SYKDOMSTIDSLINJE")

        meldingDao.lagre(overstyringIgangsatt)
        meldingDao.lagre(overstyringIgangsattForAnnenVedtaksperiode)
        assertNull(meldingDao.sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer, VEDTAKSPERIODE))

        meldingDao.lagre(mockOverstyringIgangsatt(fødselsnummer, listOf(VEDTAKSPERIODE), "KORRIGERT_SØKNAD"))
        assertNotNull(meldingDao.sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer, VEDTAKSPERIODE))
    }

    @Test
    fun `finn antall korrigerte søknader`() {
        meldingDao.opprettAutomatiseringKorrigertSøknad(VEDTAKSPERIODE, UUID.randomUUID())
        val actual = meldingDao.finnAntallAutomatisertKorrigertSøknad(VEDTAKSPERIODE)
        assertEquals(1, actual)
    }

    @Test
    fun `finn ut om automatisering av korrigert søknad allerede er håndtert`() {
        meldingDao.opprettAutomatiseringKorrigertSøknad(VEDTAKSPERIODE, HENDELSE_ID)
        val håndtert = meldingDao.erKorrigertSøknadAutomatiskBehandlet(HENDELSE_ID)
        assertTrue(håndtert)
    }

    @Test
    fun `lagrer og finner hendelser`() {
        meldingDao.lagre(godkjenningsbehov)
        val actual = meldingDao.finn(HENDELSE_ID)
            ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }
        assertEquals(FNR, actual.fødselsnummer())
    }

    @Test
    fun `lagrer og finner saksbehandlerløsning`() {
        meldingDao.lagre(saksbehandlerløsning)
        val actual = meldingDao.finn(HENDELSE_ID)
            ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }
        assertEquals(FNR, actual.fødselsnummer())
    }

    @Test
    fun `lagrer hendelser inkludert kobling til vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        meldingDao.lagre(godkjenningsbehov)
        assertEquals(VEDTAKSPERIODE, finnKobling())
    }

    private fun mockOverstyringIgangsatt(fødselsnummer: String, berørtePeriodeIder: List<UUID>, årsak: String): OverstyringIgangsatt {
        return mockk<OverstyringIgangsatt>(relaxed = true) {
            every { id } returns UUID.randomUUID()
            every { fødselsnummer() } returns fødselsnummer
            every { berørteVedtaksperiodeIder } returns berørtePeriodeIder
            every { toJson() } returns lagOverstyringIgangsatt(
                fødselsnummer = fødselsnummer,
                berørtePerioder = berørtePeriodeIder.map {
                    mapOf(
                        "vedtaksperiodeId" to "$it",
                        "periodeFom" to "2022-01-01",
                        "orgnummer" to "orgnr",
                    )
                },
                årsak = årsak,
            )
        }
    }

    private fun mockGodkjenningsbehov(): Godkjenningsbehov {
        return mockk<Godkjenningsbehov>(relaxed = true) {
            every { id } returns HENDELSE_ID
            every { fødselsnummer() } returns FNR
            every { vedtaksperiodeId() } returns VEDTAKSPERIODE
            every { toJson() } returns lagGodkjenningsbehov(AKTØR, FNR, VEDTAKSPERIODE)
        }
    }

    private fun mockSaksbehandlerløsning(): Saksbehandlerløsning {
        return mockk<Saksbehandlerløsning>(relaxed = true) {
            every { id } returns HENDELSE_ID
            every { fødselsnummer() } returns FNR
            every { toJson() } returns lagSaksbehandlerløsning(FNR)
        }
    }

    private fun finnKobling(hendelseId: UUID = HENDELSE_ID) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                "SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId
            ).map { UUID.fromString(it.string(1)) }.asSingle
        )
    }

    fun lagGodkjenningsbehov(
        aktørId: String,
        fødselsnummer: String,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        periodeFom: LocalDate = now(),
        periodeTom: LocalDate = now(),
        skjæringstidspunkt: LocalDate = now(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        førstegangsbehandling: Boolean = true,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
        kanAvvises: Boolean = true,
        id: UUID = UUID.randomUUID(),
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
        avviksvurderingId: UUID? = UUID.randomUUID(),
        spleisBehandlingId: UUID = UUID.randomUUID(),
        tags: List<String> = emptyList(),
        fastsatt: String = "EtterHovedregel",
        skjønnsfastsatt: Double? = null,
    ) =
        nyHendelse(
            id, "behov",
            mutableMapOf(
                "@behov" to listOf("Godkjenning"),
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "utbetalingId" to "$utbetalingId",
                "Godkjenning" to mapOf(
                    "periodeFom" to "$periodeFom",
                    "periodeTom" to "$periodeTom",
                    "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                    "periodetype" to periodetype.name,
                    "førstegangsbehandling" to førstegangsbehandling,
                    "utbetalingtype" to utbetalingtype.name,
                    "inntektskilde" to inntektskilde.name,
                    "orgnummereMedRelevanteArbeidsforhold" to orgnummereMedRelevanteArbeidsforhold,
                    "kanAvvises" to kanAvvises,
                    "vilkårsgrunnlagId" to vilkårsgrunnlagId,
                    "behandlingId" to spleisBehandlingId,
                    "tags" to tags,
                    "perioderMedSammeSkjæringstidspunkt" to listOf(
                        mapOf(
                            "fom" to "$periodeFom",
                            "tom" to "$periodeTom",
                            "vedtaksperiodeId" to "$vedtaksperiodeId",
                            "behandlingId" to "$spleisBehandlingId"
                        )
                    ),
                    "sykepengegrunnlagsfakta" to mapOf(
                        "fastsatt" to fastsatt,
                        "arbeidsgivere" to listOf(
                            mutableMapOf(
                                "arbeidsgiver" to organisasjonsnummer,
                                "omregnetÅrsinntekt" to 123456.7,
                                "inntektskilde" to "Arbeidsgiver",
                            ).apply {
                                if (skjønnsfastsatt != null) {
                                    put("skjønnsfastsatt", skjønnsfastsatt)
                                }
                            }
                        )
                    ),
                ),
            ).apply {
                if (avviksvurderingId != null) {
                    put("behandletAvSpinnvill", true)
                    put("avviksvurderingId", avviksvurderingId)
                }
            }
        )

    fun lagSaksbehandlerløsning(
        fødselsnummer: String
    ) =
        nyHendelse(
            UUID.randomUUID(), "saksbehandler_løsning",
            mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "oppgaveId" to 3333333,
                "hendelseId" to UUID.randomUUID(),
                "behandlingId" to UUID.randomUUID(),
                "godkjent" to true,
                "saksbehandlerident" to "X001122",
                "saksbehandleroid" to UUID.randomUUID(),
                "saksbehandlerepost" to "en.saksbehandler@nav.no",
                "godkjenttidspunkt" to "2024-07-27T08:05:22.051807803",
                "saksbehandleroverstyringer" to emptyList<String>(),
                "saksbehandler" to mapOf(
                    "ident" to "X001122",
                    "epostadresse" to "en.saksbehandler@nav.no"
                )
            )
        )


    fun lagOverstyringIgangsatt(
        fødselsnummer: String,
        berørtePerioder: List<Map<String, String>> = listOf(
            mapOf(
                "vedtaksperiodeId" to "${UUID.randomUUID()}",
                "skjæringstidspunkt" to "2022-01-01",
                "periodeFom" to "2022-01-01",
                "periodeTom" to "2022-01-31",
                "orgnummer" to "orgnr",
                "typeEndring" to "REVURDERING"
            )
        ),
        årsak: String = "KORRIGERT_SØKNAD",
        fom: LocalDate = now(),
        kilde: UUID = UUID.randomUUID(),
        id: UUID = UUID.randomUUID(),
    ) =
        nyHendelse(
            id, "overstyring_igangsatt", mapOf(
                "fødselsnummer" to fødselsnummer,
                "årsak" to årsak, // Denne leses rett fra hendelse-tabellen i HendelseDao, ikke via riveren
                "berørtePerioder" to berørtePerioder,
                "kilde" to "$kilde",
                "periodeForEndringFom" to "$fom",
            )
        )

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )
}
