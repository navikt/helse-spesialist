package no.nav.helse.spesialist.api

import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.db.AbstractDatabaseTest
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.HentSnapshot
import no.nav.helse.spesialist.api.graphql.enums.GraphQLVilkarsgrunnlagtype
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLVilkarsgrunnlaghistorikk
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatType
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
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDao
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao
import org.intellij.lang.annotations.Language

internal abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
    protected companion object {
        val NAVN = Triple("Ola", "Kari", "Nordhen")
        val ENHET = Pair(101, "Halden")
        val PERIODE = Triple(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val ARBEIDSFORHOLD = Quadruple(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2), "EN TITTEL", 100)

        const val FØDSELSNUMMER = "01017011111"
        const val AKTØRID = "01017011111111"
        const val ARBEIDSGIVER_NAVN = "EN ARBEIDSGIVER"
        const val ORGANISASJONSNUMMER = "987654321"

        val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
        const val SAKSBEHANDLER_NAVN = "Jan Banan"
        const val SAKSBEHANDLER_EPOST = "jan.banan@nav.no"
        const val SAKSBEHANDLER_IDENT = "B123456"
    }

    protected val varselDao = VarselDao(dataSource)
    protected val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    protected val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    protected val saksbehandlerDao = SaksbehandlerDao(dataSource)
    protected val notatDao = NotatDao(dataSource)
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

    protected fun opprettVedtaksperiode(adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val (id, fom, tom) = PERIODE
            val personid = opprettPerson(adressebeskyttelse)
            val arbeidsgiverid = opprettArbeidsgiver()
            val snapshotid = opprettSnapshot()

            @Language("PostgreSQL")
            val statement =
                "INSERT INTO vedtak(vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, snapshot_ref) VALUES(?, ?, ?, ?, ?, ?)"
            session.run(
                queryOf(
                    statement, id, fom, tom, arbeidsgiverid, personid, snapshotid
                ).asUpdateAndReturnGeneratedKey
            )?.also {
                opprettArbeidsforhold(personid, arbeidsgiverid)
                opprettSaksbehandleroppgavetype(Periodetype.FØRSTEGANGSBEHANDLING, Inntektskilde.EN_ARBEIDSGIVER, it)
                opprettOppgave(Oppgavestatus.AvventerSaksbehandler, Oppgavetype.SØKNAD, it)
            }
        }

    protected fun ferdigstillOppgave(vedtakRef: Long) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                UPDATE oppgave SET ferdigstilt_av = ?, ferdigstilt_av_oid = ?, status = 'Ferdigstilt', oppdatert = now()
                WHERE oppgave.vedtak_ref = ?
            """.trimIndent()
            session.run(queryOf(statement, SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, vedtakRef).asUpdate)
        }
    }

    private fun opprettSaksbehandleroppgavetype(type: Periodetype, inntektskilde: Inntektskilde, vedtakRef: Long) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO saksbehandleroppgavetype(type, vedtak_ref, inntektskilde) VALUES (?, ?, ?)"
            session.run(queryOf(statement, type.toString(), vedtakRef, inntektskilde.toString()).asUpdate)
        }

    protected fun opprettNotat(
        tekst: String = "Et notat",
        saksbehandlerOid: UUID = SAKSBEHANDLER_OID,
        vedtaksperiodeId: UUID = PERIODE.first
    ) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO notat(tekst, saksbehandler_oid, vedtaksperiode_id, type) VALUES (?, ?, ?, CAST(? as notattype))"
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
        saksbehandlerIdent: String = SAKSBEHANDLER_IDENT,
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

    protected fun vedtakId(vedtaksperiodeId: UUID = PERIODE.first) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
        requireNotNull(session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)) {
            "Finner ikke vedtak i db for vedtaksperiodeId=$vedtaksperiodeId"
        }
    }

    protected fun opprettPerson(adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert) =
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
                        FØDSELSNUMMER.toLong(),
                        AKTØRID.toLong(),
                        infotrygdutbetalingerid,
                        ENHET.first,
                        personinfoid
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

    protected fun opprettArbeidsgiver(bransjer: List<String> = emptyList()) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val bransjeid = opprettBransjer(bransjer)
            val navnid = opprettArbeidsgivernavn()

            @Language("PostgreSQL")
            val statement = "INSERT INTO arbeidsgiver(orgnummer, navn_ref, bransjer_ref) VALUES(?, ?, ?)"
            requireNotNull(
                session.run(
                    queryOf(
                        statement,
                        ORGANISASJONSNUMMER.toLong(),
                        navnid,
                        bransjeid
                    ).asUpdateAndReturnGeneratedKey
                )
            )
        }

    protected fun opprettSaksbehandler(
        oid: UUID = SAKSBEHANDLER_OID,
        navn: String = SAKSBEHANDLER_NAVN,
        epost: String = SAKSBEHANDLER_EPOST,
        ident: String = SAKSBEHANDLER_IDENT
    ): UUID {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES (?, ?, ?, ?)"
            session.run(queryOf(statement, oid, navn, epost, ident).asUpdate)
        }
        return oid
    }

    private fun opprettArbeidsforhold(personid: Long, arbeidsgiverid: Long) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val (startdato, sluttdato, tittel, prosent) = ARBEIDSFORHOLD
            @Language("PostgreSQL")
            val statement =
                "INSERT INTO arbeidsforhold(person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent) VALUES(?, ?, ?, ?, ?, ?)"
            requireNotNull(
                session.run(
                    queryOf(
                        statement,
                        personid,
                        arbeidsgiverid,
                        startdato,
                        sluttdato,
                        tittel,
                        prosent
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
        erBeslutter: Boolean = false
    ) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val statement =
                "INSERT INTO oppgave(oppdatert, status, vedtak_ref, type, er_beslutteroppgave) VALUES(now(), CAST(? as oppgavestatus), ?, CAST(? as oppgavetype), ?)"
            session.run(
                queryOf(
                    statement,
                    status.name,
                    vedtakRef,
                    oppgavetype.name,
                    erBeslutter,
                ).asUpdateAndReturnGeneratedKey
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

    protected fun mockSnapshot(fødselsnummer: String = FØDSELSNUMMER, avviksprosent: Double = 0.0) {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns object :
            GraphQLClientResponse<HentSnapshot.Result> {
            override val data = HentSnapshot.Result(snapshot(fødselsnummer, avviksprosent))
        }
    }

    private fun snapshot(fødselsnummer: String = FØDSELSNUMMER, avviksprosent: Double = 0.0) = GraphQLPerson(
        aktorId = AKTØRID,
        arbeidsgivere = emptyList(),
        dodsdato = null,
        fodselsnummer = fødselsnummer,
        versjon = 1,
        vilkarsgrunnlaghistorikk = listOf(
            GraphQLVilkarsgrunnlaghistorikk(
                id = "en-id",
                grunnlag = listOf(
                    GraphQLSpleisVilkarsgrunnlag(
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
                    )
                )
            )
        )
    )

    protected class Quadruple<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D) {
        operator fun component1() = first
        operator fun component2() = second
        operator fun component3() = third
        operator fun component4() = fourth

        override fun toString() = "($first, $second, $third, $fourth)"
    }
}
