package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandlerFraApi
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.e2etests.VårMeldingsbygger.byggUtbetalingEndret
import no.nav.helse.spesialist.e2etests.behovløserstubs.AbstractBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.RisikovurderingBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.ÅpneOppgaverBehovLøser
import no.nav.helse.spesialist.e2etests.context.Arbeidsgiver
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDateTime
import java.util.UUID

abstract class AbstractE2EIntegrationTest {
    private val testContext: TestContext = TestContext()
    protected val saksbehandler = lagSaksbehandlerFraApi()

    private val behovLøserStub = E2ETestApplikasjon.behovLøserStub.also {
        it.init(person = testContext.person, arbeidsgiver = testContext.arbeidsgiver)
    }
    private val spleisStub = E2ETestApplikasjon.spleisStub.also {
        it.init(testContext)
    }
    private val testRapid = E2ETestApplikasjon.testRapid

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
        testRapid.publish(VårMeldingsbygger.byggSendSøknadNav(testContext.person, testContext.arbeidsgiver))
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
        testRapid.publish(VårMeldingsbygger.byggGosysOppgaveEndret(testContext.person))
    }

    protected fun lagreVarseldefinisjon(varselkode: String) {
        E2ETestApplikasjon.dbModule.daos.definisjonDao.lagreDefinisjon(
            unikId = UUID.nameUUIDFromBytes(varselkode.toByteArray()),
            kode = varselkode,
            tittel = "En tittel for varselkode=$varselkode",
            forklaring = "En forklaring for varselkode=$varselkode",
            handling = "En handling for varselkode=$varselkode",
            avviklet = false,
            opprettet = LocalDateTime.now(),
        )
    }

    data class Varsel(
        val kode: String,
        val status: String,
    )

    protected fun hentVarselkoder(vedtaksperiode: Vedtaksperiode): Set<Varsel> =
        sessionOf(E2ETestApplikasjon.dbModule.dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT kode, status FROM selve_varsel WHERE vedtaksperiode_id = :vedtaksperiode_id"
            val paramMap = mapOf("vedtaksperiode_id" to vedtaksperiode.vedtaksperiodeId)
            session.list(queryOf(query, paramMap)) { Varsel(it.string("kode"), it.string("status")) }.toSet()
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
        testRapid.publish(VårMeldingsbygger.byggBehandlingOpprettet(vedtaksperiode, person, arbeidsgiver))
    }

    private fun spleisOppretterNyUtbetaling(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) {
        vedtaksperiode.utbetalingId = UUID.randomUUID()
        testRapid.publish(VårMeldingsbygger.byggVedtaksperiodeNyUtbetaling(vedtaksperiode, person, arbeidsgiver))
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
            VårMeldingsbygger.byggGodkjenningsbehov(
                person = testContext.person,
                arbeidsgiver = testContext.arbeidsgiver,
                vedtaksperiode = vedtaksperiode
            )
        )
    }

}
