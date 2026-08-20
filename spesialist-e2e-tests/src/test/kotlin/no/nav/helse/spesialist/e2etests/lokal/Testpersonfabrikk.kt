package no.nav.helse.spesialist.e2etests.lokal

import no.nav.helse.spesialist.e2etests.Meldingsbygger
import no.nav.helse.spesialist.e2etests.SpesialistTestApplikasjon
import no.nav.helse.spesialist.e2etests.behovløserstubs.RisikovurderingBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.ÅpneOppgaverBehovLøser
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Setter opp testpersoner i en lokalt kjørende Spesialist ved å spille inn de samme meldingene som
 * Spleis ville sendt på rapiden, og ved å stubbe svarene fra tjenestene Spesialist spør.
 */
class Testpersonfabrikk(
    private val applikasjon: SpesialistTestApplikasjon,
) {
    private val opprettedeTestpersoner = ConcurrentHashMap<String, Testperson>()

    fun opprettTestperson(spesifikasjon: Testpersonspesifikasjon): Testperson {
        val testContext = TestContext()
        val person = testContext.person
        val vedtaksperiode = testContext.vedtaksperioder.first()

        stubEksterneTjenester(testContext, spesifikasjon)
        spillInnMeldingerFraSpleis(testContext, vedtaksperiode, spesifikasjon)
        applikasjon.spleisStub.stubSnapshotForPerson(testContext)

        return Testperson(
            fødselsnummer = person.fødselsnummer,
            aktørId = person.aktørId,
            navn = listOfNotNull(person.fornavn, person.mellomnavn, person.etternavn).joinToString(" "),
            organisasjonsnummer = testContext.arbeidsgiver.organisasjonsnummer,
            organisasjonsnavn = testContext.arbeidsgiver.navn,
            vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
        ).also { opprettedeTestpersoner[it.fødselsnummer] = it }
    }

    fun opprettedeTestpersoner(): List<Testperson> = opprettedeTestpersoner.values.sortedBy(Testperson::fødselsnummer)

    private fun stubEksterneTjenester(
        testContext: TestContext,
        spesifikasjon: Testpersonspesifikasjon,
    ) {
        val fødselsnummer = testContext.person.fødselsnummer
        applikasjon.behovLøserStub.init(person = testContext.person, arbeidsgiver = testContext.arbeidsgiver)
        applikasjon.spleisStub.init(testContext)
        applikasjon.behovLøserStub
            .finnLøser<RisikovurderingBehovLøser>(fødselsnummer)
            .kanGodkjenneAutomatisk = spesifikasjon.kanGodkjennesAutomatisk
        applikasjon.behovLøserStub
            .finnLøser<ÅpneOppgaverBehovLøser>(fødselsnummer)
            .antall = spesifikasjon.antallÅpneGosysoppgaver
    }

    private fun spillInnMeldingerFraSpleis(
        testContext: TestContext,
        vedtaksperiode: Vedtaksperiode,
        spesifikasjon: Testpersonspesifikasjon,
    ) {
        val person = testContext.person
        val arbeidsgiver = testContext.arbeidsgiver

        publiser(person, Meldingsbygger.byggSendSøknadNav(person, arbeidsgiver))

        vedtaksperiode.spleisBehandlingId = UUID.randomUUID()
        publiser(person, Meldingsbygger.byggBehandlingOpprettet(vedtaksperiode, person, arbeidsgiver))

        vedtaksperiode.nyUtbetaling()
        publiser(person, Meldingsbygger.byggVedtaksperiodeNyUtbetaling(vedtaksperiode, person, arbeidsgiver))

        if (spesifikasjon.varselkoder.isNotEmpty()) {
            publiser(
                person,
                Meldingsbygger.byggAktivitetsloggNyAktivitetMedVarsler(
                    varselkoder = spesifikasjon.varselkoder,
                    person = person,
                    arbeidsgiver = arbeidsgiver,
                    vedtaksperiode = vedtaksperiode,
                ),
            )
        }

        publiser(
            person,
            Meldingsbygger.byggUtbetalingEndret(
                vedtaksperiode = vedtaksperiode,
                person = person,
                arbeidsgiver = arbeidsgiver,
                forrigeStatus = "NY",
                gjeldendeStatus = "IKKE_UTBETALT",
            ),
        )

        publiser(
            person,
            Meldingsbygger.byggGodkjenningsbehov(
                person = person,
                arbeidsgiver = arbeidsgiver,
                vilkårsgrunnlagId = testContext.vilkårsgrunnlagId,
                vedtaksperiode = vedtaksperiode,
            ),
        )
    }

    private fun publiser(
        person: Person,
        melding: String,
    ) {
        applikasjon.testRapid.publish(person.fødselsnummer, melding)
    }
}

/**
 * Standardverdiene gir en sak som blir liggende til manuell behandling, fordi varselet gjør at saken
 * ikke kan behandles automatisk. Uten varsler, uten åpne Gosys-oppgaver og med en risikovurdering som
 * kan godkjennes automatisk, blir saken behandlet automatisk og det opprettes ingen oppgave.
 *
 * @param varselkoder varsler Spleis melder inn på behandlingen, må finnes i api_varseldefinisjon
 * @param antallÅpneGosysoppgaver svaret stubben gir på ÅpneOppgaver-behovet
 * @param kanGodkjennesAutomatisk svaret stubben gir på Risikovurdering-behovet
 */
data class Testpersonspesifikasjon(
    val varselkoder: List<String> = listOf("RV_IV_1"),
    val antallÅpneGosysoppgaver: Int = 0,
    val kanGodkjennesAutomatisk: Boolean = true,
)

data class Testperson(
    val fødselsnummer: String,
    val aktørId: String,
    val navn: String,
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val vedtaksperiodeId: UUID,
)
