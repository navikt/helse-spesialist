package no.nav.helse.spesialist.api

import io.mockk.mockk
import no.nav.helse.e2e.AbstractDatabaseTest
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.DataSourceDbQuery
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.apr
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.mar
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
    protected val dbQuery = DataSourceDbQuery(dataSource)
    private val NAVN = Navn(lagFornavn(), lagFornavn(), lagEtternavn())
    private val ENHET = Enhet(101, "Halden")
    protected val PERIODE = Periode(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

    protected val SAKSBEHANDLER = lagSaksbehandler()

    val FØDSELSNUMMER = lagFødselsnummer()
    val AKTØRID = lagAktørId()
    val ARBEIDSGIVER_NAVN = lagOrganisasjonsnavn()
    val ORGANISASJONSNUMMER = lagOrganisasjonsnummer()

    protected val apiVarselRepository = daos.varselApiRepository
    protected val arbeidsgiverApiDao = daos.arbeidsgiverApiDao
    protected val risikovurderingApiDao = daos.risikovurderingApiDao
    protected val notatDao = daos.notatApiDao
    protected val påVentApiDao = daos.påVentApiDao
    protected val personApiDao = daos.personApiDao
    protected val tildelingApiDao = daos.tildelingApiDao
    protected val overstyringApiDao = daos.overstyringApiDao
    protected val oppgaveApiDao = daos.oppgaveApiDao
    protected val periodehistorikkApiDao = daos.periodehistorikkApiDao
    protected val vergemålApiDao = daos.vergemålApiDao

    protected val apiOppgaveService = mockk<ApiOppgaveService>(relaxed = true)

    protected fun opprettVedtaksperiode(
        personId: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode = PERIODE,
        skjæringstidspunkt: LocalDate = periode.fom,
        forkastet: Boolean = false,
        kanAvvises: Boolean = true,
    ): Long {
        val behandlingId = UUID.randomUUID()
        return opprettVedtak(
            behandlingId = behandlingId,
            personId = personId,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            forkastet = forkastet,
        ).also {
            klargjørVedtak(
                vedtakId = it,
                utbetalingId = utbetalingId,
                periode = periode,
                kanAvvises = kanAvvises,
                behandlingId = behandlingId,
            )
        }
    }

    private fun opprettBehandling(
        behandlingId: UUID?,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
    ) {
        requireNotNull(
            dbQuery.update(
                """
                INSERT INTO behandling (unik_id, vedtaksperiode_id, opprettet_av_hendelse, tilstand, fom, tom, skjæringstidspunkt)
                VALUES (:unik_id, :vedtaksperiode_id, :hendelse_id, 'VidereBehandlingAvklares',:fom, :tom, :skjaeringstidspunkt)
                """.trimIndent(),
                "unik_id" to behandlingId,
                "vedtaksperiode_id" to periode.id,
                "hendelse_id" to UUID.randomUUID(),
                "fom" to periode.fom,
                "tom" to periode.tom,
                "skjaeringstidspunkt" to skjæringstidspunkt,
            ),
        )
    }

    protected fun opprettAvviksvurdering(
        fødselsnummer: String = FØDSELSNUMMER,
        skjæringstidspunkt: LocalDate = 1 jan 2018,
        avviksvurderingId: UUID = UUID.randomUUID(),
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
        avviksprosent: Double = 25.0,
    ) {
        sessionFactory.transactionalSessionScope {
            it.avviksvurderingRepository.lagre(
                Avviksvurdering(
                    unikId = avviksvurderingId,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    opprettet = (1 jan 2018).atStartOfDay(),
                    avviksprosent = avviksprosent,
                    sammenligningsgrunnlag =
                        Sammenligningsgrunnlag(
                            totalbeløp = 10000.0,
                            innrapporterteInntekter =
                                listOf(
                                    InnrapportertInntekt(
                                        arbeidsgiverreferanse = ORGANISASJONSNUMMER,
                                        inntekter =
                                            listOf(
                                                Inntekt(
                                                    årMåned = YearMonth.from(1 jan 2018),
                                                    beløp = 2000.0,
                                                ),
                                                Inntekt(
                                                    årMåned = YearMonth.from(1 feb 2018),
                                                    beløp = 2000.0,
                                                ),
                                            ),
                                    ),
                                    InnrapportertInntekt(
                                        arbeidsgiverreferanse = "987656789",
                                        inntekter =
                                            listOf(
                                                Inntekt(
                                                    årMåned = YearMonth.from(1 jan 2018),
                                                    beløp = 1500.0,
                                                ),
                                                Inntekt(
                                                    årMåned = YearMonth.from(1 feb 2018),
                                                    beløp = 1500.0,
                                                ),
                                                Inntekt(
                                                    årMåned = YearMonth.from(1 mar 2018),
                                                    beløp = 1500.0,
                                                ),
                                                Inntekt(
                                                    årMåned = YearMonth.from(1 apr 2018),
                                                    beløp = 1500.0,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    beregningsgrunnlag =
                        Beregningsgrunnlag(
                            totalbeløp = 10000.0,
                            omregnedeÅrsinntekter =
                                listOf(
                                    OmregnetÅrsinntekt(arbeidsgiverreferanse = ORGANISASJONSNUMMER, beløp = 10000.0),
                                ),
                        ),
                ),
            )
        }
    }

    private fun opprettOpprinneligSøknadsdato(periode: Periode) =
        dbQuery.update(
            "INSERT INTO opprinnelig_soknadsdato VALUES (:vedtaksperiode_id, now())",
            "vedtaksperiode_id" to periode.id,
        )

    protected fun opprettVedtak(
        behandlingId: UUID = UUID.randomUUID(),
        personId: Long,
        periode: Periode = PERIODE,
        skjæringstidspunkt: LocalDate = periode.fom,
        forkastet: Boolean = false,
    ): Long {
        opprettBehandling(behandlingId, periode, skjæringstidspunkt)
        opprettOpprinneligSøknadsdato(periode)
        return dbQuery.updateAndReturnGeneratedKey(
            """
                INSERT INTO vedtaksperiode (vedtaksperiode_id, arbeidsgiver_identifikator, person_ref, forkastet)
                VALUES (:id, :arbeidsgiver_identifikator, :personId, :forkastet)
            """.trimMargin(),
            "id" to periode.id,
            "arbeidsgiver_identifikator" to ORGANISASJONSNUMMER,
            "personId" to personId,
            "forkastet" to forkastet,
        )!!
    }

    protected fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ): Long =
        requireNotNull(
            dbQuery.updateAndReturnGeneratedKey(
                """
                INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, opprettet) 
                VALUES (:definisjonId, :kode, :tittel, null, null, :opprettet)
                """.trimIndent(),
                "definisjonId" to definisjonId,
                "kode" to kode,
                "tittel" to tittel,
                "opprettet" to LocalDateTime.now(),
            ),
        )

    protected fun nyGenerasjon(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        generasjonId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode = PERIODE,
        tilstandEndretTidspunkt: LocalDateTime? = null,
        skjæringstidspunkt: LocalDate = periode.fom,
    ): Long =
        requireNotNull(
            dbQuery.updateAndReturnGeneratedKey(
                """
                INSERT INTO behandling (vedtaksperiode_id, unik_id, utbetaling_id, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, tilstand, fom, tom, skjæringstidspunkt) 
                VALUES (:vedtaksperiodeId, :generasjonId, :utbetalingId, :opprettetAvHendelse, :tilstandEndretTidspunkt, :tilstandEndretAvHendelse, 'VidereBehandlingAvklares', :fom, :tom, :skjaeringstidspunkt)
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiodeId,
                "generasjonId" to generasjonId,
                "utbetalingId" to utbetalingId,
                "opprettetAvHendelse" to UUID.randomUUID(),
                "tilstandEndretTidspunkt" to tilstandEndretTidspunkt,
                "tilstandEndretAvHendelse" to UUID.randomUUID(),
                "fom" to periode.fom,
                "tom" to periode.tom,
                "skjaeringstidspunkt" to skjæringstidspunkt,
            ),
        )

    protected fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime? = LocalDateTime.now(),
        kode: String = "EN_KODE",
        generasjonRef: Long,
        definisjonRef: Long? = null,
        status: String,
        endretTidspunkt: LocalDateTime? = LocalDateTime.now(),
    ) = dbQuery.update(
        """
        INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status, status_endret_ident, status_endret_tidspunkt) 
        VALUES (:id, :kode, :vedtaksperiodeId, :generasjonRef, :definisjonRef, :opprettet, :status, :ident, :endretTidspunkt)
        """.trimIndent(),
        "id" to id,
        "kode" to kode,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "generasjonRef" to generasjonRef,
        "definisjonRef" to definisjonRef,
        "opprettet" to opprettet,
        "status" to status,
        "ident" to if (endretTidspunkt != null) "EN_IDENT" else null,
        "endretTidspunkt" to endretTidspunkt,
    )

    protected fun klargjørVedtak(
        vedtakId: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode,
        kanAvvises: Boolean = true,
        behandlingId: UUID = UUID.randomUUID(),
    ) {
        opprettSaksbehandleroppgavetype(Periodetype.FØRSTEGANGSBEHANDLING, Inntektskilde.EN_ARBEIDSGIVER, vedtakId)
        val hendelseId = UUID.randomUUID()
        opprettHendelse(hendelseId)
        opprettAutomatisering(false, vedtaksperiodeId = periode.id, hendelseId = hendelseId)
        opprettOppgave(
            vedtaksperiodeId = periode.id,
            utbetalingId = utbetalingId,
            kanAvvises = kanAvvises,
            behandlingId = behandlingId,
        )
    }

    private fun opprettSaksbehandleroppgavetype(
        type: Periodetype,
        inntektskilde: Inntektskilde,
        vedtakRef: Long,
    ) = dbQuery.update(
        "INSERT INTO saksbehandleroppgavetype (type, vedtak_ref, inntektskilde) VALUES (:type, :vedtakRef, :inntektskilde)",
        "type" to type.toString(),
        "vedtakRef" to vedtakRef,
        "inntektskilde" to inntektskilde.toString(),
    )

    protected fun ferdigstillOppgave(vedtakRef: Long) =
        dbQuery.update(
            """
            UPDATE oppgave SET ferdigstilt_av = :ident, ferdigstilt_av_oid = :oid, status = 'Ferdigstilt', oppdatert = now()
            WHERE oppgave.vedtak_ref = :vedtakRef
            """.trimIndent(),
            "ident" to SAKSBEHANDLER.ident.value,
            "oid" to SAKSBEHANDLER.id.value,
            "vedtakRef" to vedtakRef,
        )

    protected fun opprettDialog() =
        sessionFactory.transactionalSessionScope { session ->
            Dialog.Factory
                .ny()
                .also(session.dialogRepository::lagre)
                .id()
        }

    protected fun opprettNotat(
        tekst: String = "Et notat",
        saksbehandlerOid: SaksbehandlerOid = SAKSBEHANDLER.id,
        vedtaksperiodeId: UUID = PERIODE.id,
        dialogRef: DialogId = opprettDialog(),
    ) = sessionFactory.transactionalSessionScope { session ->
        Notat.Factory
            .ny(
                type = NotatType.Generelt,
                tekst = tekst,
                dialogRef = dialogRef,
                vedtaksperiodeId = vedtaksperiodeId,
                saksbehandlerOid = saksbehandlerOid,
            ).also(session.notatRepository::lagre)
            .id()
    }

    protected fun opprettPerson(
        fødselsnummer: String = FØDSELSNUMMER,
        aktørId: String = AKTØRID,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
        bostedId: Int = ENHET.id,
        erEgenAnsatt: Boolean = false,
    ): Long {
        val personId = opprettMinimalPerson(fødselsnummer, aktørId)
        val personinfoid = opprettPersoninfo(adressebeskyttelse)
        val infotrygdutbetalingerid = opprettInfotrygdutbetalinger()
        oppdaterPersonpekere(fødselsnummer, personinfoid, infotrygdutbetalingerid)
        opprettEgenAnsatt(personId, erEgenAnsatt)
        oppdaterEnhet(personId, bostedId)
        return personId
    }

    protected fun opprettMinimalPerson(
        fødselsnummer: String = FØDSELSNUMMER,
        aktørId: String = AKTØRID,
    ) = opprettHelPerson(fødselsnummer, aktørId, null, null, null)

    private fun opprettHelPerson(
        fødselsnummer: String,
        aktørId: String,
        personinfoid: Long?,
        bostedId: Int?,
        infotrygdutbetalingerid: Long?,
    ) = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO person (fødselsnummer, aktør_id, info_ref, enhet_ref, infotrygdutbetalinger_ref)
            VALUES (:foedselsnummer, :aktoerId, :personinfoId, :enhetId, :infotrygdutbetalingerId)
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer,
            "aktoerId" to aktørId,
            "personinfoId" to personinfoid,
            "enhetId" to bostedId,
            "infotrygdutbetalingerId" to infotrygdutbetalingerid,
        ),
    )

    private fun opprettPersoninfo(adressebeskyttelse: Adressebeskyttelse) =
        dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO person_info (fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
            VALUES (:fornavn, :mellomnavn, :etternavn, :foedselsdato::date, :kjoenn::person_kjonn, :adressebeskyttelse)
            """.trimIndent(),
            "fornavn" to NAVN.fornavn,
            "mellomnavn" to NAVN.mellomnavn,
            "etternavn" to NAVN.etternavn,
            "foedselsdato" to LocalDate.of(1970, 1, 1),
            "kjoenn" to "Ukjent",
            "adressebeskyttelse" to adressebeskyttelse.name,
        )

    private fun oppdaterPersonpekere(
        fødselsnummer: String,
        personinfoId: Long? = null,
        infotrygdutbetalingerId: Long? = null,
    ) {
        dbQuery.update(
            """
            update person
            set info_ref=:personinfoId,
                infotrygdutbetalinger_ref=:infotrygdutbetalingerRef,
                personinfo_oppdatert = (
                    CASE 
                        when (:harPersoninfoId is not null) then now()
                    END
                ),
                infotrygdutbetalinger_oppdatert = (
                    CASE 
                        when (:harInfotrygdutbetalingerRef is not null) then now()
                    END
                )
            where fødselsnummer = :foedselsnummer
            """.trimIndent(),
            "personinfoId" to personinfoId,
            "harPersoninfoId" to (personinfoId != null),
            "infotrygdutbetalingerRef" to infotrygdutbetalingerId,
            "harInfotrygdutbetalingerRef" to (infotrygdutbetalingerId != null),
            "foedselsnummer" to fødselsnummer,
        )
    }

    protected fun oppdaterEnhet(
        personId: Long,
        enhetNr: Int,
    ) = dbQuery.update(
        "update person set enhet_ref = :enhetNr, enhet_ref_oppdatert = now() where id = :personId",
        "enhetNr" to enhetNr,
        "personId" to personId,
    )

    protected fun opprettEgenAnsatt(
        personId: Long,
        erEgenAnsatt: Boolean,
    ) = dbQuery.update(
        "INSERT INTO egen_ansatt VALUES (:personId, :erEgenAnsatt, now())",
        "personId" to personId,
        "erEgenAnsatt" to erEgenAnsatt,
    )

    protected fun opprettArbeidsgiver() {
        sessionFactory.transactionalSessionScope { session ->
            session.arbeidsgiverRepository.lagre(
                Arbeidsgiver.Factory.ny(
                    id = ArbeidsgiverIdentifikator.fraString(ORGANISASJONSNUMMER),
                    navnString = ARBEIDSGIVER_NAVN,
                ),
            )
        }
    }

    protected fun opprettSaksbehandler(
        oid: UUID = SAKSBEHANDLER.id.value,
        navn: String = SAKSBEHANDLER.navn,
        epost: String = SAKSBEHANDLER.epost,
        ident: String = SAKSBEHANDLER.ident.value,
    ): UUID {
        dbQuery.update(
            "INSERT INTO saksbehandler (oid, navn, epost, ident) VALUES (:oid, :navn, :epost, :ident)",
            "oid" to oid,
            "navn" to navn,
            "epost" to epost,
            "ident" to ident,
        )
        return oid
    }

    private fun opprettInfotrygdutbetalinger() =
        dbQuery.updateAndReturnGeneratedKey(
            "INSERT INTO infotrygdutbetalinger (data) VALUES ('[]')",
        )

    private fun opprettOppgave(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID = UUID.randomUUID(),
        kanAvvises: Boolean = true,
        behandlingId: UUID = UUID.randomUUID(),
    ): Long =
        sessionFactory.transactionalSessionScope {
            it.oppgaveDao.reserverNesteId().also { oppgaveId ->
                it.oppgaveRepository.lagre(
                    Oppgave.ny(
                        id = oppgaveId,
                        førsteOpprettet = null,
                        vedtaksperiodeId = vedtaksperiodeId,
                        behandlingId = behandlingId,
                        utbetalingId = utbetalingId,
                        hendelseId = UUID.randomUUID(),
                        kanAvvises = kanAvvises,
                        egenskaper =
                            setOf(
                                Egenskap.SØKNAD,
                                Egenskap.FORSTEGANGSBEHANDLING,
                                Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                                Egenskap.EN_ARBEIDSGIVER,
                            ),
                    ),
                )
            }
        }

    private fun opprettHendelse(
        hendelseId: UUID,
        fødselsnummer: String = FØDSELSNUMMER,
    ) = dbQuery.update(
        """
        INSERT INTO hendelse (id, data, type)
        VALUES (:hendelseId, :data::json, 'type')
        """.trimIndent(),
        "hendelseId" to hendelseId,
        "data" to """ { "fødselsnummer": "$fødselsnummer" } """,
    )

    private fun opprettAutomatisering(
        automatisert: Boolean,
        stikkprøve: Boolean = false,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID = UUID.randomUUID(),
    ) = dbQuery.update(
        """
        INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
        VALUES ((SELECT id FROM vedtaksperiode WHERE vedtaksperiode_id = :vedtaksperiodeId), :hendelseId, :automatisert, :stikkproeve, :utbetalingId);
        """.trimIndent(),
        "vedtaksperiodeId" to vedtaksperiodeId,
        "hendelseId" to hendelseId,
        "automatisert" to automatisert,
        "stikkproeve" to stikkprøve,
        "utbetalingId" to utbetalingId,
    )

    protected fun finnOppgaveIdFor(vedtaksperiodeId: UUID): Long =
        dbQuery.single(
            "SELECT o.id FROM oppgave o JOIN vedtaksperiode v ON v.id = o.vedtak_ref WHERE v.vedtaksperiode_id = :vedtaksperiode_id;",
            "vedtaksperiode_id" to vedtaksperiodeId,
        ) { it.long("id") }

    protected data class Navn(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
    )

    protected data class Enhet(
        val id: Int,
        val navn: String,
    )

    protected data class Periode(
        val id: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
    ) {
        companion object {
            infix fun LocalDate.til(tom: LocalDate) = Periode(UUID.randomUUID(), this, tom)
        }
    }
}
