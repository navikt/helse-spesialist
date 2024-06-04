package no.nav.helse.spesialist.api

import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.utils.io.core.use
import io.mockk.every
import io.mockk.mockk
import kotliquery.Query
import kotliquery.Row
import kotliquery.action.QueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao.Inntekter
import no.nav.helse.spesialist.api.db.AbstractDatabaseTest
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagEtternavn
import no.nav.helse.spesialist.test.lagFornavn
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnavn
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.spesialist.test.lagSaksbehandlerident
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.helse.spleis.graphql.enums.GraphQLHendelsetype
import no.nav.helse.spleis.graphql.enums.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.enums.Utbetalingtype
import no.nav.helse.spleis.graphql.hentsnapshot.Alder
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLHendelse
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLRefusjonselement
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsledig
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.properties.Delegates

internal abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
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

    protected val apiVarselRepository = ApiVarselRepository(dataSource)
    protected val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    protected val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    protected val notatDao = NotatDao(dataSource)
    protected val totrinnsvurderingApiDao = TotrinnsvurderingApiDao(dataSource)
    protected val påVentApiDao = PåVentApiDao(dataSource)
    protected val personApiDao = PersonApiDao(dataSource)
    protected val tildelingDao = TildelingDao(dataSource)
    protected val overstyringApiDao = OverstyringApiDao(dataSource)
    protected val oppgaveApiDao = OppgaveApiDao(dataSource)
    protected val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    private val snapshotApiDao = SnapshotApiDao(dataSource)

    protected val egenAnsattApiDao = mockk<EgenAnsattApiDao>(relaxed = true)
    protected val snapshotClient = mockk<SnapshotClient>(relaxed = true)

    protected var sisteOppgaveId by Delegates.notNull<Long>()
    protected var sisteCommandContextId by Delegates.notNull<UUID>()

    protected val snapshotMediator = SnapshotMediator(snapshotApiDao, snapshotClient)
    protected val notatMediator = mockk<NotatMediator>(relaxed = true)
    protected val oppgavehåndterer = mockk<Oppgavehåndterer>(relaxed = true)
    protected val totrinnsvurderinghåndterer = mockk<Totrinnsvurderinghåndterer>(relaxed = true)

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
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement =
            "INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse, tilstand, fom, tom, skjæringstidspunkt) VALUES (:unik_id, :vedtaksperiode_id, :hendelse_id, 'VidereBehandlingAvklares',:fom, :tom, :skjaeringstidspunkt)"
        requireNotNull(
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "unik_id" to UUID.randomUUID(),
                        "vedtaksperiode_id" to periode.id,
                        "hendelse_id" to UUID.randomUUID(),
                        "fom" to periode.fom,
                        "tom" to periode.tom,
                        "skjaeringstidspunkt" to skjæringstidspunkt,
                    ),
                ).asUpdate,
            ),
        )
    }

    private fun opprettOpprinneligSøknadsdato(periode: Periode) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                "INSERT INTO opprinnelig_soknadsdato VALUES (:vedtaksperiode_id, now())"
            requireNotNull(
                session.run(
                    queryOf(
                        statement,
                        mapOf(
                            "vedtaksperiode_id" to periode.id,
                        ),
                    ).asUpdate,
                ),
            )
        }

    protected fun opprettVedtak(
        personId: Long,
        arbeidsgiverId: Long,
        periode: Periode = PERIODE,
        skjæringstidspunkt: LocalDate = periode.fom,
        forkastet: Boolean = false,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        opprettGenerasjon(periode, skjæringstidspunkt)
        opprettOpprinneligSøknadsdato(periode)

        @Language("PostgreSQL")
        val statement =
            "INSERT INTO vedtak(vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, forkastet) VALUES(?, ?, ?, ?, ?, ?)"
        requireNotNull(
            session.run(
                queryOf(
                    statement,
                    periode.id,
                    periode.fom,
                    periode.tom,
                    arbeidsgiverId,
                    personId,
                    forkastet,
                ).asUpdateAndReturnGeneratedKey,
            ),
        )
    }

    protected fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ): Long =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val query = """
            INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, opprettet) 
            VALUES (?, ?, ?, ?, ?, ?)    
        """
            requireNotNull(
                session.run(
                    queryOf(
                        query,
                        definisjonId,
                        kode,
                        tittel,
                        null,
                        null,
                        LocalDateTime.now(),
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        }

    protected fun nyGenerasjon(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        generasjonId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode = PERIODE,
        tilstandEndretTidspunkt: LocalDateTime? = null,
        skjæringstidspunkt: LocalDate = periode.fom,
    ): Long =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val query = """
            INSERT INTO selve_vedtaksperiode_generasjon(vedtaksperiode_id, unik_id, utbetaling_id, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, tilstand, fom, tom, skjæringstidspunkt) 
            VALUES (?, ?, ?, ?, ?, ?, 'VidereBehandlingAvklares', ?, ?, ?)
        """
            return requireNotNull(
                session.run(
                    queryOf(
                        query,
                        vedtaksperiodeId,
                        generasjonId,
                        utbetalingId,
                        UUID.randomUUID(),
                        tilstandEndretTidspunkt,
                        UUID.randomUUID(),
                        periode.fom,
                        periode.tom,
                        skjæringstidspunkt,
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        }

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
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_varsel(unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status, status_endret_ident, status_endret_tidspunkt) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        session.run(
            queryOf(
                query,
                id,
                kode,
                vedtaksperiodeId,
                generasjonRef,
                definisjonRef,
                opprettet,
                status,
                if (endretTidspunkt != null) "EN_IDENT" else null,
                endretTidspunkt,
            ).asExecute,
        )
    }

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
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO saksbehandleroppgavetype(type, vedtak_ref, inntektskilde) VALUES (?, ?, ?)"
        session.run(queryOf(statement, type.toString(), vedtakRef, inntektskilde.toString()).asUpdate)
    }

    protected fun ferdigstillOppgave(vedtakRef: Long) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                """
                UPDATE oppgave SET ferdigstilt_av = ?, ferdigstilt_av_oid = ?, status = 'Ferdigstilt', oppdatert = now()
                WHERE oppgave.vedtak_ref = ?
                """.trimIndent()
            session.run(queryOf(statement, SAKSBEHANDLER.ident, SAKSBEHANDLER.oid, vedtakRef).asUpdate)
        }
    }

    protected fun opprettNotat(
        tekst: String = "Et notat",
        saksbehandlerOid: UUID = SAKSBEHANDLER.oid,
        vedtaksperiodeId: UUID = PERIODE.id,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement =
            "INSERT INTO notat(tekst, saksbehandler_oid, vedtaksperiode_id, type) VALUES (?, ?, ?, CAST(? as notattype))"
        session.run(
            queryOf(
                statement,
                tekst,
                saksbehandlerOid,
                vedtaksperiodeId,
                NotatType.Generelt.name,
            ).asUpdateAndReturnGeneratedKey,
        )
    }

    protected fun opprettKommentar(
        tekst: String = "En kommentar",
        notatRef: Int,
        saksbehandlerIdent: String = SAKSBEHANDLER.ident,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO kommentarer(tekst, notat_ref, saksbehandlerident) VALUES (?, ?, ?)"
        session.run(
            queryOf(
                statement,
                tekst,
                notatRef,
                saksbehandlerIdent,
            ).asUpdateAndReturnGeneratedKey,
        )
    }

    protected fun vedtakId(vedtaksperiodeId: UUID = PERIODE.id) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
            requireNotNull(session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)) {
                "Finner ikke vedtak i db for vedtaksperiodeId=$vedtaksperiodeId"
            }
        }

    protected fun opprettPerson(
        fødselsnummer: String = FØDSELSNUMMER,
        aktørId: String = AKTØRID,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
        bostedId: Int = ENHET.id,
        erEgenAnsatt: Boolean = false,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        val personinfoid = opprettPersoninfo(adressebeskyttelse)
        val infotrygdutbetalingerid = opprettInfotrygdutbetalinger()

        @Language("PostgreSQL")
        val statement =
            "INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref) VALUES(?, ?, ?, ?, ?)"
        val personId =
            requireNotNull(
                session.run(
                    queryOf(
                        statement,
                        fødselsnummer.toLong(),
                        aktørId.toLong(),
                        personinfoid,
                        bostedId,
                        infotrygdutbetalingerid,
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        opprettEgenAnsatt(personId, erEgenAnsatt)
        personId
    }

    protected fun opprettPåVent(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        frist: LocalDate = LocalDate.now().plusDays(21),
        begrunnelse: String = "En begrunnelse",
        saksbehandlerOid: UUID = SAKSBEHANDLER.oid,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement =
            "INSERT INTO pa_vent(vedtaksperiode_id, frist, begrunnelse, saksbehandler_ref) VALUES(?, ?, ?, ?)"
        session.run(
            queryOf(
                statement,
                vedtaksperiodeId,
                frist,
                begrunnelse,
                saksbehandlerOid,
            ).asUpdate,
        )
    }

    private fun opprettPersoninfo(adressebeskyttelse: Adressebeskyttelse) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val (fornavn, mellomnavn, etternavn) = NAVN

            @Language("PostgreSQL")
            val statement =
                "INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse) VALUES(?, ?, ?, ?::date, ?::person_kjonn, ?)"
            requireNotNull(
                session.run(
                    queryOf(
                        statement,
                        fornavn,
                        mellomnavn,
                        etternavn,
                        LocalDate.of(1970, 1, 1),
                        "Ukjent",
                        adressebeskyttelse.name,
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        }

    private fun opprettEgenAnsatt(
        personId: Long,
        erEgenAnsatt: Boolean,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement =
            "INSERT INTO egen_ansatt VALUES($personId, $erEgenAnsatt, now())"
        requireNotNull(session.run(queryOf(statement).asUpdate))
    }

    protected fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGANISASJONSNUMMER,
        bransjer: List<String> = emptyList(),
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        val bransjeid = opprettBransjer(bransjer)
        val navnid = opprettArbeidsgivernavn()

        @Language("PostgreSQL")
        val statement =
            "INSERT INTO arbeidsgiver(orgnummer, navn_ref, bransjer_ref) VALUES(?, ?, ?) ON CONFLICT DO NOTHING"
        requireNotNull(
            session.run(
                queryOf(
                    statement,
                    organisasjonsnummer.toLong(),
                    navnid,
                    bransjeid,
                ).asUpdateAndReturnGeneratedKey,
            ),
        )
    }

    private fun finnArbeidsgiverId(): Int =
        requireNotNull(
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val statement = "SELECT id FROM arbeidsgiver WHERE orgnummer = ?"
                session.run(queryOf(statement, ORGANISASJONSNUMMER.toLong()).map { it.int("id") }.asSingle)
            },
        )

    protected fun opprettSaksbehandler(
        oid: UUID = SAKSBEHANDLER.oid,
        navn: String = SAKSBEHANDLER.navn,
        epost: String = SAKSBEHANDLER.epost,
        ident: String = SAKSBEHANDLER.ident,
    ): UUID {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES (?, ?, ?, ?)"
            session.run(queryOf(statement, oid, navn, epost, ident).asUpdate)
        }
        return oid
    }

    protected fun opprettArbeidsforhold(
        personid: Long,
        arbeidsgiverid: Long,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement =
            "INSERT INTO arbeidsforhold(person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent) VALUES(?, ?, ?, ?, ?, ?)"
        requireNotNull(
            session.run(
                queryOf(
                    statement,
                    personid,
                    arbeidsgiverid,
                    ARBEIDSFORHOLD.start,
                    ARBEIDSFORHOLD.slutt,
                    ARBEIDSFORHOLD.tittel,
                    ARBEIDSFORHOLD.prosent,
                ).asUpdateAndReturnGeneratedKey,
            ),
        )
    }

    private fun opprettBransjer(bransjer: List<String>) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO arbeidsgiver_bransjer(bransjer) VALUES(?)"
            requireNotNull(
                session.run(
                    queryOf(
                        statement,
                        objectMapper.writeValueAsString(bransjer),
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        }

    private fun opprettArbeidsgivernavn() =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO arbeidsgiver_navn(navn) VALUES(?)"
            requireNotNull(session.run(queryOf(statement, ARBEIDSGIVER_NAVN).asUpdateAndReturnGeneratedKey))
        }

    private fun opprettInfotrygdutbetalinger() =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO infotrygdutbetalinger(data) VALUES('[]')"
            requireNotNull(session.run(queryOf(statement).asUpdateAndReturnGeneratedKey))
        }

    private fun opprettSnapshot() =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO snapshot(data, versjon) VALUES(?::json, ?)"
            requireNotNull(
                session.run(
                    queryOf(
                        statement,
                        objectMapper.writeValueAsString(snapshot()),
                        1,
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        }

    private fun opprettOppgave(
        status: Oppgavestatus = Oppgavestatus.AvventerSaksbehandler,
        vedtakRef: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        kanAvvises: Boolean = true,
    ): Long {
        val commandContextId = UUID.randomUUID()
        return requireNotNull(
            sessionOf(dataSource, returnGeneratedKey = true).use { session ->
                @Language("PostgreSQL")
                val statement =
                    "INSERT INTO oppgave(utbetaling_id, opprettet, oppdatert, status, vedtak_ref, command_context_id, kan_avvises) VALUES(?, ?, now(), CAST(? as oppgavestatus), ?, ?, ?)"
                session.run(
                    queryOf(
                        statement,
                        utbetalingId,
                        opprettet,
                        status.name,
                        vedtakRef,
                        commandContextId,
                        kanAvvises,
                    ).asUpdateAndReturnGeneratedKey,
                )
            },
        ).also {
            sisteOppgaveId = it
            sisteCommandContextId = commandContextId
        }
    }

    private fun opprettHendelse(
        hendelseId: UUID,
        foedselsnummer: String = FØDSELSNUMMER,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO hendelse(id, fodselsnummer, data, type)
            VALUES (:hendelseId, :foedselsnummer, '{}', 'type')
        """
        session.run(
            queryOf(
                statement,
                mapOf("hendelseId" to hendelseId, "foedselsnummer" to foedselsnummer.toLong()),
            ).asUpdate,
        )
    }

    private fun opprettAutomatisering(
        automatisert: Boolean,
        stikkprøve: Boolean = false,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID = UUID.randomUUID(),
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = """
                INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
                VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId), :hendelseId, :automatisert, :stikkproeve, :utbetalingId);
            """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "hendelseId" to hendelseId,
                    "automatisert" to automatisert,
                    "stikkproeve" to stikkprøve,
                    "utbetalingId" to utbetalingId,
                ),
            ).asUpdate,
        )
    }

    protected fun opprettInntekt(
        personId: Long,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ) = sessionOf(dataSource).use { session ->
        session.transaction { transaction ->
            @Language("PostgreSQL")
            val query = """
                        INSERT INTO inntekt (person_ref, skjaeringstidspunkt, inntekter)
                        VALUES (:person_ref, :skjaeringstidspunkt, :inntekter::json)
                    """
            transaction.run(
                queryOf(
                    query,
                    mapOf(
                        "person_ref" to personId,
                        "skjaeringstidspunkt" to skjæringstidspunkt,
                        "inntekter" to objectMapper.writeValueAsString(inntekter),
                    ),
                ).asExecute,
            )
        }
    }

    protected fun tildelOppgave(
        oppgaveRef: Long,
        saksbehandlerOid: UUID,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement =
            "INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref) VALUES(?, ?)"
        session.run(
            queryOf(
                statement,
                saksbehandlerOid,
                oppgaveRef,
            ).asUpdate,
        )
    }

    open fun mockSnapshot(
        fødselsnummer: String = FØDSELSNUMMER,
        avviksprosent: Double = 0.0,
        arbeidsgivere: List<GraphQLArbeidsgiver> = emptyList(),
    ) {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns
            object :
                GraphQLClientResponse<HentSnapshot.Result> {
                override val data = HentSnapshot.Result(snapshot(fødselsnummer, arbeidsgivere))
            }
    }

    private fun snapshot(
        fødselsnummer: String = FØDSELSNUMMER,
        arbeidsgivere: List<GraphQLArbeidsgiver> = emptyList(),
    ): GraphQLPerson {
        val vilkårsgrunnlag =
            GraphQLSpleisVilkarsgrunnlag(
                id = UUID.randomUUID(),
                inntekter =
                    listOf(
                        GraphQLArbeidsgiverinntekt(
                            arbeidsgiver = ORGANISASJONSNUMMER,
                            omregnetArsinntekt =
                                GraphQLOmregnetArsinntekt(
                                    belop = 500_000.0,
                                    manedsbelop = 55_000.0,
                                    kilde = GraphQLInntektskilde.INNTEKTSMELDING,
                                ),
                        ),
                        GraphQLArbeidsgiverinntekt(
                            arbeidsgiver = "987656789",
                            omregnetArsinntekt =
                                GraphQLOmregnetArsinntekt(
                                    belop = 500_000.0,
                                    manedsbelop = 55_000.0,
                                    kilde = GraphQLInntektskilde.INNTEKTSMELDING,
                                ),
                        ),
                    ),
                omregnetArsinntekt = 1_000_000.0,
                skjonnsmessigFastsattAarlig = 0.0,
                skjaeringstidspunkt = 1.januar(2020),
                sykepengegrunnlag = 1_000_000.0,
                antallOpptjeningsdagerErMinst = 123,
                grunnbelop = 100_000,
                sykepengegrunnlagsgrense =
                    GraphQLSykepengegrunnlagsgrense(
                        grunnbelop = 100_000,
                        grense = 600_000,
                        virkningstidspunkt = 1.januar(2020),
                    ),
                oppfyllerKravOmMedlemskap = true,
                oppfyllerKravOmMinstelonn = true,
                oppfyllerKravOmOpptjening = true,
                opptjeningFra = 1.januar(2000),
                arbeidsgiverrefusjoner =
                    listOf(
                        GraphQLArbeidsgiverrefusjon(
                            arbeidsgiver = ORGANISASJONSNUMMER,
                            refusjonsopplysninger =
                                listOf(
                                    GraphQLRefusjonselement(
                                        fom = 1.januar(2020),
                                        tom = null,
                                        belop = 30000.0,
                                        meldingsreferanseId = UUID.randomUUID(),
                                    ),
                                ),
                        ),
                    ),
            )

        return GraphQLPerson(
            aktorId = AKTØRID,
            arbeidsgivere = arbeidsgivere,
            dodsdato = null,
            fodselsnummer = fødselsnummer,
            versjon = 1,
            vilkarsgrunnlag = listOf(vilkårsgrunnlag),
        )
    }

    protected fun opprettSnapshotArbeidsgiver(generasjoner: List<GraphQLGenerasjon>) =
        GraphQLArbeidsgiver(
            organisasjonsnummer = ORGANISASJONSNUMMER,
            ghostPerioder = emptyList(),
            generasjoner = generasjoner,
        )

    protected fun opprettSnapshotHendelse(eksternDokumentId: UUID) =
        GraphQLSoknadArbeidsledig(
            id = UUID.randomUUID().toString(),
            eksternDokumentId = eksternDokumentId.toString(),
            fom = 11.mai(2022),
            tom = 30.mai(2022),
            rapportertDato = 10.oktober(2023).atStartOfDay(),
            sendtNav = 10.oktober(2023).atStartOfDay(),
            type = GraphQLHendelsetype.SENDTSOKNADARBEIDSLEDIG,
        )

    protected fun opprettSnapshotGenerasjon(
        perioder: List<GraphQLTidslinjeperiode>,
        id: UUID = UUID.randomUUID(),
    ) = GraphQLGenerasjon(id = id, perioder = perioder)

    protected fun opprettBeregnetPeriode(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        behandlingId: UUID = UUID.randomUUID(),
        hendelser: List<GraphQLHendelse> = emptyList(),
    ) = GraphQLBeregnetPeriode(
        erForkastet = false,
        fom = fom,
        tom = tom,
        inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now(),
        periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
        periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
        skjaeringstidspunkt = LocalDate.now(),
        tidslinje = emptyList(),
        vedtaksperiodeId = vedtaksperiodeId,
        beregningId = UUID.randomUUID(),
        forbrukteSykedager = null,
        gjenstaendeSykedager = null,
        hendelser = hendelser,
        maksdato = LocalDate.now(),
        vilkarsgrunnlagId = null,
        periodevilkar =
            GraphQLPeriodevilkar(
                alder = Alder(55, true),
                sykepengedager =
                    Sykepengedager(
                        maksdato = LocalDate.now(),
                        oppfylt = true,
                        skjaeringstidspunkt = LocalDate.now(),
                    ),
            ),
        behandlingId = behandlingId,
        utbetaling =
            GraphQLUtbetaling(
                id = utbetalingId,
                arbeidsgiverFagsystemId = "EN_FAGSYSTEM_ID",
                arbeidsgiverNettoBelop = 1,
                personFagsystemId = "EN_FAGSYSTEM_ID",
                personNettoBelop = 0,
                statusEnum = GraphQLUtbetalingstatus.IKKEGODKJENT,
                typeEnum = Utbetalingtype.UTBETALING,
            ),
    )

    protected fun opprettUberegnetPeriode(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        behandlingId: UUID = UUID.randomUUID(),
    ) = GraphQLUberegnetPeriode(
        erForkastet = false,
        fom = fom,
        tom = tom,
        inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now(),
        periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
        periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
        skjaeringstidspunkt = LocalDate.now(),
        tidslinje = emptyList(),
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        hendelser = emptyList(),
    )

    protected fun assertGodkjenteVarsler(
        generasjonRef: Long,
        forventetAntall: Int,
    ) {
        @Language("PostgreSQL")
        val query =
            "SELECT COUNT(1) FROM selve_varsel sv WHERE sv.generasjon_ref = ? AND status = 'GODKJENT'"
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, generasjonRef).map { it.int(1) }.asSingle)
            }
        assertEquals(forventetAntall, antall)
    }

    protected fun assertAvvisteVarsler(
        generasjonRef: Long,
        forventetAntall: Int,
    ) {
        @Language("PostgreSQL")
        val query =
            "SELECT COUNT(1) FROM selve_varsel sv WHERE sv.generasjon_ref = ? AND status = 'AVVIST'"
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, generasjonRef).map { it.int(1) }.asSingle)
            }
        assertEquals(forventetAntall, antall)
    }

    protected fun finnOppgaveIdFor(vedtaksperiodeId: UUID): Long =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT o.id FROM oppgave o JOIN vedtak v ON v.id = o.vedtak_ref WHERE v.vedtaksperiode_id = :vedtaksperiode_id;"
            return requireNotNull(
                session.run(
                    queryOf(
                        query,
                        mapOf("vedtaksperiode_id" to vedtaksperiodeId),
                    ).map { it.long("id") }.asSingle,
                ),
            )
        }

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
    )

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

    protected fun query(
        @Language("postgresql") query: String,
        vararg params: Pair<String, Any>,
    ) = queryOf(query, params.toMap())

    protected fun Query.update() = asUpdate.runInSession()

    protected fun <T> Query.single(mapper: (Row) -> T?) = map(mapper).asSingle.runInSession()

    protected fun <T> Query.list(mapper: (Row) -> T?) = map(mapper).asList.runInSession()

    private fun <T> QueryAction<T>.runInSession() = sessionOf(dataSource).use(::runWithSession)
}
