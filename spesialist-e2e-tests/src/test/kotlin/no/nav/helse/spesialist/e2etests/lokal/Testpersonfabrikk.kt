package no.nav.helse.spesialist.e2etests.lokal

import kotliquery.sessionOf
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.e2etests.Meldingsbygger
import no.nav.helse.spesialist.e2etests.SpesialistTestApplikasjon
import no.nav.helse.spesialist.e2etests.behovløserstubs.RisikovurderingBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.ÅpneOppgaverBehovLøser
import no.nav.helse.spesialist.e2etests.context.Arbeidsgiver
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Setter opp testpersoner i en lokalt kjørende Spesialist ved å spille inn de samme meldingene som
 * Spleis ville sendt på rapiden, og ved å stubbe svarene fra tjenestene Spesialist spør.
 */
class Testpersonfabrikk(
    private val applikasjon: SpesialistTestApplikasjon,
) {
    private val opprettedeTestpersoner = ConcurrentHashMap<String, Testperson>()
    private val ventendeVedtaksperioder = ConcurrentHashMap<String, ConcurrentLinkedDeque<Vedtaksperiode>>()
    private val testContexter = ConcurrentHashMap<String, TestContext>()

    fun opprettTestperson(spesifikasjon: Testpersonspesifikasjon): Testperson {
        val testContext = byggTestContext(spesifikasjon)
        val person = testContext.person

        stubEksterneTjenester(testContext, spesifikasjon)
        testContexter[person.fødselsnummer] = testContext
        ventendeVedtaksperioder[person.fødselsnummer] =
            ConcurrentLinkedDeque(testContext.vedtaksperioder.sortedBy(Vedtaksperiode::fom))

        sendNestePeriodeHvisNoenVenter(testContext)

        val førsteArbeidsgiver = testContext.arbeidsgivere.first()
        val førsteVedtaksperiode = testContext.vedtaksperioderFor(førsteArbeidsgiver).firstOrNull()

        return Testperson(
            fødselsnummer = person.fødselsnummer,
            aktørId = person.aktørId,
            navn = listOfNotNull(person.fornavn, person.mellomnavn, person.etternavn).joinToString(" "),
            organisasjonsnummer = førsteArbeidsgiver.organisasjonsnummer,
            organisasjonsnavn = førsteArbeidsgiver.navn,
            vedtaksperiodeId = førsteVedtaksperiode?.vedtaksperiodeId,
            arbeidsgivere =
                testContext.arbeidsgivere.map { arbeidsgiver ->
                    TestpersonArbeidsgiver(
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                        organisasjonsnavn = arbeidsgiver.navn,
                        vedtaksperiodeIder = testContext.vedtaksperioderFor(arbeidsgiver).map(Vedtaksperiode::vedtaksperiodeId),
                    )
                },
        ).also { opprettedeTestpersoner[it.fødselsnummer] = it }
    }

    fun opprettedeTestpersoner(): List<Testperson> = opprettedeTestpersoner.values.sortedBy(Testperson::fødselsnummer)

    /**
     * Spesialist tillater ikke mer enn én aktiv oppgave per person samtidig. Testfabrikken sender
     * derfor bare én vedtaksperiode om gangen: neste periode i køen sendes ikke automatisk – kall
     * denne metoden etter at den gjeldende oppgaven faktisk er godkjent (automatisk eller manuelt av
     * en saksbehandler) for å sende neste periode i rekkefølgen. Gjør ingenting dersom personen ikke
     * finnes, eller det ikke er flere perioder i køen.
     */
    fun fortsettMedNestePeriode(fødselsnummer: String) {
        val testContext = testContexter[fødselsnummer] ?: return
        sendNestePeriodeHvisNoenVenter(testContext)
    }

    /**
     * Bygger context-modellen fra spesifikasjonen: én Arbeidsgiver per Arbeidsgiverspesifikasjon,
     * og for hver av dem én Vedtaksperiode per Vedtaksperiodespesifikasjon. Perioder hos samme
     * arbeidsgiver forskyves én måned per periode; perioder med samme indeks hos ulike arbeidsgivere
     * havner i samme måned (parallelle arbeidsforhold), med mindre fom/tom er eksplisitt satt.
     *
     * Skjæringstidspunktet utledes til slutt ved å gruppere ALLE vedtaksperioder på tvers av
     * arbeidsgivere i sykefraværstilfeller: en sammenhengende periode med sykdom, der opphold (gap)
     * på mer enn én dag starter et nytt tilfelle. Alle perioder i samme tilfelle får samme
     * skjæringstidspunkt (tilfellets tidligste fom).
     */
    private fun byggTestContext(spesifikasjon: Testpersonspesifikasjon): TestContext {
        val arbeidsgivere =
            spesifikasjon.arbeidsgivere.map { ag ->
                Arbeidsgiver(
                    organisasjonsnummer = ag.organisasjonsnummer ?: Arbeidsgiver().organisasjonsnummer,
                    navn = ag.navn ?: Arbeidsgiver().navn,
                )
            }
        val testContext = TestContext(arbeidsgivere = arbeidsgivere.toMutableList())

        val vedtaksperioder = testContext.vedtaksperioder
        vedtaksperioder.clear()

        spesifikasjon.arbeidsgivere.forEachIndexed { agIndex, ag ->
            val arbeidsgiver = arbeidsgivere[agIndex]
            ag.vedtaksperioder.forEachIndexed { periodeIndex, periodeSpek ->
                val fom = periodeSpek.fom ?: (1 jan 2018).plusMonths(periodeIndex.toLong())
                val tom = periodeSpek.tom ?: fom.withDayOfMonth(fom.lengthOfMonth())
                vedtaksperioder.add(
                    Vedtaksperiode(
                        fom = fom,
                        tom = tom,
                        skjæringstidspunkt = fom,
                        arbeidsgiver = arbeidsgiver,
                        varselkoder = periodeSpek.varselkoder ?: spesifikasjon.varselkoder,
                    ),
                )
            }
        }

        utledSykefraværstilfeller(vedtaksperioder)

        return testContext
    }

    private fun utledSykefraværstilfeller(vedtaksperioder: List<Vedtaksperiode>) {
        vedtaksperioder
            .sortedBy(Vedtaksperiode::fom)
            .fold(mutableListOf<MutableList<Vedtaksperiode>>()) { tilfeller, periode ->
                val gjeldende = tilfeller.lastOrNull()
                if (gjeldende != null && !periode.fom.isAfter(gjeldende.maxOf(Vedtaksperiode::tom).plusDays(1))) {
                    gjeldende.add(periode)
                } else {
                    tilfeller.add(mutableListOf(periode))
                }
                tilfeller
            }.forEach { perioderITilfellet ->
                val skjæringstidspunkt = perioderITilfellet.minOf(Vedtaksperiode::fom)
                perioderITilfellet.forEach { it.skjæringstidspunkt = skjæringstidspunkt }
            }
    }

    private fun stubEksterneTjenester(
        testContext: TestContext,
        spesifikasjon: Testpersonspesifikasjon,
    ) {
        val fødselsnummer = testContext.person.fødselsnummer
        applikasjon.behovLøserStub.init(person = testContext.person, arbeidsgivere = testContext.arbeidsgivere)
        applikasjon.spleisStub.init(testContext)
        applikasjon.behovLøserStub
            .finnLøser<RisikovurderingBehovLøser>(fødselsnummer)
            .kanGodkjenneAutomatisk = spesifikasjon.kanGodkjennesAutomatisk
        applikasjon.behovLøserStub
            .finnLøser<ÅpneOppgaverBehovLøser>(fødselsnummer)
            .antall = spesifikasjon.antallÅpneGosysoppgaver
    }

    /**
     * Sender neste vedtaksperiode i køen for personen, hvis det ikke allerede er en aktiv oppgave
     * for personen. Spesialist tillater ikke mer enn én aktiv oppgave per person samtidig, så denne
     * sjekken hindrer at vi sender en periode til godkjenning mens en annen står ubehandlet.
     *
     * Blir kalt både når testpersonen opprettes (for å sende den første perioden) og eksplisitt via
     * [fortsettMedNestePeriode] etter at en oppgave er godkjent.
     */
    private fun sendNestePeriodeHvisNoenVenter(testContext: TestContext) {
        val person = testContext.person
        val kø = ventendeVedtaksperioder[person.fødselsnummer] ?: return
        if (kø.isEmpty()) return
        if (harAktivOppgave(person.fødselsnummer)) return

        val vedtaksperiode = kø.poll() ?: return
        sendVedtaksperiode(testContext, vedtaksperiode)
    }

    private fun harAktivOppgave(fødselsnummer: String): Boolean =
        sessionOf(applikasjon.dbModule.dataSource, strict = true).use { session ->
            session.run(
                asSQL(
                    """
                    SELECT count(*) as antall
                    FROM oppgave o, vedtaksperiode v, person p
                    WHERE o.vedtak_ref = v.id
                    AND v.person_ref = p.id
                    AND p.fødselsnummer = :fodselsnummer
                    AND o.status = 'AvventerSaksbehandler'::oppgavestatus
                    """.trimIndent(),
                    "fodselsnummer" to fødselsnummer,
                ).map { it.int("antall") }.asSingle,
            )
        } != 0

    /**
     * Spiller inn hele meldingskjeden for én vedtaksperiode, fra søknad til godkjenningsbehov. Dette
     * kan enten godkjennes automatisk (hvis ingen varsler/åpne Gosys-oppgaver blokkerer, og
     * risikovurderingen kan godkjennes automatisk) eller ende med en aktiv oppgave for en
     * saksbehandler.
     */
    private fun sendVedtaksperiode(
        testContext: TestContext,
        vedtaksperiode: Vedtaksperiode,
    ) {
        val person = testContext.person
        val ghostOrgnumre = testContext.ghostArbeidsgivere().map(Arbeidsgiver::organisasjonsnummer)
        val flereArbeidsgivereMedPerioder = testContext.arbeidsgivere.count { testContext.vedtaksperioderFor(it).isNotEmpty() } > 1
        val arbeidsgiver = vedtaksperiode.arbeidsgiver
        val varselkoder = vedtaksperiode.varselkoder

        publiser(person, Meldingsbygger.byggSendSøknadNav(person, arbeidsgiver))

        vedtaksperiode.spleisBehandlingId = UUID.randomUUID()
        publiser(person, Meldingsbygger.byggBehandlingOpprettet(vedtaksperiode, person, arbeidsgiver))

        vedtaksperiode.nyUtbetaling()
        publiser(person, Meldingsbygger.byggVedtaksperiodeNyUtbetaling(vedtaksperiode, person, arbeidsgiver))

        if (varselkoder.isNotEmpty()) {
            publiser(
                person,
                Meldingsbygger.byggAktivitetsloggNyAktivitetMedVarsler(
                    varselkoder = varselkoder,
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

        // Kun perioder som allerede har fått en behandling (spleisBehandlingId) kan refereres til i
        // meldingen. Perioder som ikke er sendt ennå, har ikke fått en.
        val perioderMedSammeSkjæringstidspunkt =
            testContext.vedtaksperioder.filter {
                it.skjæringstidspunkt == vedtaksperiode.skjæringstidspunkt && it.spleisBehandlingId != null
            }

        // Snapshotet må reflektere periodene som faktisk er sendt så langt, siden en eventuell
        // automatisk godkjenning kan skje synkront rett under.
        applikasjon.spleisStub.stubSnapshotForPerson(testContext)

        publiser(
            person,
            Meldingsbygger.byggGodkjenningsbehov(
                person = person,
                arbeidsgiver = arbeidsgiver,
                vilkårsgrunnlagId = testContext.vilkårsgrunnlagId,
                vedtaksperiode = vedtaksperiode,
                orgnummereMedRelevanteArbeidsforhold = ghostOrgnumre,
                perioderMedSammeSkjæringstidspunkt = perioderMedSammeSkjæringstidspunkt,
                inntektskilde =
                    if (flereArbeidsgivereMedPerioder) {
                        Inntektskilde.FLERE_ARBEIDSGIVERE
                    } else {
                        Inntektskilde.EN_ARBEIDSGIVER
                    },
            ),
        )

        // Hvis denne perioden ble godkjent automatisk (ingen aktiv oppgave), fortsetter vi med
        // neste periode i køen med det samme.
        sendNestePeriodeHvisNoenVenter(testContext)
    }

    private fun publiser(
        person: Person,
        melding: String,
    ) {
        applikasjon.testRapid.publish(person.fødselsnummer, melding)
    }
}

/**
 * Standardverdiene gir en sak med én arbeidsgiver og én vedtaksperiode som blir liggende til manuell
 * behandling, fordi varselet gjør at saken ikke kan behandles automatisk. Uten varsler, uten åpne
 * Gosys-oppgaver og med en risikovurdering som kan godkjennes automatisk, blir saken behandlet
 * automatisk og det opprettes ingen oppgave.
 *
 * @param varselkoder fallback-varsler for vedtaksperioder som ikke har egne varselkoder, må finnes i
 * api_varseldefinisjon
 * @param antallÅpneGosysoppgaver svaret stubben gir på ÅpneOppgaver-behovet
 * @param kanGodkjennesAutomatisk svaret stubben gir på Risikovurdering-behovet
 * @param arbeidsgivere hvilke arbeidsgivere personen skal ha, og hvilke vedtaksperioder hver av dem
 * skal ha. En arbeidsgiver uten vedtaksperioder blir en "ghost"-arbeidsgiver: den vises i
 * sykepengegrunnlaget, men har ingen egne perioder.
 */
data class Testpersonspesifikasjon(
    val varselkoder: List<String> = listOf("RV_IV_1"),
    val antallÅpneGosysoppgaver: Int = 0,
    val kanGodkjennesAutomatisk: Boolean = true,
    val arbeidsgivere: List<Arbeidsgiverspesifikasjon> = listOf(Arbeidsgiverspesifikasjon()),
)

/**
 * @param organisasjonsnummer null gir et generert organisasjonsnummer
 * @param navn null gir et generert organisasjonsnavn
 * @param vedtaksperioder tom liste gir en arbeidsgiver uten egne vedtaksperioder ("ghost"-arbeidsgiver)
 */
data class Arbeidsgiverspesifikasjon(
    val organisasjonsnummer: String? = null,
    val navn: String? = null,
    val vedtaksperioder: List<Vedtaksperiodespesifikasjon> = listOf(Vedtaksperiodespesifikasjon()),
)

/**
 * @param fom null gir en dato utledet fra periodens indeks hos arbeidsgiveren (én måned forskjøvet
 * per periode, med start 1. januar 2018)
 * @param tom null gir siste dag i fom-månedens
 * @param varselkoder null gir at perioden arver Testpersonspesifikasjon.varselkoder
 */
data class Vedtaksperiodespesifikasjon(
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val varselkoder: List<String>? = null,
)

data class Testperson(
    val fødselsnummer: String,
    val aktørId: String,
    val navn: String,
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val vedtaksperiodeId: UUID?,
    val arbeidsgivere: List<TestpersonArbeidsgiver> = emptyList(),
)

data class TestpersonArbeidsgiver(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val vedtaksperiodeIder: List<UUID>,
)
