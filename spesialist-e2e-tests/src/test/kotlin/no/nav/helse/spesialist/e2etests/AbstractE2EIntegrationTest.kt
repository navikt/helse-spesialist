package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandlerFraApi
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.e2etests.Meldingsbygger.byggUtbetalingEndret
import no.nav.helse.spesialist.e2etests.behovløserstubs.AbstractBehovLøser
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
    private val testContext: TestContext = TestContext()
    private var saksbehandler = lagSaksbehandlerFraApi()

    enum class Tilgangstype {
        KODE7
    }

    protected fun saksbehandlerHarTilgang(tilgangstype: Tilgangstype) {
        saksbehandler = saksbehandler.copy(
            grupper = saksbehandler.grupper.plus(
                when (tilgangstype) {
                    Tilgangstype.KODE7 -> E2ETestApplikasjon.TilgangsgrupperForTest.kode7GruppeId
                }
            )
        )
    }

    private val behovLøserStub = E2ETestApplikasjon.behovLøserStub.also {
        it.init(person = testContext.person, arbeidsgiver = testContext.arbeidsgiver)
    }
    private val spleisStub = E2ETestApplikasjon.spleisStub.also {
        it.init(testContext)
    }
    private val testRapid = E2ETestApplikasjon.testRapid

    protected val hentPersoninfoV2BehovLøser = finnLøserForDenneTesten<HentPersoninfoV2BehovLøser>()
    protected val risikovurderingBehovLøser = finnLøserForDenneTesten<RisikovurderingBehovLøser>()
    protected val åpneOppgaverBehovLøser = finnLøserForDenneTesten<ÅpneOppgaverBehovLøser>()

    private inline fun <reified T : AbstractBehovLøser> finnLøserForDenneTesten() =
        behovLøserStub.finnLøser<T>(testContext.person.fødselsnummer)

    protected fun besvarBehovIgjen(behov: String) {
        behovLøserStub.besvarIgjen(testContext.person.fødselsnummer, behov)
    }

    protected fun søknadOgGodkjenningbehovKommerInn(tilleggsmeldinger: TilleggsmeldingReceiver.() -> Unit = {}) {
        personSenderSøknad()
        val vedtaksperiode = førsteVedtaksperiode()
        spleisForberederBehandling(vedtaksperiode, tilleggsmeldinger)
        spleisSenderGodkjenningsbehov(vedtaksperiode)
    }

    protected fun personSenderSøknad() {
        testRapid.publish(Meldingsbygger.byggSendSøknadNav(testContext.person, testContext.arbeidsgiver))
    }

    protected fun spleisForberederBehandling(
        vedtaksperiode: Vedtaksperiode,
        tilleggsmeldinger: TilleggsmeldingReceiver.() -> Unit
    ) {
        spleisOppretterBehandling(
            vedtaksperiode = vedtaksperiode,
            person = testContext.person,
            arbeidsgiver = testContext.arbeidsgiver
        )
        spleisOppretterNyUtbetaling(
            vedtaksperiode = vedtaksperiode,
            person = testContext.person,
            arbeidsgiver = testContext.arbeidsgiver
        )
        TilleggsmeldingReceiver(testRapid, testContext, vedtaksperiode).tilleggsmeldinger()
        utbetalingEndres(vedtaksperiode, testContext.person, testContext.arbeidsgiver)
    }

    protected fun detPubliseresEnGosysOppgaveEndretMelding() {
        testRapid.publish(Meldingsbygger.byggGosysOppgaveEndret(testContext.person))
    }

    protected fun detPubliseresEnAdressebeskyttelseEndretMelding() {
        testRapid.publish(Meldingsbygger.byggAdressebeskyttelseEndret(testContext.person))
    }

    protected fun varseldefinisjonOpprettes(varselkode: String) {
        testRapid.publish(Meldingsbygger.byggVarselkodeNyDefinisjon(varselkode))
    }

    protected fun assertGodkjenningsbehovBesvart(
        godkjent: Boolean,
        automatiskBehandlet: Boolean,
        vararg årsakerTilAvvist: String,
    ) {
        val løsning = testRapid.meldingslogg.get()
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

    protected fun assertBehandlingTilstand(expectedTilstand: String) {
        val actualTilstand = sessionOf(E2ETestApplikasjon.dbModule.dataSource, strict = true).use { session ->
            session.run(
                asSQL(
                    "SELECT tilstand FROM behandling WHERE vedtaksperiode_id = :vedtaksperiode_id",
                    "vedtaksperiode_id" to førsteVedtaksperiode().vedtaksperiodeId,
                ).map { it.string("tilstand") }.asSingle
            )
        }
        assertEquals(expectedTilstand, actualTilstand)
    }

    protected fun leggTilVedtaksperiode() {
        testContext.leggTilVedtaksperiode()
    }

    protected fun førsteVedtaksperiode() = testContext.vedtaksperioder.first()

    protected fun andreVedtaksperiode() = testContext.vedtaksperioder[1]

    protected fun medPersonISpeil(block: SpeilPersonReceiver.() -> Unit) {
        spleisStub.stubSnapshotForPerson(testContext)
        SpeilPersonReceiver(
            aktørId = testContext.person.aktørId,
            saksbehandlerIdent = saksbehandler.ident,
            bearerAuthToken = E2ETestApplikasjon.apiModuleIntegrationTestFixture.token(saksbehandler)
        ).block()
    }

    private fun spleisOppretterBehandling(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) {
        vedtaksperiode.spleisBehandlingId = UUID.randomUUID()
        testRapid.publish(Meldingsbygger.byggBehandlingOpprettet(vedtaksperiode, person, arbeidsgiver))
    }

    private fun spleisOppretterNyUtbetaling(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) {
        vedtaksperiode.utbetalingId = UUID.randomUUID()
        testRapid.publish(Meldingsbygger.byggVedtaksperiodeNyUtbetaling(vedtaksperiode, person, arbeidsgiver))
    }

    private fun utbetalingEndres(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) {
        testRapid.publish(
            byggUtbetalingEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
                forrigeStatus = "NY",
                gjeldendeStatus = "IKKE_UTBETALT"
            )
        )
    }

    protected fun spleisSenderGodkjenningsbehov(vedtaksperiode: Vedtaksperiode) {
        testRapid.publish(
            Meldingsbygger.byggGodkjenningsbehov(
                person = testContext.person,
                arbeidsgiver = testContext.arbeidsgiver,
                vilkårsgrunnlagId = testContext.vilkårsgrunnlagId,
                vedtaksperiode = vedtaksperiode
            )
        )
    }

}
