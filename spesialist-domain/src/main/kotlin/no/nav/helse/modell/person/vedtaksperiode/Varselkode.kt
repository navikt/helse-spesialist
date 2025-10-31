package no.nav.helse.modell.person.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID

// Alle Varselkoder må følge formatet
internal const val VARSELKODEFORMAT = "(SB_|RV_)\\D{2}_\\d{1,3}"
private val regex = "^$VARSELKODEFORMAT$".toRegex()

// RV = Risikovurdering
// BO = Beslutteroppgave
// EX = Eksterne systemer
// IK = Inngangskriterier
// SØ = Søknad

enum class Varselkode(
    private val varseltekst: String,
) {
    SB_BO_1("Beslutteroppgave: Lovvalg og medlemskap"), // Avviklet
    SB_BO_2("Beslutteroppgave: Overstyring av utbetalingsdager"), // Avviklet
    SB_BO_3("Beslutteroppgave: Overstyring av inntekt"), // Avviklet
    SB_BO_4("Beslutteroppgave: Overstyring av annet arbeidsforhold"), // Avviklet
    SB_BO_5(
        "Beslutteroppgave: Lovvalg og medlemskap og/eller overstyring av utbetalingsdager og/eller overstyring av inntekt og/eller overstyring av annet arbeidsforhold",
    ), // Avviklet
    SB_EX_1("Det finnes åpne oppgaver på sykepenger i Gosys"),
    SB_EX_2(
        "Ikke registrert eller mangler samtykke i Kontakt- og reservasjonsregisteret, eventuell kommunikasjon må skje i brevform",
    ), // Avviklet
    SB_EX_3("Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys"),
    SB_IK_1("Registrert fullmakt på personen"),
    SB_RV_1("Faresignaler oppdaget. Kontroller om faresignalene påvirker retten til sykepenger"),
    SB_RV_2("Veileder har stanset automatisk behandling. Se Gosys for mer informasjon"),
    SB_RV_3("Kunne ikke sjekke om veileder har stanset automatisk behandling på grunn av teknisk feil"),
    SB_EX_4("Den sykmeldte er under vergemål"),
    SB_EX_5(
        "Det har kommet inn dokumentasjon som igangsetter en revurdering, og den sykmeldte er nå registrert på bokommune 0393 (NAV utland og fellestjenester)",
    ),
    SB_SØ_1("Sjekk endring av søknad"),

    RV_IV_2("Over 25% avvik"),
    RV_OV_5("Den sykmeldte ser ikke ut til å ha opptjent ny rett til sykepenger etter § 8-12 og arbeidsgiver krever refusjon"),
    ;

    init {
        require(this.name.matches(regex)) { "Ugyldig varselkode-format: ${this.name}" }
    }

    fun nyttVarsel(vedtaksperiodeId: UUID): LegacyVarsel = LegacyVarsel(UUID.randomUUID(), this.name, LocalDateTime.now(), vedtaksperiodeId)

    override fun toString() = "${this.name}: $varseltekst"
}
