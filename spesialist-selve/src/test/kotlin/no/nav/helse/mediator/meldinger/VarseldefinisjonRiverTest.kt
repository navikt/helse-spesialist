package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VarseldefinisjonRiverTest {
    private val testRapid = TestRapid()
    private val varselRepository = mockk<VarselRepository>(relaxed = true)

    init {
        Varseldefinisjon.River(testRapid, varselRepository)
    }

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `leser definisjoner fra kafka`() {
        testRapid.sendTestMessage(melding)
        verify { varselRepository.lagreDefinisjon(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Language("JSON")
    private val melding = """{
  "@event_name": "varseldefinisjoner_endret",
  "definisjoner": [
    {
      "id":"${UUID.randomUUID()}",
      "kode": "RV_SØ_1",
      "tittel": "Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger",
      "forklaring": "Bruker har oppgitt permittering på søknad om sykepenger",
      "handling": "Kontrollér at permitteringen ikke påvirker sykepengerettighetene",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_2",
      "tittel": "Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd",
      "forklaring": "Søknaden har kommet inn mer enn 3 måneder før dagen/dagene.",
      "handling": "Hvis det ikke finnes noe unntak fra foreldelsesfristen skal du godkjenne i Speil og sende vedtaksbrev fra Infotrygd. Hvis forslaget er feil må du avvise saken i Speil og behandle i Infotrygd. Dersom dagen/dagene som er avslått er innenfor arbeidsgiverperioden trenger du ikke å sende avslag fra Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_3",
      "tittel": "Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling.",
      "forklaring": "Sykmeldingen er tilbakedatert.",
      "handling": "Hvis tilbakedateringen er godkjent, skal du finne notat i Gosys eller stå i fritekstfelt på SP-UB. Da kan du godkjenne saken i Speil. Hvis tilbakedateringen er under vurdering, skal det være en åpen Gosys-oppgave. I slike tilfeller skal du legge saken på vent til vurderingen er ferdig. Hvis tilbakedateringen er avslått, skal du avvise saken i Speil og sende vedtak. Registerer avslaget i SP-SA. Husk også å lukke oppgaven på søknaden/inntektsmeldingen i Gosys.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_4",
      "tittel": "Utdanning oppgitt i perioden i søknaden.",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_5",
      "tittel": "Søknaden inneholder Permisjonsdager utenfor sykdomsvindu",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_6",
      "tittel": "Søknaden inneholder egenmeldingsdager etter sykmeldingsperioden",
      "forklaring": "Det foreligger egenmeldingsdager etter sykmeldingsperioden",
      "handling": "Egenmeldinger kan ikke benyttes rett etter en sykmelding, sjekk arbeidsgiverperioden og utbetalingsoversikten.",
      "avviklet": true,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_7",
      "tittel": "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_8",
      "tittel": "Utenlandsopphold oppgitt i perioden i søknaden.",
      "forklaring": "Det er oppgitt utenlandsopphold utenfor EØS i søknaden",
      "handling": "Undersøk om det er vedtak om beholde sykepenger under utenlandsopphold. Hvis det er søkt uten at det er behandlet - legg saken på vent til det foreligger vedtak i saken. Korriger dager dersom det er gitt avslag - og godkjenn saken i ny løsning. Hvis brukeren har fått vedtak fra NAV-kontoret, trenger du ikke å sende nytt vedtak.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_10",
      "tittel": "Den sykmeldte har fått et nytt inntektsforhold.",
      "forklaring": "Den sykmeldte har oppgitt i søknaden at han/hun har fått et nytt inntektsforhold i sykmeldingsperioden.",
      "handling": "Du må sjekke om sykepengene skal graderes ut ifra inntekt i det nye inntektsforholdet. Du må finne informasjon om inntekten og når den er opptjent. Ta stilling til om det bør sendes en oppgave til NAV-kontoret for vurdering av vilkårene i § 8-4.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_OO_1",
      "tittel": "Det er behandlet en søknad i Speil for en senere periode enn denne.",
      "forklaring": "Søknadene har ikke kommet i vanlig rekkefølge. Dette kan skje dersom den sykmeldte avbryter en søknad/sykmelding, eller en lege skriver ut en tilbakedatert sykmelding. Perioder som allerede er avsluttet vil revurderes når denne perioden godkjennes.",
      "handling": "Undersøk at behandlingen av perioden blir riktig.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_OO_2",
      "tittel": "Saken må revurderes fordi det har blitt behandlet en tidligere periode som kan ha betydning.",
      "forklaring": "Det har blitt behandlet en søknadsperiode som gjelder et tidligere tidsrom. Dette kan medføre at antall forbrukte dager er endret eller at opptjening av ny rett til sykepenger påvirkes.",
      "handling": "Sjekk at telling av forbrukte dager og/eller opptjening av ny rett er riktig beregnet. Dersom revurderingen gjør at det er for mye utbetalt til den sykmeldte, se egne rutiner for håndtering av feilutbetalinger.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IM_1",
      "tittel": "Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første/bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt.",
      "forklaring": "Det kan bety at den sykmeldte har gjenopptatt arbeidet og så blitt sykmeldt igjen. Det kan derfor være et nytt skjæringstidspunkt.",
      "handling": "Undersøk om arbeidet er gjenopptatt slik at sykmeldingen avbrytes. Det må avklares om det blir et nytt skjæringstidspunkt. Hvis det blir nytt skjæringstidspunkt, må perioden med det nye skjæringstidspunktet avvises i vedtaksløsningen og saken må behandles i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IM_2",
      "tittel": "Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.",
      "forklaring": "Første fraværsdag i inntektsmeldingen er ulik første fraværsdag i sykdomsperioden:",
      "handling": "Kontroller at skjæringstidspunktet er riktig.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IM_3",
      "tittel": "Inntektsmeldingen og vedtaksløsningen er uenige om beregningen av arbeidsgiverperioden. Undersøk hva som er riktig arbeidsgiverperiode.",
      "forklaring": "Arbeidsgiver og vedtaksløsningen har beregnet arbeidsgiverperioden ulikt.",
      "handling": "Vurder hva som er riktig arbeidsgiverperiode. Hvis forslaget til vedtaksløsningen er feil, må saken behandles i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IM_4",
      "tittel": "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IM_5",
      "tittel": "Sykmeldte har oppgitt ferie første dag i arbeidsgiverperioden.",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IM_6",
      "tittel": "Inntektsmelding inneholder ikke beregnet inntekt",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IM_7",
      "tittel": "Brukeren har opphold i naturalytelser",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_RE_1",
      "tittel": "Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.",
      "forklaring": "Fant ikke informasjon om det er refusjon i perioden. Refusjonen er satt til 100 % siden det samsvarer med siste utbetalte periode i Infotrygd.",
      "handling": "Undersøk hva som er oppgitt i siste inntektsmelding og eventuell informasjon i Infotrygd. Dersom refusjonen er satt feil må perioden behandles i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IT_1",
      "tittel": "Det er utbetalt en periode i Infotrygd etter perioden du skal behandle nå. Undersøk at antall forbrukte dager og grunnlag i Infotrygd er riktig",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IT_2",
      "tittel": "Perioden er lagt inn i Infotrygd, men ikke utbetalt. Fjern fra Infotrygd hvis det utbetales via speil.",
      "forklaring": "Det ligger registrert en søknad om sykepenger på SP UB.",
      "handling": "Dette varselet betyr ikke at perioden må behandles i Infotrygd. Fjern perioden fra SP UB når du utbetaler via speil. Hvis inntekten er registert så kan den slettes på SP-VT. Hvis det er en forutgående periode i Infotrygd som enda ikke er behandlet, trenger du ikke å avvise perioden hvis det er flere sykepengedager igjen er enn det som ligger til behandling i Infotrygd.",
      "avviklet": true,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IT_3",
      "tittel": "Utbetaling i Infotrygd overlapper med vedtaksperioden",
      "forklaring": "Denne sakstypen tas normalt ikke inn i Speil. Det kan ha skjedd en endring fra saken ble behandlet første gang til den nå skal revurderes.",
      "handling": "Undersøk om saken kan revurderes i Speil og at utbetalingen blir riktig. Hvis saken ikke kan revurderes i Speil, må du annullere og behandle saken i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IT_4",
      "tittel": "Det er registrert utbetaling på nødnummer",
      "forklaring": "Denne sakstypen tas normalt ikke inn i Speil. Det kan ha skjedd en endring fra saken ble behandlet første gang til den nå skal revurderes.",
      "handling": "Undersøk om saken kan revurderes i Speil og at utbetalingen blir riktig. Hvis saken ikke kan revurderes i Speil, må du annullere og behandle saken i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IT_5",
      "tittel": "Mangler inntekt for første utbetalingsdag i en av infotrygdperiodene",
      "forklaring": "Denne sakstypen tas normalt ikke inn i Speil. Det kan ha skjedd en endring fra saken ble behandlet første gang til den nå skal revurderes.",
      "handling": "Undersøk om saken kan revurderes i Speil og at utbetalingen blir riktig. Hvis saken ikke kan revurderes i Speil, må du annullere og behandle saken i Infotrygd.",
      "avviklet": true,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_VV_1",
      "tittel": "Arbeidsgiver er ikke registrert i Aa-registeret.",
      "forklaring": "Arbeidsgiver på sykmelding eller inntektsmelding er ikke registrert i Aa-registeret.",
      "handling": "Sjekk at bruker er ansatt hos arbeidsgiveren.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_VV_2",
      "tittel": "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold",
      "forklaring": "Det tidligste sykefraværet bestemmer skjæringstidspunktet. Dette gjelder: Inntekt på inntektsmeldingen som kommer fra arbeidsgivere med senere starttidspunkt kan gjelde feil periode og/eller inntekten som er foreslått for minst et av arbeidsforholdene er innhentet fra a-ordningen og er gjennomsnittet av rapportert inntekt de tre siste månedene før tidspunktet for arbeidsuførhet (§8-28 3. ledd bokstav a).",
      "handling": "Du må fastsette et sykepengegrunnlag basert på inntekt fra alle arbeidsforholdene. Du må vurdere om de foreslåtte arbeidsforholdene er reelle og skal tas med i beregningen.  Hvis det er kommet inntektsmelding fra arbeidsgiver med senere starttidspunkt, må det sjekkes om inntekten for dette arbeidsforholdet er riktig beregnet (§8-28). Hvis det ikke er kommet inntektsmelding fra denne arbeidsgiveren, må den foreslåtte inntekten vurderes. Du må vurdere om den innhentede inntekten er riktig. Sjekk om det er et nytt arbeidsforhold eller varig lønnsendring i beregningsperioden. Sjekk om det variasjoner i inntekten som kan bety at det er lovlig fravær uten lønn i beregningsperioden. Vurderes etter §8-28.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_VV_3",
      "tittel": "Første utbetalingsdag er i Infotrygd og mellom 1. og 16. mai. Kontroller at riktig grunnbeløp er brukt.",
      "forklaring": "Vi klarer ikke å lese riktig skjæringstidspunkt når saken har startet i Infotrygd. Vi må kontrollere at riktig grunnbeløp er brukt i saker med sykepengegrunnlag over 6G.",
      "handling": "Kontroller at dagsatsen samsvarer med grunnbeløpet som skal brukes.",
      "avviklet": true,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_VV_4",
      "tittel": "Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %. Vurder å sende vedtaksbrev fra Infotrygd",
      "forklaring": "Minst én dag uten utbetaling på grunn av sykdomsgrad under 20",
      "handling": "Dersom du er enig i vurderingen, send avslagsbrev fra Infotrygd og godkjenn i ny løsning. OBS!!: Hør med den som er sykmeldt hvis det er mistanke om misforståelser ved utfylling av søknaden. Da kan du sende spørsmål i Modia og legge saken på vent til du får svar.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_VV_5",
      "tittel": "Bruker mangler nødvendig inntekt ved validering av Vilkårsgrunnlag",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_VV_8",
      "tittel": "Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig.",
      "forklaring": "Den sykmeldte har skiftet arbeidsgiver i løpet av sykefraværet.",
      "handling": "Sjekk at riktig dagsats er lagt til grunn.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_VV_9",
      "tittel": "Bruker er fortsatt syk 26 uker etter maksdato",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_OV_1",
      "tittel": "Perioden er avslått på grunn av manglende opptjening",
      "forklaring": "Alle dager er avvist på grunn av at personen ikke har jobbet mer enn fire uker før sykmeldingstidspunktet",
      "handling": "Undersøk at opptjeningen er vurdert riktig. Hvis opptjening ikke er oppfylt kan saken godkjennes i speil og det må sendes avslagsbrev i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_OV_2",
      "tittel": "Opptjeningsvurdering må gjøres manuelt fordi opplysningene fra AA-registeret er ufullstendige",
      "forklaring": null,
      "handling": null,
      "avviklet": true,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_MV_1",
      "tittel": "Vurder lovvalg og medlemskap",
      "forklaring": "Medlemskapssjekken har ikke klart å entydig konkludere med at bruker er medlem",
      "handling": "Sjekk om personen er medlem i folketrygden – se i medlemskapsfanen i Gosys. Sjekk bostedsadressen og hvor arbeidet utføres. Hvis bokommune er 0393 eller 2101 skal saken avvises og behandles av NAV Utland. Kilder: Gosys og Aa-registeret.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_MV_2",
      "tittel": "Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden",
      "forklaring": "Alle dager er avvist på grunn av at personen ikke er medlem av Folketrygden",
      "handling": "Undersøk om det er riktig at personen ikke er medlem av Folketrygden. Hvis personen ikke er medlem kan saken godkjennes i speil og det må sendes vedtak i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IV_1",
      "tittel": "Bruker har flere inntektskilder de siste tre månedene enn arbeidsforhold som er oppdaget i Aa-registeret.",
      "forklaring": "Bruker kan være frilanser.",
      "handling": "Sjekk om bruker er kombinert arbeidstaker og frilanser. Hvis ja, send saken til Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IV_2",
      "tittel": "Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.",
      "forklaring": "Det er lagt inn en ny inntekt som gir 25 % avvik. Skjønnsmessig fastsettelse av sykepengegrunnlaget støttes ikke i Speil.",
      "handling": "Du kan endre inntekten en gang til hvis du har gjort noe feil. Dersom endringen skal gjennomføres må periodene annulleres i Speil og behandles i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_IV_3",
      "tittel": "Fant frilanserinntekt på en arbeidsgiver de siste 3 månedene",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SV_1",
      "tittel": "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag",
      "forklaring": "Alle dager er avvist på grunn av at sykepengegrunnlaget er under minstekrav",
      "handling": "Undersøk at sykepengegrunnlaget er korrekt. Hvis det er fastsatt korrekt kan saken godkjennes i speil, da må avslagsbrev sendes i infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SV_2",
      "tittel": "Minst en arbeidsgiver inngår ikke i sykepengegrunnlaget",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_AY_3",
      "tittel": "Bruker har mottatt AAP innenfor 6 måneder før skjæringstidspunktet. Kontroller at brukeren har rett til sykepenger",
      "forklaring": "Bruker har mottatt AAP innenfor 6 måneder før skjæringstidspunktet",
      "handling": "Kontrollér ny opptjeningstid etter maksdato",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_AY_4",
      "tittel": "Bruker har mottatt dagpenger innenfor 4 uker før skjæringstidspunktet. Kontroller om bruker er dagpengemottaker. Kombinerte ytelser støttes foreløpig ikke av systemet",
      "forklaring": "Bruker har mottatt dagpenger innenfor 4 uker før skjæringstidspunktet",
      "handling": "Sjekk om bruker er dagpengemottaker på sykmeldingstidspunktet. Hvis ja kan ikke saken behandles i dette systemet ennå.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_AY_5",
      "tittel": "Det er utbetalt foreldrepenger i samme periode.",
      "forklaring": "Det er utbetalt foreldrepenger i Infotrygd etter at perioden ble godkjent i Speil.",
      "handling": "Undersøk hvilken ytelse som er riktig. Hvis utbetalingene i Speil må avslås på grunn av andre ytelser må du annullere og behandle i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_AY_6",
      "tittel": "Det er utbetalt pleiepenger i samme periode.",
      "forklaring": "Det er utbetalt pleiepenger i Infotrygd etter at perioden ble godkjent i Speil.",
      "handling": "Undersøk hvilken ytelse som er riktig. Hvis utbetalingene i Speil må avslås på grunn av andre ytelser må du annullere og behandle i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_AY_7",
      "tittel": "Det er utbetalt omsorgspenger i samme periode.",
      "forklaring": "Det er utbetalt omsorgspenger i Infotrygd etter at perioden ble godkjent i Speil.",
      "handling": "Undersøk hvilken ytelse som er riktig. Hvis utbetalingene i Speil må avslås på grunn av andre ytelser må du annullere og behandle i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_AY_8",
      "tittel": "Det er utbetalt opplæringspenger i samme periode.",
      "forklaring": "Det er utbetalt opplæringspenger i Infotrygd etter at perioden ble godkjent i Speil.",
      "handling": "Undersøk hvilken ytelse som er riktig. Hvis utbetalingene i Speil må avslås på grunn av andre ytelser må du annullere og behandle i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_AY_9",
      "tittel": "Det er institusjonsopphold i perioden. Vurder retten til sykepenger.",
      "forklaring": "Vi henter inn opplysninger om institusjonsopphold. Det har kommet informasjon om at den sykmeldte oppholdt seg i institusjon (sykehus eller fengsel) i perioden.",
      "handling": "Undersøk om institusjonsoppholdet er forenlig med å motta sykepenger. Dersom en periode skal avslås på grunn av institusjonsopphold må saken annulleres og behandles i Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SI_1",
      "tittel": "Feil under simulering",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SI_2",
      "tittel": "Simulering av revurdert utbetaling feilet. Utbetalingen må annulleres",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_UT_1",
      "tittel": "Utbetaling av revurdert periode ble avvist av saksbehandler. Utbetalingen må annulleres",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_UT_2",
      "tittel": "Utbetalingen ble gjennomført, men med advarsel",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_UT_3",
      "tittel": "Feil ved utbetalingstidslinjebygging",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_UT_4",
      "tittel": "Finner ingen utbetaling å annullere",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_OS_1",
      "tittel": "Utbetalingen forlenger et tidligere oppdrag som opphørte alle utbetalte dager. Sjekk simuleringen.",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_OS_2",
      "tittel": "Utbetalingens fra og med-dato er endret. Kontroller simuleringen",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_OS_3",
      "tittel": "Endrer tidligere oppdrag. Kontroller simuleringen.",
      "forklaring": "Det er opphørt en tidligere linje i Oppdrag. Dette skjer dersom en FOM-dato på en linje endrer seg.",
      "handling": "Undersøk om perioden blir riktig simulert, og ta en sjekk på oppdraget i ettertid",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_RV_1",
      "tittel": "Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_9",
      "tittel": "Det er oppgitt annen inntektskilde i søknaden. Vurder inntekt.",
      "forklaring": "Den sykmeldte har oppgitt «Annet» som annen inntektskilde i søknaden.",
      "handling": "Sjekk hva den andre inntektskilden består av og vurder om saken kan behandles i Speil. Hvis saken ikke kan behandles i Speil, må du behandle saken i Infotrygd.",
      "avviklet": true,
      "opprettet": "2022-11-01T14:04:27.271588"
    }
  ],
  "@id": "5ab434ad-e20d-40e8-92ce-0b0f56109d9a",
  "@opprettet": "2022-11-29T11:51:28.597000151"
}
    """
}