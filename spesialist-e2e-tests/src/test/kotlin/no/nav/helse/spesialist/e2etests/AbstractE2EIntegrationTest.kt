package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import kotliquery.sessionOf
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.melding.VedtakFattetMelding
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.e2etests.Meldingsbygger.byggUtbetalingEndret
import no.nav.helse.spesialist.e2etests.behovløserstubs.AbstractBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.AvviksvurderingBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.HentInfotrygdutbetalingerBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.HentPersoninfoV2BehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.RisikovurderingBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.ÅpneOppgaverBehovLøser
import no.nav.helse.spesialist.e2etests.context.Arbeidsgiver
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.UUID

abstract class AbstractE2EIntegrationTest {
    protected val testContext: TestContext = TestContext()
    protected var saksbehandler = lagSaksbehandler()
    private var saksbehandlerTilgangsgrupper = mutableSetOf<Tilgangsgruppe>()
    protected var beslutter = lagSaksbehandler()

    protected fun saksbehandlerHarTilgang(tilgangsgruppe: Tilgangsgruppe) {
        saksbehandlerTilgangsgrupper += tilgangsgruppe
    }

    private val behovLøserStub =
        E2ETestApplikasjon.behovLøserStub.also {
            it.init(person = testContext.person, arbeidsgiver = testContext.arbeidsgiver)
        }
    private val spleisStub =
        E2ETestApplikasjon.spleisStub.also {
            it.init(testContext)
        }
    private val testRapid = E2ETestApplikasjon.testRapid

    protected val hentPersoninfoV2BehovLøser = finnLøserForDenneTesten<HentPersoninfoV2BehovLøser>()
    protected val risikovurderingBehovLøser = finnLøserForDenneTesten<RisikovurderingBehovLøser>()
    protected val åpneOppgaverBehovLøser = finnLøserForDenneTesten<ÅpneOppgaverBehovLøser>()
    protected val hentInfotrygdutbetalingerBehovLøser = finnLøserForDenneTesten<HentInfotrygdutbetalingerBehovLøser>()

    private inline fun <reified T : AbstractBehovLøser> finnLøserForDenneTesten() = behovLøserStub.finnLøser<T>(testContext.person.fødselsnummer)

    protected fun besvarBehovIgjen(behov: String) {
        behovLøserStub.besvarIgjen(testContext.person.fødselsnummer, behov)
    }

    protected fun fødselsnummer() = testContext.person.fødselsnummer

    protected fun organisasjonsnummer() = testContext.arbeidsgiver.organisasjonsnummer

    protected fun meldinger() = testRapid.meldingslogg(testContext.person.fødselsnummer)

    protected fun søknadOgGodkjenningbehovKommerInn(
        tags: List<String> = listOf("Innvilget"),
        tilleggsmeldinger: TilleggsmeldingReceiver.() -> Unit = {},
    ): Vedtaksperiode {
        personSenderSøknad()
        val vedtaksperiode = førsteVedtaksperiode()
        spleisForberederBehandling(vedtaksperiode, tilleggsmeldinger)
        spleisSenderGodkjenningsbehov(vedtaksperiode, tags = tags)
        return vedtaksperiode
    }

