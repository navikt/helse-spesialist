package no.nav.helse.spesialist.db.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavnOrNull
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.typer.Kjønn
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
    private val daos = DBTestFixture.module.daos
    private val sessionFactory = DBTestFixture.module.sessionFactory
    private val dbQuery = DbQuery(DBTestFixture.module.dataSource)
    private val meldingDao = daos.meldingDao
    private val FNR = lagFødselsnummer()
    private val VEDTAKSPERIODE = UUID.randomUUID()
    private val HENDELSE_ID = UUID.randomUUID()
    private val AKTØR = lagAktørId()
    private val FORNAVN = lagFornavn()
    private val MELLOMNAVN = lagMellomnavnOrNull()
    private val ETTERNAVN = lagEtternavn()
    private val KJØNN = Kjønn.entries.toTypedArray<Kjønn>().random<Kjønn>()
    private val FØDSELSDATO = LocalDate.EPOCH
    private val ENHET = "0301"
    private var personId = -1L
    private var vedtakId = -1L
    private val ORGNUMMER = lagOrganisasjonsnummer()
    private val ORGNAVN = lagOrganisasjonsnavn()
    private val BRANSJER = listOf("EN BRANSJE")
    private val FOM = LocalDate.of(2018, 1, 1)
    private val TOM = LocalDate.of(2018, 1, 31)
    private val UTBETALING_ID = UUID.randomUUID()

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

    private fun finnKobling(hendelseId: UUID = HENDELSE_ID) = sessionOf(DBTestFixture.module.dataSource).use { session ->
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
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        skjæringstidspunkt: LocalDate = LocalDate.now(),
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
            "fom" to LocalDate.now().minusDays(20),
            "tom" to LocalDate.now(),
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
        objectMapper.readTree(objectMapper.writeValueAsString(nyHendelse(id, navn) + hendelse))

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )

    private fun opprettPerson(
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
    ): Persondata {
        val personinfoId =
            insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, adressebeskyttelse)
        val infotrygdutbetalingerId =
            daos.personDao.upsertInfotrygdutbetalinger(fødselsnummer, objectMapper.createObjectNode())
        val enhetId = ENHET.toInt()
        personId = daos.personDao.insertPerson(fødselsnummer, aktørId, personinfoId, enhetId, infotrygdutbetalingerId)
        daos.egenAnsattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        return Persondata(
            personId = personId,
            personinfoId = personinfoId,
            enhetId = enhetId,
            infotrygdutbetalingerId = infotrygdutbetalingerId,
        )
    }

    private data class Persondata(
        val personId: Long,
        val personinfoId: Long,
        val enhetId: Int,
        val infotrygdutbetalingerId: Long,
    )

    private fun insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
    ) = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO person_info (fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
        VALUES (:fornavn, :mellomnavn, :etternavn, :foedselsdato, CAST(:kjoenn as person_kjonn), :adressebeskyttelse);
        """.trimIndent(),
        "fornavn" to fornavn,
        "mellomnavn" to mellomnavn,
        "etternavn" to etternavn,
        "foedselsdato" to fødselsdato,
        "kjoenn" to kjønn.name,
        "adressebeskyttelse" to adressebeskyttelse.name,
    ).let(::requireNotNull)

    private fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGNUMMER,
        navn: String = ORGNAVN,
        bransjer: List<String> = BRANSJER,
    ) {
        sessionFactory.transactionalSessionScope {
            it.inntektskilderRepository.lagreInntektskilder(
                listOf(
                    KomplettInntektskildeDto(
                        identifikator = organisasjonsnummer,
                        type = InntektskildetypeDto.ORDINÆR,
                        navn = navn,
                        bransjer = bransjer,
                        sistOppdatert = LocalDate.now(),
                    ),
                ),
            )
        }
    }

    private fun opprettVedtaksperiode(
        fødselsnummer: String = FNR,
        organisasjonsnummer: String = ORGNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fom: LocalDate = FOM,
        tom: LocalDate = TOM,
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        utbetalingId: UUID? = UTBETALING_ID,
        forkastet: Boolean = false,
        spleisBehandlingId: UUID = UUID.randomUUID(),
    ) {
        sessionFactory.transactionalSessionScope {
            it.personRepository.brukPersonHvisFinnes(fødselsnummer) {
                this.nySpleisBehandling(
                    SpleisBehandling(
                        organisasjonsnummer,
                        vedtaksperiodeId,
                        spleisBehandlingId,
                        fom,
                        tom
                    )
                )
                if (utbetalingId != null) this.nyUtbetalingForVedtaksperiode(vedtaksperiodeId, utbetalingId)
                if (forkastet) this.vedtaksperiodeForkastet(vedtaksperiodeId)
            }
        }
        daos.vedtakDao.finnVedtakId(vedtaksperiodeId)?.also {
            vedtakId = it
        }
        opprettVedtakstype(vedtaksperiodeId, periodetype, inntektskilde)
    }

    private fun opprettVedtakstype(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        type: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
    ) {
        daos.vedtakDao.leggTilVedtaksperiodetype(vedtaksperiodeId, type, inntektskilde)
    }
}
