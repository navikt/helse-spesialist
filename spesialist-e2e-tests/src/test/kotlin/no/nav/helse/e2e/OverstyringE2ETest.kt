package no.nav.helse.e2e

import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.TestRapidHelpers.siste
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiDagoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektoverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiLovhjemmel
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringDag
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Invalidert
import no.nav.helse.spesialist.api.person.PersonService
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.domain.tilgangskontroll.SaksbehandlerTilganger
import no.nav.helse.util.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class OverstyringE2ETest : AbstractE2ETest() {
    @Test
    fun `saksbehandler overstyrer sykdomstidslinje`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterOverstyrTidslinje(
            dager =
                listOf(
                    ApiOverstyringDag(
                        dato = 20.januar,
                        type = "Feriedag",
                        fraType = "Sykedag",
                        grad = null,
                        fraGrad = 100,
                        lovhjemmel = null,
                    ),
                ),
        )
        assertOverstyrTidslinje(FØDSELSNUMMER, 1)

        assertSaksbehandleroppgave(oppgavestatus = Invalidert)

        val nyUtbetalingId = UUID.randomUUID()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = nyUtbetalingId),
        )
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `saksbehandler overstyrer sykdomstidslinje med referanse til lovhjemmel`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterOverstyrTidslinje(
            dager =
                listOf(
                    ApiOverstyringDag(
                        dato = 20.januar,
                        type = "Feriedag",
                        fraType = "Sykedag",
                        grad = null,
                        fraGrad = 100,
                        lovhjemmel =
                            ApiLovhjemmel(
                                paragraf = "EN PARAGRAF",
                                ledd = "ET LEDD",
                                bokstav = "EN BOKSTAV",
                                lovverk = "folketrygdloven",
                                lovverksversjon = "1970-01-01",
                            ),
                    ),
                ),
        )
        assertOverstyrTidslinje(FØDSELSNUMMER, 1)
        val subsumsjon = inspektør.siste("subsumsjon").path("subsumsjon")

        assertNotNull(subsumsjon["sporing"]["overstyrtidslinje"])
    }

    @Test
    fun `saksbehandler overstyrer inntekt og refusjon`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterOverstyrInntektOgRefusjon(
            arbeidsgivere =
                listOf(
                    ApiOverstyringArbeidsgiver(
                        organisasjonsnummer = ORGNR,
                        manedligInntekt = 25000.0,
                        fraManedligInntekt = 25001.0,
                        forklaring = "testbortforklaring",
                        lovhjemmel = ApiLovhjemmel("8-28", "LEDD_1", "BOKSTAV_A", "folketrygdloven", "1970-01-01"),
                        refusjonsopplysninger = null,
                        fraRefusjonsopplysninger = null,
                        begrunnelse = "begrunnelse",
                        fom = null,
                        tom = null,
                    ),
                ),
            skjæringstidspunkt = 1.januar,
        )

        assertOverstyrInntektOgRefusjon(FØDSELSNUMMER, 1)
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)

        val nyUtbetalingId = UUID.randomUUID()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = nyUtbetalingId),
        )

        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
        assertTildeling(SAKSBEHANDLER_EPOST, nyUtbetalingId)
    }

    @Test
    fun `saksbehandler overstyrer arbeidsforhold`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterOverstyrArbeidsforhold()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
        assertOverstyrArbeidsforhold(FØDSELSNUMMER, 1)

        val nyUtbetalingId = UUID.randomUUID()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = nyUtbetalingId),
        )

        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
        assertTildeling(SAKSBEHANDLER_EPOST, nyUtbetalingId)
    }

    private val saksbehandlertilgangerIngenTilganger =
        SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = UUID.randomUUID(),
            beslutterSaksbehandlergruppe = UUID.randomUUID(),
            skjermedePersonerSaksbehandlergruppe = UUID.randomUUID(),
        )

    @Test
    fun `legger ved overstyringer i speil snapshot`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterOverstyrTidslinje()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        håndterOverstyrInntektOgRefusjon()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        håndterOverstyrArbeidsforhold()

        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER) } returns
            SaksbehandlerFraApi(
                oid = UUID.randomUUID(),
                navn = "epost",
                epost = "navn",
                ident = "A123456",
                grupper = emptyList(),
                tilgangsgrupper = emptySet()
            )
        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerTilganger>(TILGANGER) } returns saksbehandlertilgangerIngenTilganger
        val nyUtbetalingId = UUID.randomUUID()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = nyUtbetalingId),
        )
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)

        mockSnapshot()

        val snapshot: ApiPerson = runBlocking { personQuery.person(FØDSELSNUMMER, null, dataFetchingEnvironment).data!! }

        assertNotNull(snapshot)
        val overstyringer = snapshot.arbeidsgivere().first().overstyringer()
        assertEquals(3, overstyringer.size)
        assertEquals(1, (overstyringer[0] as ApiDagoverstyring).dager.size)
        assertEquals(25000.0, (overstyringer[1] as ApiInntektoverstyring).inntekt.manedligInntekt)
        assertEquals(true, (overstyringer[2] as ApiArbeidsforholdoverstyring).deaktivert)
        assertFalse(overstyringer.first().ferdigstilt)
        assertFalse(overstyringer[1].ferdigstilt)
        assertFalse(overstyringer.last().ferdigstilt)
    }

    private fun assertOverstyrTidslinje(
        fødselsnummer: String,
        @Suppress("SameParameterValue") forventetAntall: Int,
    ) {
        @Language("PostgreSQL")
        val query =
            """
                SELECT COUNT(1) FROM overstyring o 
                INNER JOIN overstyring_tidslinje ot on o.id = ot.overstyring_ref 
                WHERE o.person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer)
            """
        val antallOverstyrTidslinje =
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf(
                        query,
                        mapOf(
                            "fodselsnummer" to fødselsnummer,
                        ),
                    ).map { it.int(1) }.asSingle,
                )
            } ?: 0

        assertEquals(forventetAntall, antallOverstyrTidslinje)
    }

    private fun assertOverstyrArbeidsforhold(
        fødselsnummer: String,
        @Suppress("SameParameterValue") forventetAntall: Int,
    ) {
        @Language("PostgreSQL")
        val query =
            """
                SELECT COUNT(1) FROM overstyring o
                INNER JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
                WHERE o.person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer)
            """
        val antallOverstyrArbeidsforhold =
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf(
                        query,
                        mapOf(
                            "fodselsnummer" to fødselsnummer,
                        ),
                    ).map { it.int(1) }.asSingle,
                )
            } ?: 0

        assertEquals(forventetAntall, antallOverstyrArbeidsforhold)
    }

    private fun assertOverstyrInntektOgRefusjon(
        fødselsnummer: String,
        @Suppress("SameParameterValue") forventetAntall: Int,
    ) {
        @Language("PostgreSQL")
        val query =
            """
                SELECT COUNT(1) FROM overstyring o
                INNER JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
                WHERE o.person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer)
            """
        val antallOverstyrInntektOgRefusjon =
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf(
                        query,
                        mapOf(
                            "fodselsnummer" to fødselsnummer,
                        ),
                    ).map { it.int(1) }.asSingle,
                )
            } ?: 0

        assertEquals(forventetAntall, antallOverstyrInntektOgRefusjon)
    }

    private fun assertTildeling(
        saksbehandlerEpost: String,
        utbetalingId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            """
                SELECT epost FROM saksbehandler s 
                INNER JOIN tildeling t on s.oid = t.saksbehandler_ref
                INNER JOIN oppgave o on o.id = t.oppgave_id_ref
                WHERE o.utbetaling_id = ?
            """
        val tildeltEpost =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, utbetalingId).map { it.string("epost") }.asSingle)
            }
        assertEquals(saksbehandlerEpost, tildeltEpost)
    }

    private val dataFetchingEnvironment = mockk<DataFetchingEnvironment>(relaxed = true)

    private val personQuery =
        PersonQuery(
            handler = PersonQueryHandler(
                personoppslagService = PersonService(
                    personApiDao = daos.personApiDao,
                    egenAnsattApiDao = daos.egenAnsattApiDao,
                    vergemålApiDao = daos.vergemålApiDao,
                    tildelingApiDao = daos.tildelingApiDao,
                    arbeidsgiverApiDao = daos.arbeidsgiverApiDao,
                    overstyringApiDao = daos.overstyringApiDao,
                    risikovurderingApiDao = daos.risikovurderingApiDao,
                    varselRepository = daos.varselApiRepository,
                    oppgaveApiDao = daos.oppgaveApiDao,
                    periodehistorikkApiDao = daos.periodehistorikkApiDao,
                    notatDao = daos.notatApiDao,
                    påVentApiDao = daos.påVentApiDao,
                    apiOppgaveService = mockk(relaxed = true),
                    saksbehandlerMediator = mockk(relaxed = true),
                    stansAutomatiskBehandlinghåndterer = mockk(relaxed = true),
                    personhåndterer = object : Personhåndterer {
                        override fun oppdaterPersondata(fødselsnummer: String) {}
                        override fun klargjørPersonForVisning(fødselsnummer: String) {}
                    },
                    snapshotService = SnapshotService(daos.personinfoDao, snapshothenter),
                    reservasjonshenter = mockk(relaxed = true),
                    sessionFactory = sessionFactory,
                    vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao,
                    stansAutomatiskBehandlingSaksbehandlerDao = daos.stansAutomatiskBehandlingSaksbehandlerDao,
                ),
            ),
        )
}