    protected fun personSenderSøknad() {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggSendSøknadNav(testContext.person, testContext.arbeidsgiver),
        )
    }

    protected fun spleisForberederBehandling(
        vedtaksperiode: Vedtaksperiode,
        tilleggsmeldinger: TilleggsmeldingReceiver.() -> Unit,
    ) {
        spleisOppretterBehandling(
            vedtaksperiode = vedtaksperiode,
            person = testContext.person,
            arbeidsgiver = testContext.arbeidsgiver,
        )
        spleisOppretterNyUtbetaling(
            vedtaksperiode = vedtaksperiode,
            person = testContext.person,
            arbeidsgiver = testContext.arbeidsgiver,
        )
        TilleggsmeldingReceiver(testRapid, testContext, vedtaksperiode).tilleggsmeldinger()
        utbetalingEndres(vedtaksperiode, testContext.person, testContext.arbeidsgiver)
    }

    protected fun spleisReberegnerAutomatisk(vedtaksperiode: Vedtaksperiode) {
        spleisStub.spleisReberegnerPerioden(testContext, vedtaksperiode)
    }

    protected fun spleisKasterUtSaken(vedtaksperiode: Vedtaksperiode) {
        spleisStub.spleisForkasterPerioden(testContext, vedtaksperiode)
    }

    protected fun detPubliseresEnGosysOppgaveEndretMelding() {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggGosysOppgaveEndret(testContext.person),
        )
    }

    protected fun detPubliseresEnAdressebeskyttelseEndretMelding() {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggAdressebeskyttelseEndret(testContext.person),
        )
    }

    protected fun varseldefinisjonOpprettes(varselkode: String) {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggVarselkodeNyDefinisjon(varselkode),
        )
    }

    protected fun assertGodkjenningsbehovBesvart(
        godkjent: Boolean,
        automatiskBehandlet: Boolean,
        vararg årsakerTilAvvist: String,
    ) {
        val løsning =
            testRapid
                .meldingslogg(testContext.person.fødselsnummer)
                .mapNotNull { it["@løsning"] }
                .mapNotNull { it["Godkjenning"] }
                .last()

        assertTrue(løsning["godkjent"].isBoolean)
        assertEquals(godkjent, løsning["godkjent"].booleanValue())
        assertEquals(automatiskBehandlet, løsning["automatiskBehandling"].booleanValue())
        assertNotNull(løsning["godkjenttidspunkt"].asLocalDateTime())
        if (årsakerTilAvvist.isNotEmpty()) {
            val begrunnelser = løsning["begrunnelser"].map { it.asText() }
            assertEquals(begrunnelser, begrunnelser.distinct())
            assertEquals(årsakerTilAvvist.toSet(), begrunnelser.toSet())
        }
    }

    protected fun assertVedtakFattetEtterHovedregel(utfall: VedtakFattetMelding.BegrunnelseType = VedtakFattetMelding.BegrunnelseType.Innvilgelse) {
        val vedtakFattet =
            testRapid
                .meldingslogg(testContext.person.fødselsnummer)
                .find { it["@event_name"].asText() == "vedtak_fattet" }
                ?: error("Forventet å finne vedtak_fattet i meldingslogg")

        assertEquals(1, vedtakFattet["begrunnelser"].size())
        assertEquals(utfall.name, vedtakFattet["begrunnelser"][0]["type"].asText())
        assertEquals("EtterHovedregel", vedtakFattet["sykepengegrunnlagsfakta"]["fastsatt"].asText())
    }

    protected fun assertVedtakFattetEtterSkjønn(begrunnelseFritekst: String = "skjønnsfastsetter tredje avsnitt") {
        val vedtakFattet =
            testRapid
                .meldingslogg(testContext.person.fødselsnummer)
                .find { it["@event_name"].asText() == "vedtak_fattet" }
                ?: error("Forventet å finne vedtak_fattet i meldingslogg")

        assertEquals(4, vedtakFattet["begrunnelser"].size())
        assertEquals("SkjønnsfastsattSykepengegrunnlagMal", vedtakFattet["begrunnelser"][0]["type"].asText())
        assertEquals("SkjønnsfastsattSykepengegrunnlagFritekst", vedtakFattet["begrunnelser"][1]["type"].asText())
        assertEquals(begrunnelseFritekst, vedtakFattet["begrunnelser"][1]["begrunnelse"].asText())
        assertEquals("SkjønnsfastsattSykepengegrunnlagKonklusjon", vedtakFattet["begrunnelser"][2]["type"].asText())
        assertEquals("Innvilgelse", vedtakFattet["begrunnelser"][3]["type"].asText())
        assertEquals("EtterSkjønn", vedtakFattet["sykepengegrunnlagsfakta"]["fastsatt"].asText())
    }

    protected fun assertOppgaveTildeltSaksbehandler() {
        val oppgaveEvent =
            testRapid
                .meldingslogg(testContext.person.fødselsnummer)
                .findLast { it["@event_name"].asText() in listOf("oppgave_oppdatert", "oppgave_opprettet") }
                ?: error("Forventet å finne oppgave_opprettet/oppdatert i meldingslogg")
        assertEquals(saksbehandler.id.value, oppgaveEvent["saksbehandler"].asUUID())
    }

    protected fun assertBehandlingTilstand(expectedTilstand: String) {
        val actualTilstand =
            sessionOf(E2ETestApplikasjon.dbModule.dataSource, strict = true).use { session ->
                session.run(
                    asSQL(
                        "SELECT tilstand FROM behandling WHERE vedtaksperiode_id = :vedtaksperiode_id",
                        "vedtaksperiode_id" to førsteVedtaksperiode().vedtaksperiodeId,
                    ).map { it.string("tilstand") }.asSingle,
                )
            }
        assertEquals(expectedTilstand, actualTilstand)
    }

    protected fun assertPeriodeForkastet(expectedForkastet: Boolean) {
        val actualForkastet =
            sessionOf(E2ETestApplikasjon.dbModule.dataSource, strict = true).use { session ->
                session.run(
                    asSQL(
                        "SELECT forkastet FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiode_id",
                        "vedtaksperiode_id" to førsteVedtaksperiode().vedtaksperiodeId,
                    ).map { it.boolean("forkastet") }.asSingle,
                )
            }
        assertEquals(expectedForkastet, actualForkastet)
    }

    protected fun assertGjeldendeOppgavestatus(expectedStatus: String) {
        val actualStatus =
            sessionOf(E2ETestApplikasjon.dbModule.dataSource, strict = true).use { session ->
                session.run(
                    asSQL(
                        """
                        SELECT o.status
                        FROM oppgave o, vedtak v
                        WHERE o.vedtak_ref = v.id
                        AND v.vedtaksperiode_id = :vedtaksperiode_id
                        """.trimIndent(),
                        "vedtaksperiode_id" to førsteVedtaksperiode().vedtaksperiodeId,
                    ).map { it.string("status") }.asSingle,
                )
            }
        assertEquals(expectedStatus, actualStatus)
    }

    protected fun assertSykepengegrunnlagfakta() {
        val vedtakFattet =
            testRapid
                .meldingslogg(testContext.person.fødselsnummer)
                .find { it["@event_name"].asText() == "vedtak_fattet" }
                ?: error("Forventet å finne vedtak_fattet i meldingslogg")

        assertEquals(
            AvviksvurderingBehovLøser.AVVIKSPROSENT,
            vedtakFattet["sykepengegrunnlagsfakta"]["avviksprosent"].asDouble(),
        )
        assertEquals(
            AvviksvurderingBehovLøser.SAMMENLIGNINGSGRUNNLAG_TOTALBELØP,
            vedtakFattet["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asDouble(),
        )
    }

    protected fun leggTilVedtaksperiode() {
        testContext.leggTilVedtaksperiode()
    }

    protected fun førsteVedtaksperiode() = testContext.vedtaksperioder.first()

    protected fun andreVedtaksperiode() = testContext.vedtaksperioder[1]

    protected fun saksbehandlerIdent() = saksbehandler.ident

    protected fun medPersonISpeil(
        saksbehandler: Saksbehandler = this.saksbehandler,
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe> = this.saksbehandlerTilgangsgrupper,
        block: SpeilPersonReceiver.() -> Unit,
    ) {
        spleisStub.stubSnapshotForPerson(testContext)
        SpeilPersonReceiver(
            testContext = testContext,
            saksbehandler = saksbehandler,
            tilgangsgrupper = saksbehandlerTilgangsgrupper,
        ).block()
    }

    protected fun beslutterMedPersonISpeil(block: SpeilPersonReceiver.() -> Unit) {
        spleisStub.stubSnapshotForPerson(testContext)
        SpeilPersonReceiver(
            testContext = testContext,
            saksbehandler = beslutter,
            tilgangsgrupper = setOf(Tilgangsgruppe.BESLUTTER),
        ).block()
    }

    private fun spleisOppretterBehandling(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
    ) {
        vedtaksperiode.spleisBehandlingId = UUID.randomUUID()
        testRapid.publish(
            person.fødselsnummer,
            Meldingsbygger.byggBehandlingOpprettet(vedtaksperiode, person, arbeidsgiver),
        )
    }

    private fun spleisOppretterNyUtbetaling(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
    ) {
        vedtaksperiode.utbetalingId = UUID.randomUUID()
        testRapid.publish(
            person.fødselsnummer,
            Meldingsbygger.byggVedtaksperiodeNyUtbetaling(vedtaksperiode, person, arbeidsgiver),
        )
    }

    private fun utbetalingEndres(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
    ) {
        testRapid.publish(
            person.fødselsnummer,
            byggUtbetalingEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
                forrigeStatus = "NY",
                gjeldendeStatus = "IKKE_UTBETALT",
            ),
        )
    }

    protected fun spleisSenderGodkjenningsbehov(
        vedtaksperiode: Vedtaksperiode,
        tags: List<String> = listOf("Innvilget"),
    ) {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggGodkjenningsbehov(
                person = testContext.person,
                arbeidsgiver = testContext.arbeidsgiver,
                vilkårsgrunnlagId = testContext.vilkårsgrunnlagId,
                vedtaksperiode = vedtaksperiode,
                tags = tags,
            ),
        )
    }

    protected fun skjermetInfoEndres(skjermet: Boolean) {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggEndretSkjermetinfo(testContext.person, skjermet),
        )
    }

    protected fun callHttpGet(
        relativeUrl: String,
        saksbehandler: Saksbehandler = this.saksbehandler,
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe> = this.saksbehandlerTilgangsgrupper,
    ) = REST.get(
        relativeUrl = relativeUrl,
        saksbehandler = saksbehandler,
        tilgangsgrupper = saksbehandlerTilgangsgrupper,
    )

    protected fun callHttpPost(
        relativeUrl: String,
        saksbehandler: Saksbehandler = this.saksbehandler,
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe> = this.saksbehandlerTilgangsgrupper,
        request: Any,
    ) = REST.post(
        relativeUrl = relativeUrl,
        saksbehandler = saksbehandler,
        tilgangsgrupper = saksbehandlerTilgangsgrupper,
        request = request,
    )
}
