package no.nav.helse.spesialist.api

import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.db.AbstractDatabaseTest
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.HentSnapshot
import no.nav.helse.spesialist.api.graphql.enums.GraphQLInntektstype
import no.nav.helse.spesialist.api.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spesialist.api.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spesialist.api.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spesialist.api.graphql.enums.GraphQLVilkarsgrunnlagtype
import no.nav.helse.spesialist.api.graphql.enums.Utbetalingtype
import no.nav.helse.spesialist.api.graphql.hentsnapshot.Alder
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLRefusjonselement
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUberegnetPeriode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spesialist.api.graphql.hentsnapshot.Sykepengedager
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao
import org.intellij.lang.annotations.Language

internal abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
    protected companion object {
        val NAVN = Navn("Ola", "Kari", "Nordhen")
        val ENHET = Enhet(101, "Halden")
        val PERIODE = Periode(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val ARBEIDSFORHOLD = Arbeidsforhold(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2), "EN TITTEL", 100)
        val SAKSBEHANDLER = Saksbehandler(UUID.randomUUID(), "Jan Banan", "jan.banan@nav.no", "B123456")

        const val FØDSELSNUMMER = "01017011111"
        const val AKTØRID = "01017011111111"
        const val ARBEIDSGIVER_NAVN = "EN ARBEIDSGIVER"
        const val ORGANISASJONSNUMMER = "987654321"
    }

    protected val varselDao = VarselDao(dataSource)
    protected val apiVarselRepository = ApiVarselRepository(dataSource)
    protected val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    protected val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    protected val saksbehandlerDao = SaksbehandlerDao(dataSource)
    protected val notatDao = NotatDao(dataSource)
    protected val totrinnsvurderingApiDao = TotrinnsvurderingApiDao(dataSource)
    protected val personApiDao = PersonApiDao(dataSource)
    protected val tildelingDao = TildelingDao(dataSource)
    protected val overstyringApiDao = OverstyringApiDao(dataSource)
    protected val oppgaveApiDao = OppgaveApiDao(dataSource)
    protected val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    private val snapshotApiDao = SnapshotApiDao(dataSource)

    protected val utbetalingApiDao = mockk<UtbetalingApiDao>(relaxed = true)
    protected val egenAnsattApiDao = mockk<EgenAnsattApiDao>(relaxed = true)
    protected val snapshotClient = mockk<SnapshotClient>(relaxed = true)

    protected val snapshotMediator = SnapshotMediator(snapshotApiDao, snapshotClient)

    protected fun opprettVedtaksperiode(
        personId: Long,
        arbeidsgiverId: Long,
        utbetalingId: UUID? = null,
        periode: Periode = PERIODE,
        oppgavetype: Oppgavetype = Oppgavetype.SØKNAD,
    ) = opprettVedtak(personId, arbeidsgiverId, periode).also { klargjørVedtak(it, utbetalingId, periode, oppgavetype) }

    protected fun opprettGenerasjon(
        periode: Periode,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement =
            "INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse, tilstand) VALUES (:unik_id, :vedtaksperiode_id, :hendelse_id, 'Ulåst')"
        requireNotNull(
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "unik_id" to UUID.randomUUID(),
                        "vedtaksperiode_id" to periode.id,
                        "hendelse_id" to UUID.randomUUID(),
                    )
                ).asUpdate
            )
        )
    }

    protected fun opprettOpprinneligSøknadsdato(
        periode: Periode,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement =
            "INSERT INTO opprinnelig_soknadsdato VALUES (:vedtaksperiode_id, now())"
        requireNotNull(
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "vedtaksperiode_id" to periode.id,
                    )
                ).asUpdate
            )
        )
    }

    protected fun opprettVedtak(
        personId: Long,
        arbeidsgiverId: Long,
        periode: Periode = PERIODE,
    ) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val snapshotid = opprettSnapshot()
            opprettGenerasjon(periode)
            opprettOpprinneligSøknadsdato(periode)

            @Language("PostgreSQL")
            val statement =
                "INSERT INTO vedtak(vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, snapshot_ref, forkastet) VALUES(?, ?, ?, ?, ?, ?, ?)"
            requireNotNull(
                session.run(
                    queryOf(
                        statement, periode.id, periode.fom, periode.tom, arbeidsgiverId, personId, snapshotid, false
                    ).asUpdateAndReturnGeneratedKey
                )
            )
        }

    protected fun opprettVarseldefinisjon(tittel: String = "EN_TITTEL", kode: String = "EN_KODE", definisjonId: UUID = UUID.randomUUID()): Long = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, opprettet) 
            VALUES (?, ?, ?, ?, ?, ?)    
        """
        requireNotNull(session.run(queryOf(query, definisjonId, kode, tittel, null, null, LocalDateTime.now()).asUpdateAndReturnGeneratedKey))
    }

    protected fun nyGenerasjon(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        generasjonId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        tilstandEndretTidspunkt: LocalDateTime? = null
    ): Long = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_vedtaksperiode_generasjon(vedtaksperiode_id, unik_id, utbetaling_id, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, tilstand) 
            VALUES (?, ?, ?, ?, ?, ?, 'Ulåst')
        """
        return requireNotNull(session.run(queryOf(query, vedtaksperiodeId, generasjonId, utbetalingId, UUID.randomUUID(), tilstandEndretTidspunkt, UUID.randomUUID()).asUpdateAndReturnGeneratedKey))
    }

    protected fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kode: String = "EN_KODE",
        generasjonRef: Long,
        definisjonRef: Long? = null,
    ) = nyttVarsel(id, vedtaksperiodeId, kode, generasjonRef, definisjonRef, "AKTIV", null)

    protected fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
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
                LocalDateTime.now(),
                status,
                if (endretTidspunkt != null) "EN_IDENT" else null,
                endretTidspunkt
            ).asExecute
        )
    }

    protected fun klargjørVedtak(
        vedtakId: Long,
        utbetlingId: UUID? = null,
        periode: Periode,
        oppgavetype: Oppgavetype,
    ) {
        opprettSaksbehandleroppgavetype(Periodetype.FØRSTEGANGSBEHANDLING, Inntektskilde.EN_ARBEIDSGIVER, vedtakId)
        val hendelseId = UUID.randomUUID()
        opprettHendelse(hendelseId)
        opprettAutomatisering(false, vedtaksperiodeId = periode.id, hendelseId = hendelseId)
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, oppgavetype, vedtakId, utbetlingId)
    }

    private fun opprettSaksbehandleroppgavetype(type: Periodetype, inntektskilde: Inntektskilde, vedtakRef: Long) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO saksbehandleroppgavetype(type, vedtak_ref, inntektskilde) VALUES (?, ?, ?)"
            session.run(queryOf(statement, type.toString(), vedtakRef, inntektskilde.toString()).asUpdate)
        }

    protected fun ferdigstillOppgave(vedtakRef: Long) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
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
    ) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
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
                ).asUpdateAndReturnGeneratedKey
            )
        }

    protected fun opprettKommentar(
        tekst: String = "En kommentar",
        notatRef: Int,
        saksbehandlerIdent: String = SAKSBEHANDLER.ident,
    ) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO kommentarer(tekst, notat_ref, saksbehandlerident) VALUES (?, ?, ?)"
            session.run(
                queryOf(
                    statement,
                    tekst,
                    notatRef,
                    saksbehandlerIdent,
                ).asUpdateAndReturnGeneratedKey
            )
        }

    protected fun vedtakId(vedtaksperiodeId: UUID = PERIODE.id) = sessionOf(dataSource).use { session ->
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
    ) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val personinfoid = opprettPersoninfo(adressebeskyttelse)
            val infotrygdutbetalingerid = opprettInfotrygdutbetalinger()

            @Language("PostgreSQL")
            val statement =
                "INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref) VALUES(?, ?, ?, ?, ?)"
            requireNotNull(
                session.run(
                    queryOf(
                        statement,
                        fødselsnummer.toLong(),
                        aktørId.toLong(),
                        personinfoid,
                        bostedId,
                        infotrygdutbetalingerid
                    ).asUpdateAndReturnGeneratedKey
                )
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
                        adressebeskyttelse.name
                    ).asUpdateAndReturnGeneratedKey
                )
            )
        }

    protected fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGANISASJONSNUMMER,
        bransjer: List<String> = emptyList(),
    ) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
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
                        bransjeid
                    ).asUpdateAndReturnGeneratedKey
                )
            )
        }

    private fun finnArbeidsgiverId(): Int =
        requireNotNull(sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "SELECT id FROM arbeidsgiver WHERE orgnummer = ?"
            session.run(queryOf(statement, ORGANISASJONSNUMMER.toLong()).map { it.int("id") }.asSingle)
        })

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

    protected fun opprettArbeidsforhold(personid: Long, arbeidsgiverid: Long) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
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
                    ).asUpdateAndReturnGeneratedKey
                )
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
                        objectMapper.writeValueAsString(bransjer)
                    ).asUpdateAndReturnGeneratedKey
                )
            )
        }

    private fun opprettArbeidsgivernavn() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO arbeidsgiver_navn(navn) VALUES(?)"
        requireNotNull(session.run(queryOf(statement, ARBEIDSGIVER_NAVN).asUpdateAndReturnGeneratedKey))
    }

    private fun opprettInfotrygdutbetalinger() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO infotrygdutbetalinger(data) VALUES('[]')"
        requireNotNull(session.run(queryOf(statement).asUpdateAndReturnGeneratedKey))
    }

    private fun opprettSnapshot() = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO snapshot(data, versjon) VALUES(?::json, ?)"
        requireNotNull(
            session.run(
                queryOf(
                    statement,
                    objectMapper.writeValueAsString(snapshot()),
                    1
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    protected fun opprettOppgave(
        status: Oppgavestatus = Oppgavestatus.AvventerSaksbehandler,
        oppgavetype: Oppgavetype = Oppgavetype.SØKNAD,
        vedtakRef: Long,
        utbetlingId: UUID? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) =
        requireNotNull(
            sessionOf(dataSource, returnGeneratedKey = true).use { session ->
                @Language("PostgreSQL")
                val statement =
                    "INSERT INTO oppgave(utbetaling_id, opprettet, oppdatert, status, vedtak_ref, type) VALUES(?, ?, now(), CAST(? as oppgavestatus), ?, CAST(? as oppgavetype))"
                session.run(
                    queryOf(
                        statement,
                        utbetlingId,
                        opprettet,
                        status.name,
                        vedtakRef,
                        oppgavetype.name,
                    ).asUpdateAndReturnGeneratedKey
                )
            })

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
                mapOf("hendelseId" to hendelseId, "foedselsnummer" to foedselsnummer.toLong())
            ).asUpdate
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
                    "utbetalingId" to utbetalingId
                )
            ).asUpdate
        )
    }

    protected fun tildelOppgave(oppgaveRef: Long, saksbehandlerOid: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                "INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref) VALUES(?, ?)"
            session.run(
                queryOf(
                    statement,
                    saksbehandlerOid,
                    oppgaveRef,
                ).asUpdate
            )
        }

    protected fun mockSnapshot(fødselsnummer: String = FØDSELSNUMMER, avviksprosent: Double = 0.0, arbeidsgivere: List<GraphQLArbeidsgiver> = emptyList()) {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns object :
            GraphQLClientResponse<HentSnapshot.Result> {
            override val data = HentSnapshot.Result(snapshot(fødselsnummer, avviksprosent, arbeidsgivere))
        }
    }

    private fun snapshot(fødselsnummer: String = FØDSELSNUMMER, avviksprosent: Double = 0.0, arbeidsgivere: List<GraphQLArbeidsgiver> = emptyList()): GraphQLPerson {
        val vilkårsgrunnlag = GraphQLSpleisVilkarsgrunnlag(
            id = UUID.randomUUID().toString(),
            vilkarsgrunnlagtype = GraphQLVilkarsgrunnlagtype.SPLEIS,
            inntekter = emptyList(),
            omregnetArsinntekt = 1_000_000.0,
            sammenligningsgrunnlag = 1_000_000.0,
            skjaeringstidspunkt = "2020-01-01",
            sykepengegrunnlag = 1_000_000.0,
            antallOpptjeningsdagerErMinst = 123,
            avviksprosent = avviksprosent,
            grunnbelop = 100_000,
            sykepengegrunnlagsgrense = GraphQLSykepengegrunnlagsgrense(
                grunnbelop = 100_000,
                grense = 600_000,
                virkningstidspunkt = "2020-01-01",
            ),
            oppfyllerKravOmMedlemskap = true,
            oppfyllerKravOmMinstelonn = true,
            oppfyllerKravOmOpptjening = true,
            opptjeningFra = "2000-01-01",
            arbeidsgiverrefusjoner = listOf(
                GraphQLArbeidsgiverrefusjon(
                    arbeidsgiver = ORGANISASJONSNUMMER,
                    refusjonsopplysninger = listOf(
                        GraphQLRefusjonselement(
                            fom = "2020-01-01",
                            tom = null,
                            belop = 30000.0,
                            meldingsreferanseId = UUID.randomUUID().toString()
                        )
                    )
                )
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
            generasjoner = generasjoner
        )

    protected fun opprettSnapshotGenerasjon(perioder: List<GraphQLTidslinjeperiode>, id: UUID = UUID.randomUUID()) =
        GraphQLGenerasjon(id = id.toString(), perioder = perioder)

    protected fun opprettBeregnetPeriode(
        fom: String = LocalDate.now().toString(),
        tom: String = LocalDate.now().toString(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
    ) = GraphQLBeregnetPeriode(
        erForkastet = false,
        fom = fom,
        tom = tom,
        inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now().toString(),
        periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
        periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
        skjaeringstidspunkt = LocalDate.now().toString(),
        tidslinje = emptyList(),
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        id = UUID.randomUUID().toString(),
        aktivitetslogg = emptyList(),
        beregningId = UUID.randomUUID().toString(),
        forbrukteSykedager = null,
        gjenstaendeSykedager = null,
        hendelser = emptyList(),
        maksdato = LocalDate.now().toString(),
        vilkarsgrunnlagId = null,
        periodevilkar = GraphQLPeriodevilkar(
            alder = Alder(55, true),
            sykepengedager = Sykepengedager(
                maksdato = LocalDate.now().toString(),
                oppfylt = true,
                skjaeringstidspunkt = LocalDate.now().toString(),
            )
        ),
        utbetaling = GraphQLUtbetaling(
            id = utbetalingId.toString(),
            arbeidsgiverFagsystemId = "EN_FAGSYSTEM_ID",
            arbeidsgiverNettoBelop = 1,
            personFagsystemId = "EN_FAGSYSTEM_ID",
            personNettoBelop = 0,
            statusEnum = GraphQLUtbetalingstatus.IKKEGODKJENT,
            typeEnum = Utbetalingtype.UTBETALING,
        ),
    )

    protected fun opprettUberegnetPeriode(
        fom: String = LocalDate.now().toString(),
        tom: String = LocalDate.now().toString(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
    ) = GraphQLUberegnetPeriode(
        erForkastet = false,
        fom = fom,
        tom = tom,
        inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now().toString(),
        periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
        periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
        skjaeringstidspunkt = LocalDate.now().toString(),
        tidslinje = emptyList(),
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        id = UUID.randomUUID().toString(),
        hendelser = emptyList(),
    )

    protected fun finnOppgaveIdFor(vedtaksperiodeId: UUID): Long = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT o.id FROM oppgave o JOIN vedtak v ON v.id = o.vedtak_ref WHERE v.vedtaksperiode_id = :vedtaksperiode_id;"
        return requireNotNull(session.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).map { it.long("id") }.asSingle))
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
}
