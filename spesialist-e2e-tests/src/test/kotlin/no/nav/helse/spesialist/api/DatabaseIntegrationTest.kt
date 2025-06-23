package no.nav.helse.spesialist.api

import io.mockk.mockk
import no.nav.helse.db.api.ArbeidsgiverApiDao.Inntekter
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.e2e.AbstractDatabaseTest
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.apr
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.testfixtures.mar
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
    protected val dbQuery = DbQuery(dataSource)
    private val NAVN = Navn(lagFornavn(), lagFornavn(), lagEtternavn())
    private val ENHET = Enhet(101, "Halden")
    protected val PERIODE = Periode(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

    protected val ARBEIDSFORHOLD = Arbeidsforhold(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2), "EN TITTEL", 100)
    protected val SAKSBEHANDLER =
        Saksbehandler(
            oid = UUID.randomUUID(),
            navn = "Jan Banan",
            ident = lagSaksbehandlerident(),
            epost = "jan.banan@nav.no",
        )

    val FØDSELSNUMMER = lagFødselsnummer()
    val AKTØRID = lagAktørId()
    val ARBEIDSGIVER_NAVN = lagOrganisasjonsnavn()
    val ORGANISASJONSNUMMER = lagOrganisasjonsnummer()
    val ORGANISASJONSNUMMER_GHOST = lagOrganisasjonsnummer()

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

    protected val egenAnsattApiDao = mockk<EgenAnsattApiDao>(relaxed = true)
    protected val apiOppgaveService = mockk<ApiOppgaveService>(relaxed = true)

    protected fun opprettVedtaksperiode(
        personId: Long,
        arbeidsgiverId: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode = PERIODE,
        skjæringstidspunkt: LocalDate = periode.fom,
        forkastet: Boolean = false,
        kanAvvises: Boolean = true,
    ) = opprettVedtak(personId, arbeidsgiverId, periode, skjæringstidspunkt, forkastet).also {
        klargjørVedtak(
            it,
            utbetalingId,
            periode,
            kanAvvises = kanAvvises,
        )
    }

    private fun opprettGenerasjon(
        periode: Periode,
        skjæringstidspunkt: LocalDate = periode.fom,
    ) = requireNotNull(
        dbQuery.update(
            """
            INSERT INTO behandling (unik_id, vedtaksperiode_id, opprettet_av_hendelse, tilstand, fom, tom, skjæringstidspunkt)
            VALUES (:unik_id, :vedtaksperiode_id, :hendelse_id, 'VidereBehandlingAvklares',:fom, :tom, :skjaeringstidspunkt)
            """.trimIndent(),
            "unik_id" to UUID.randomUUID(),
            "vedtaksperiode_id" to periode.id,
            "hendelse_id" to UUID.randomUUID(),
            "fom" to periode.fom,
            "tom" to periode.tom,
            "skjaeringstidspunkt" to skjæringstidspunkt,
        )
    )

    protected fun opprettAvviksvurdering(
        fødselsnummer: String = FØDSELSNUMMER,
        skjæringstidspunkt: LocalDate = 1 jan 2018,
        avviksvurderingId: UUID = UUID.randomUUID(),
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
        avviksprosent: Double = 25.0
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
                )

            )
        }
    }

    private fun opprettOpprinneligSøknadsdato(periode: Periode) = dbQuery.update(
        "INSERT INTO opprinnelig_soknadsdato VALUES (:vedtaksperiode_id, now())",
        "vedtaksperiode_id" to periode.id,
    )

    protected fun opprettVedtak(
        personId: Long,
        arbeidsgiverId: Long,
        periode: Periode = PERIODE,
        skjæringstidspunkt: LocalDate = periode.fom,
        forkastet: Boolean = false,
    ): Long {
        opprettGenerasjon(periode, skjæringstidspunkt)
        opprettOpprinneligSøknadsdato(periode)
        return dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO vedtak (vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, forkastet)
            VALUES (:id, :fom, :tom, :arbeidsgiverId, :personId, :forkastet)
            """.trimMargin(),
            "id" to periode.id,
            "fom" to periode.fom,
            "tom" to periode.tom,
            "arbeidsgiverId" to arbeidsgiverId,
            "personId" to personId,
            "forkastet" to forkastet,
        )!!
    }

    protected fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ): Long = requireNotNull(
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
    ): Long = requireNotNull(
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
        )
    )

    protected fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime? = LocalDateTime.now(),
        kode: String = "EN_KODE",
        generasjonRef: Long,
        definisjonRef: Long? = null,
    ) = nyttVarsel(id, vedtaksperiodeId, opprettet, kode, generasjonRef, definisjonRef, "AKTIV", null)

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
    ) {
        opprettSaksbehandleroppgavetype(Periodetype.FØRSTEGANGSBEHANDLING, Inntektskilde.EN_ARBEIDSGIVER, vedtakId)
        val hendelseId = UUID.randomUUID()
        opprettHendelse(hendelseId)
        opprettAutomatisering(false, vedtaksperiodeId = periode.id, hendelseId = hendelseId)
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, vedtakId, utbetalingId, kanAvvises = kanAvvises)
    }

    private fun opprettSaksbehandleroppgavetype(
        type: Periodetype,
        inntektskilde: Inntektskilde,
        vedtakRef: Long,
    ) = dbQuery.update(
        "INSERT INTO saksbehandleroppgavetype (type, vedtak_ref, inntektskilde) VALUES (:type, :vedtakRef, :inntektskilde)",
        "type" to type.toString(),
        "vedtakRef" to vedtakRef,
        "inntektskilde" to inntektskilde.toString()
    )

    protected fun ferdigstillOppgave(vedtakRef: Long) = dbQuery.update(
        """
        UPDATE oppgave SET ferdigstilt_av = :ident, ferdigstilt_av_oid = :oid, status = 'Ferdigstilt', oppdatert = now()
        WHERE oppgave.vedtak_ref = :vedtakRef
        """.trimIndent(),
        "ident" to SAKSBEHANDLER.ident,
        "oid" to SAKSBEHANDLER.oid,
        "vedtakRef" to vedtakRef,
    )

    protected fun opprettDialog() =
        sessionFactory.transactionalSessionScope { session ->
            Dialog.Factory.ny().also(session.dialogRepository::lagre).id()
        }

    protected fun opprettNotat(
        tekst: String = "Et notat",
        saksbehandlerOid: SaksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER.oid),
        vedtaksperiodeId: UUID = PERIODE.id,
        dialogRef: DialogId = opprettDialog(),
    ) = sessionFactory.transactionalSessionScope { session ->
        Notat.Factory.ny(
            type = NotatType.Generelt,
            tekst = tekst,
            dialogRef = dialogRef,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid
        ).also(session.notatRepository::lagre).id()
    }

    protected fun opprettPersonOld(
        fødselsnummer: String = FØDSELSNUMMER,
        aktørId: String = AKTØRID,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
        bostedId: Int = ENHET.id,
        erEgenAnsatt: Boolean = false,
    ): Long {
        val personinfoid = opprettPersoninfo(adressebeskyttelse)
        val infotrygdutbetalingerid = opprettInfotrygdutbetalinger()
        val personId = opprettHelPerson(fødselsnummer, aktørId, personinfoid, bostedId, infotrygdutbetalingerid)
        opprettEgenAnsatt(personId, erEgenAnsatt)
        return personId
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
        )
    )

    protected fun opprettPåVent(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        frist: LocalDate = LocalDate.now().plusDays(21),
        saksbehandlerOid: UUID = SAKSBEHANDLER.oid,
    ) = dbQuery.update(
        "INSERT INTO pa_vent (vedtaksperiode_id, frist, saksbehandler_ref) VALUES (:vedtaksperiodeId, :frist, :oid)",
        "vedtaksperiodeId" to vedtaksperiodeId,
        "frist" to frist,
        "oid" to saksbehandlerOid,
    )

    protected fun oppdaterPersoninfo(adressebeskyttelse: Adressebeskyttelse) {
        val personinfoId = opprettPersoninfo(adressebeskyttelse)
        oppdaterPersonpekere(FØDSELSNUMMER, personinfoId)
    }

    private fun opprettPersoninfo(adressebeskyttelse: Adressebeskyttelse) = dbQuery.updateAndReturnGeneratedKey(
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

    protected fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGANISASJONSNUMMER,
    ): Long =
        Arbeidsgiver.Factory.ny(identifikator = Arbeidsgiver.Identifikator.fraString(organisasjonsnummer))
            .also { it.oppdaterMedNavn(navn = ARBEIDSGIVER_NAVN) }
            .also { arbeidsgiver ->
                sessionFactory.transactionalSessionScope {
                    it.arbeidsgiverRepository.lagre(arbeidsgiver)
                }
            }
            .id().value.toLong()

    protected fun opprettSaksbehandler(
        oid: UUID = SAKSBEHANDLER.oid,
        navn: String = SAKSBEHANDLER.navn,
        epost: String = SAKSBEHANDLER.epost,
        ident: String = SAKSBEHANDLER.ident,
    ): UUID {
        dbQuery.update(
            "INSERT INTO saksbehandler (oid, navn, epost, ident) VALUES (:oid, :navn, :epost, :ident)",
            "oid" to oid,
            "navn" to navn,
            "epost" to epost,
            "ident" to ident
        )
        return oid
    }

    protected fun opprettArbeidsforhold(
        personId: Long,
        arbeidsgiverId: Long,
    ) = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO arbeidsforhold (person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent, oppdatert)
        VALUES (:personId, :arbeidsgiverId, :startdato, :sluttdato, :tittel, :prosent, :oppdatert)
        """.trimIndent(),
        "personId" to personId,
        "arbeidsgiverId" to arbeidsgiverId,
        "startdato" to ARBEIDSFORHOLD.start,
        "sluttdato" to ARBEIDSFORHOLD.slutt,
        "tittel" to ARBEIDSFORHOLD.tittel,
        "prosent" to ARBEIDSFORHOLD.prosent,
        "oppdatert" to LocalDateTime.now(),
    )

    private fun opprettInfotrygdutbetalinger() = dbQuery.updateAndReturnGeneratedKey(
        "INSERT INTO infotrygdutbetalinger (data) VALUES ('[]')"
    )

    private fun opprettOppgave(
        status: Oppgavestatus = Oppgavestatus.AvventerSaksbehandler,
        vedtakRef: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        kanAvvises: Boolean = true,
    ): Long {
        val oppgaveId = dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO oppgave (utbetaling_id, opprettet, oppdatert, status, vedtak_ref, hendelse_id_godkjenningsbehov, kan_avvises)
            VALUES (:utbetalingId, :opprettet, now(), CAST(:status as oppgavestatus), :vedtakRef, :godkjenningsbehovId, :kanAvvises)
            """.trimIndent(),
            "utbetalingId" to utbetalingId,
            "opprettet" to opprettet,
            "status" to status.name,
            "vedtakRef" to vedtakRef,
            "godkjenningsbehovId" to UUID.randomUUID(),
            "kanAvvises" to kanAvvises,
        )
        return requireNotNull(oppgaveId)
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
        "data" to """ { "fødselsnummer": "$fødselsnummer" } """
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
        VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId), :hendelseId, :automatisert, :stikkproeve, :utbetalingId);
        """.trimIndent(),
        "vedtaksperiodeId" to vedtaksperiodeId,
        "hendelseId" to hendelseId,
        "automatisert" to automatisert,
        "stikkproeve" to stikkprøve,
        "utbetalingId" to utbetalingId,
    )

    protected fun opprettInntekt(
        personId: Long,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ) = dbQuery.update(
        """
        INSERT INTO inntekt (person_ref, skjaeringstidspunkt, inntekter)
        VALUES (:person_ref, :skjaeringstidspunkt, :inntekter::json)
        """.trimIndent(),
        "person_ref" to personId,
        "skjaeringstidspunkt" to skjæringstidspunkt,
        "inntekter" to objectMapper.writeValueAsString(inntekter),
    )

    protected fun tildelOppgave(
        oppgaveRef: Long,
        saksbehandlerOid: UUID,
    ) = dbQuery.update(
        "INSERT INTO tildeling (saksbehandler_ref, oppgave_id_ref) VALUES (:oid, :oppgaveRef)",
        "oid" to saksbehandlerOid,
        "oppgaveRef" to oppgaveRef,
    )

    protected fun assertGodkjenteVarsler(
        generasjonRef: Long,
        forventetAntall: Int,
    ) {
        val antall = dbQuery.single(
            "SELECT COUNT(1) FROM selve_varsel sv WHERE sv.generasjon_ref = :generasjonRef AND status = 'GODKJENT'",
            "generasjonRef" to generasjonRef
        ) { it.int(1) }
        assertEquals(forventetAntall, antall)
    }

    protected fun finnOppgaveIdFor(vedtaksperiodeId: UUID): Long = dbQuery.single(
        "SELECT o.id FROM oppgave o JOIN vedtak v ON v.id = o.vedtak_ref WHERE v.vedtaksperiode_id = :vedtaksperiode_id;",
        "vedtaksperiode_id" to vedtaksperiodeId
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
            infix fun LocalDate.til(tom: LocalDate) =
                Periode(UUID.randomUUID(), this, tom)
        }
    }

    protected data class Arbeidsforhold(
        val start: LocalDate,
        val slutt: LocalDate,
        val tittel: String,
        val prosent: Int,
    )

    protected data class Saksbehandler(
        val oid: UUID,
        val navn: String,
        val ident: String,
        val epost: String,
    )
}
