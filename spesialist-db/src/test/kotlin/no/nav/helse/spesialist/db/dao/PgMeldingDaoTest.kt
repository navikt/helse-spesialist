package no.nav.helse.spesialist.db.dao

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PgMeldingDaoTest {
    private val meldingDao = DBTestFixture.module.daos.meldingDao

    @Test
    fun `finn siste behandling opprettet om det er korrigert søknad`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val behandlingOpprettetRevurdering = lagBehandlingOpprettet(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId1,
            type = "Revurdering"
        )
        val behandlingOpprettetSøknad = lagBehandlingOpprettet(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId2,
            type = "Søknad"
        )

        // When:
        meldingDao.lagre(behandlingOpprettetRevurdering)
        meldingDao.lagre(behandlingOpprettetSøknad)

        // Then:
        assertNull(meldingDao.sisteBehandlingOpprettetOmKorrigertSøknad(fødselsnummer, vedtaksperiodeId2))
        assertNotNull(meldingDao.sisteBehandlingOpprettetOmKorrigertSøknad(fødselsnummer, vedtaksperiodeId1))
    }

    @Test
    fun `finn antall korrigerte søknader`() {
        // Given:
        val vedtaksperiodeId1 = UUID.randomUUID()

        // When:
        meldingDao.opprettAutomatiseringKorrigertSøknad(
            vedtaksperiodeId = vedtaksperiodeId1,
            meldingId = UUID.randomUUID()
        )
        meldingDao.opprettAutomatiseringKorrigertSøknad(
            vedtaksperiodeId = UUID.randomUUID(),
            meldingId = UUID.randomUUID()
        )

        // Then:
        assertEquals(1, meldingDao.finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId1))
    }

    @Test
    fun `finn ut om automatisering av korrigert søknad allerede er håndtert`() {
        // Given:
        val vedtaksperiodeId = UUID.randomUUID()
        val meldingId = UUID.randomUUID()

        // When:
        meldingDao.opprettAutomatiseringKorrigertSøknad(vedtaksperiodeId, meldingId)

        // Then:
        assertTrue(meldingDao.erKorrigertSøknadAutomatiskBehandlet(meldingId))
    }

    @Test
    fun `lagrer og finner hendelser`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val hendelseId = UUID.randomUUID()
        val godkjenningsbehov = lagGodkjenningsbehov(hendelseId = hendelseId, fødselsnummer = fødselsnummer)

        // When:
        meldingDao.lagre(godkjenningsbehov)

        // Then:
        val actual = meldingDao.finn(hendelseId) ?: fail { "Forventet å finne en hendelse med id $hendelseId" }
        assertEquals(fødselsnummer, actual.fødselsnummer())
    }

    @Test
    fun `lagrer og finner saksbehandlerløsning`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val meldingId = UUID.randomUUID()
        val saksbehandlerløsning = lagSaksbehandlerløsning(meldingId = meldingId, fødselsnummer = fødselsnummer)

        // When:
        meldingDao.lagre(saksbehandlerløsning)

        // Then:
        val actual = meldingDao.finn(meldingId) ?: fail { "Forventet å finne en hendelse med id $meldingId" }
        assertEquals(fødselsnummer, actual.fødselsnummer())
    }

    @Test
    fun `lagrer hendelser inkludert kobling til vedtak`() {
        // Given:
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val godkjenningsbehov = lagGodkjenningsbehov(hendelseId = hendelseId, vedtaksperiodeId = vedtaksperiodeId)

        // When:
        meldingDao.lagre(godkjenningsbehov)

        // Then:
        val actualVedtaksperiodeId = sessionOf(DBTestFixture.module.dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT vedtaksperiode_id FROM vedtaksperiode_hendelse WHERE hendelse_ref = ?", hendelseId
                ).map { UUID.fromString(it.string(1)) }.asSingle
            )
        }
        assertEquals(vedtaksperiodeId, actualVedtaksperiodeId)
    }

    private fun lagBehandlingOpprettet(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        type: String
    ) = BehandlingOpprettet(
        jsonNode = mapOf(
            "@event_name" to "behandling_opprettet",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "organisasjonsnummer" to lagOrganisasjonsnummer(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "behandlingId" to UUID.randomUUID(),
            "fødselsnummer" to fødselsnummer,
            "fom" to LocalDate.now().minusDays(20),
            "tom" to LocalDate.now(),
            "yrkesaktivitetstype" to Yrkesaktivitetstype.ARBEIDSTAKER,
            "type" to type,
            "kilde" to mapOf(
                "avsender" to "SYKMELDT"
            ),
            "@forårsaket_av" to mapOf(
                "event_name" to "sendt_søknad_nav"
            )
        ).toJsonNode()
    )

    private fun lagSaksbehandlerløsning(
        meldingId: UUID,
        fødselsnummer: String
    ) = Saksbehandlerløsning(
        jsonNode = mapOf(
            "@event_name" to "saksbehandler_løsning",
            "@id" to meldingId,
            "@opprettet" to LocalDateTime.now(),
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
        ).toJsonNode()
    )

    private fun lagGodkjenningsbehov(
        hendelseId: UUID,
        fødselsnummer: String = lagFødselsnummer(),
        vedtaksperiodeId: UUID = UUID.randomUUID()
    ): Godkjenningsbehov {
        val fom = LocalDate.now()
        val tom = LocalDate.now()
        val spleisBehandlingId = UUID.randomUUID()
        return Godkjenningsbehov.fraJson(
            json = mapOf(
                "@event_name" to "behov",
                "@id" to hendelseId,
                "@opprettet" to LocalDateTime.now(),
                "@behov" to listOf("Godkjenning"),
                "aktørId" to lagAktørId(),
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to "orgnr",
                "yrkesaktivitetstype" to Yrkesaktivitetstype.ARBEIDSTAKER,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "utbetalingId" to "${UUID.randomUUID()}",
                "Godkjenning" to mapOf(
                    "periodeFom" to "$fom",
                    "periodeTom" to "$tom",
                    "skjæringstidspunkt" to LocalDate.now().toString(),
                    "periodetype" to Periodetype.FØRSTEGANGSBEHANDLING.name,
                    "førstegangsbehandling" to true,
                    "utbetalingtype" to Utbetalingtype.UTBETALING.name,
                    "inntektskilde" to Inntektskilde.EN_ARBEIDSGIVER.name,
                    "orgnummereMedRelevanteArbeidsforhold" to emptyList<String>(),
                    "kanAvvises" to true,
                    "vilkårsgrunnlagId" to UUID.randomUUID(),
                    "behandlingId" to spleisBehandlingId,
                    "tags" to emptyList<String>(),
                    "perioderMedSammeSkjæringstidspunkt" to listOf(
                        mapOf(
                            "fom" to "$fom",
                            "tom" to "$tom",
                            "vedtaksperiodeId" to "$vedtaksperiodeId",
                            "behandlingId" to "$spleisBehandlingId"
                        )
                    ),
                    "sykepengegrunnlagsfakta" to mapOf(
                        "fastsatt" to "EtterHovedregel",
                        "omregnetÅrsinntektTotalt" to 123456.7,
                        "6G" to 666666.0,
                        "sykepengegrunnlag" to 123456.7,
                        "arbeidsgivere" to listOf(
                            mapOf(
                                "arbeidsgiver" to "orgnr",
                                "omregnetÅrsinntekt" to 123456.7,
                                "inntektskilde" to "Arbeidsgiver",
                            )
                        )
                    ),
                    "omregnedeÅrsinntekter" to listOf(
                        mapOf(
                            "organisasjonsnummer" to "orgnr",
                            "beløp" to 123456.7,
                        )
                    ),
                ),
            ).toJson()
        )
    }

    private fun Map<String, Any>.toJson(): String =
        objectMapper.writeValueAsString(this)

    private fun Map<String, Any>.toJsonNode(): JsonNode =
        objectMapper.readTree(this.toJson())
}
