package no.nav.helse.spesialist.bootstrap

import no.nav.helse.spesialist.bootstrap.Meldingsbygger.byggUtbetalingEndret
import no.nav.helse.spesialist.bootstrap.behovløserstubs.AbstractBehovLøser
import no.nav.helse.spesialist.bootstrap.behovløserstubs.BehovLøserStub
import no.nav.helse.spesialist.bootstrap.behovløserstubs.HentInfotrygdutbetalingerBehovLøser
import no.nav.helse.spesialist.bootstrap.behovløserstubs.HentPersoninfoV2BehovLøser
import no.nav.helse.spesialist.bootstrap.behovløserstubs.RisikovurderingBehovLøser
import no.nav.helse.spesialist.bootstrap.behovløserstubs.ÅpneOppgaverBehovLøser
import no.nav.helse.spesialist.bootstrap.context.Arbeidsgiver
import no.nav.helse.spesialist.bootstrap.context.Person
import no.nav.helse.spesialist.bootstrap.context.TestContext
import no.nav.helse.spesialist.bootstrap.context.Vedtaksperiode
import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.util.UUID

class Scenario(
    spleisStub: SpleisStub,
    behovLøserStub: BehovLøserStub,
    private val testRapid: LoopbackTestRapid,
    private val dbModule: DBModule,
) {
    internal val testContext: TestContext = TestContext()
    internal var saksbehandler = lagSaksbehandler()
    private val brukerroller = mutableSetOf<Brukerrolle>()
    private val tilganger = mutableSetOf(Tilgang.Skriv)

    internal var beslutter = lagSaksbehandler()

    internal fun saksbehandlerHarRolle(brukerrolle: Brukerrolle) {
        brukerroller.add(brukerrolle)
    }

    private val behovLøserStub =
        behovLøserStub.also {
            it.init(person = testContext.person, arbeidsgiver = testContext.arbeidsgiver)
        }
    private val spleisStub =
        spleisStub.also {
            it.init(testContext)
        }

    internal val hentPersoninfoV2BehovLøser = finnLøserForDenneTesten<HentPersoninfoV2BehovLøser>()
    internal val risikovurderingBehovLøser = finnLøserForDenneTesten<RisikovurderingBehovLøser>()
    internal val åpneOppgaverBehovLøser = finnLøserForDenneTesten<ÅpneOppgaverBehovLøser>()
    internal val hentInfotrygdutbetalingerBehovLøser = finnLøserForDenneTesten<HentInfotrygdutbetalingerBehovLøser>()

    private inline fun <reified T : AbstractBehovLøser> finnLøserForDenneTesten() = behovLøserStub.finnLøser<T>(testContext.person.fødselsnummer)

    internal fun besvarBehovIgjen(behov: String) {
        behovLøserStub.besvarIgjen(testContext.person.fødselsnummer, behov)
    }

    internal fun fødselsnummer() = testContext.person.fødselsnummer

    internal fun organisasjonsnummer() = testContext.arbeidsgiver.organisasjonsnummer

    internal fun meldinger() = testRapid.meldingslogg(testContext.person.fødselsnummer)

    internal fun søknadOgGodkjenningbehovKommerInn(
        tags: List<String> = listOf("Innvilget"),
        tilleggsmeldinger: TilleggsmeldingReceiver.() -> Unit = {},
    ): Vedtaksperiode {
        personSenderSøknad()
        val vedtaksperiode = førsteVedtaksperiode()
        spleisForberederBehandling(vedtaksperiode, tilleggsmeldinger)
        spleisSenderGodkjenningsbehov(vedtaksperiode, tags = tags)
        return vedtaksperiode
    }

    internal fun personSenderSøknad() {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggSendSøknadNav(testContext.person, testContext.arbeidsgiver),
        )
    }

    internal fun spleisForberederBehandling(
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

    internal fun spleisReberegnerAutomatisk(vedtaksperiode: Vedtaksperiode) {
        spleisStub.spleisReberegnerPerioden(testContext, vedtaksperiode)
    }

    internal fun spleisKasterUtSaken(vedtaksperiode: Vedtaksperiode) {
        spleisStub.spleisForkasterPerioden(testContext, vedtaksperiode)
    }

    internal fun detPubliseresEnGosysOppgaveEndretMelding() {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggGosysOppgaveEndret(testContext.person),
        )
    }

    internal fun detPubliseresEnAdressebeskyttelseEndretMelding() {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggAdressebeskyttelseEndret(testContext.person),
        )
    }

    internal fun varseldefinisjonOpprettes(varselkode: String) {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggVarselkodeNyDefinisjon(varselkode),
        )
    }

    internal fun leggTilVedtaksperiode() {
        testContext.leggTilVedtaksperiode()
    }

    internal fun førsteVedtaksperiode() = testContext.vedtaksperioder.first()

    internal fun andreVedtaksperiode() = testContext.vedtaksperioder[1]

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

    internal fun spleisSenderGodkjenningsbehov(
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

    internal fun skjermetInfoEndres(skjermet: Boolean) {
        testRapid.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggEndretSkjermetinfo(testContext.person, skjermet),
        )
    }
}
