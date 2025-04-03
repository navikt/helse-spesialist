package no.nav.helse.modell

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.e2e.DatabaseIntegrationTest
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
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

class PgMeldingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `finn siste behandling opprettet om det er korrigert søknad`() {
        val annenVedtaksperiode = UUID.randomUUID()

        val behandlingOpprettetKorrigertSøknad = lagBehandlingOpprettet(FNR, VEDTAKSPERIODE, type = "Revurdering")
        val behandlingOpprettetSøknadAnnenPeriode = lagBehandlingOpprettet(FNR, annenVedtaksperiode, type = "Søknad")

        meldingDao.lagre(behandlingOpprettetKorrigertSøknad)
        meldingDao.lagre(behandlingOpprettetSøknadAnnenPeriode)

        assertNull(meldingDao.sisteBehandlingOpprettetOmKorrigertSøknad(FNR, annenVedtaksperiode))
        assertNotNull(meldingDao.sisteBehandlingOpprettetOmKorrigertSøknad(FNR, VEDTAKSPERIODE))
    }

    @Test
    fun `finn antall korrigerte søknader`() {
        meldingDao.opprettAutomatiseringKorrigertSøknad(VEDTAKSPERIODE, UUID.randomUUID())
        meldingDao.opprettAutomatiseringKorrigertSøknad(UUID.randomUUID(), UUID.randomUUID())
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
        meldingDao.lagre(lagGodkjenningsbehov(HENDELSE_ID, AKTØR, FNR, VEDTAKSPERIODE))
        val actual = meldingDao.finn(HENDELSE_ID) ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }
        assertEquals(FNR, actual.fødselsnummer())
    }

    @Test
    fun `lagrer og finner saksbehandlerløsning`() {
        meldingDao.lagre(lagSaksbehandlerløsning(FNR, HENDELSE_ID))
        val actual = meldingDao.finn(HENDELSE_ID) ?: fail { "Forventet å finne en hendelse med id $HENDELSE_ID" }
        assertEquals(FNR, actual.fødselsnummer())
    }

    @Test
    fun `lagrer hendelser inkludert kobling til vedtak`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        meldingDao.lagre(lagGodkjenningsbehov(HENDELSE_ID, AKTØR, FNR, VEDTAKSPERIODE))
        assertEquals(VEDTAKSPERIODE, finnKobling())
    }

    private fun finnKobling(hendelseId: UUID = HENDELSE_ID) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                "SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId
            ).map { UUID.fromString(it.string(1)) }.asSingle
        )
    }

    private fun lagGodkjenningsbehov(
        hendelseId: UUID,
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
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID = UUID.randomUUID(),
        tags: List<String> = emptyList(),
        fastsatt: String = "EtterHovedregel",
        skjønnsfastsatt: Double? = null,
    ) =
        nyHendelse(
            hendelseId, "behov",
            mapOf(
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
                    "omregnedeÅrsinntekter" to listOf(
                        mapOf(
                            "organisasjonsnummer" to organisasjonsnummer,
                            "beløp" to 123456.7,
                        )
                    ),
                ),
            )
        ).let(::Godkjenningsbehov)

    private fun lagSaksbehandlerløsning(
        fødselsnummer: String,
        hendelseId: UUID,
    ) =
        nyHendelse(
            hendelseId, "saksbehandler_løsning",
            mapOf(
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
        ).let(::Saksbehandlerløsning)

    private fun lagBehandlingOpprettet(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        id: UUID = UUID.randomUUID(),
        avsender: String = "SYKMELDT",
        forårsaketAv: String = "sendt_søknad_nav",
        type: String = "Søknad",
    ) = nyHendelse(
        id, "behandling_opprettet", mapOf(
            "organisasjonsnummer" to lagOrganisasjonsnummer(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "behandlingId" to UUID.randomUUID(),
            "fødselsnummer" to fødselsnummer,
            "fom" to now().minusDays(20),
            "tom" to now(),
            "type" to type, // Denne leses rett fra hendelse-tabellen i MeldingDao, ikke via riveren
            "kilde" to mapOf(
                "avsender" to avsender // Denne leses rett fra hendelse-tabellen i MeldingDao, ikke via riveren
            ),
            "@forårsaket_av" to mapOf(
                "event_name" to forårsaketAv // Denne leses rett fra hendelse-tabellen i MeldingDao, ikke via riveren
            )
        )
    ).let(::BehandlingOpprettet)

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson().let(objectMapper::readTree)

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )
}
