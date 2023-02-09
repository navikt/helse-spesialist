package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.Generasjon

// Alle Varselkoder må følge formatet
internal const val varselkodeformat = "SB_\\D{2}_\\d{1,3}"
private val regex = "^$varselkodeformat$".toRegex()

// RV = Risikovurdering
// BO = Beslutteroppgave
// EX = Eksterne systemer
// IK = Inngangskriterier

enum class Varselkode(
    private val varseltekst: String,
    private val avviklet: Boolean = false,
) {

    SB_BO_1("Beslutteroppgave: Lovvalg og medlemskap"),
    SB_BO_2("Beslutteroppgave: Overstyring av utbetalingsdager"),
    SB_BO_3("Beslutteroppgave: Overstyring av inntekt"),
    SB_BO_4("Beslutteroppgave: Overstyring av annet arbeidsforhold"),
    SB_BO_5("Beslutteroppgave: Lovvalg og medlemskap og/eller overstyring av utbetalingsdager og/eller overstyring av inntekt og/eller overstyring av annet arbeidsforhold", avviklet = true),
    SB_EX_1("Det finnes åpne oppgaver på sykepenger i Gosys"),
    SB_EX_2("Ikke registrert eller mangler samtykke i Kontakt- og reservasjonsregisteret, eventuell kommunikasjon må skje i brevform", avviklet = true),
    SB_EX_3("Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys"),
    SB_IK_1("Registrert fullmakt på personen"),
    SB_RV_1("Faresignaler oppdaget. Kontroller om faresignalene påvirker retten til sykepenger"),
    SB_RV_2("Veileder har stanset automatisk behandling. Se Gosys for mer informasjon"),
    SB_RV_3("Kunne ikke sjekke om veileder har stanset automatisk behandling på grunn av teknisk feil");

    init {
        require(this.name.matches(regex)) { "Ugyldig varselkode-format: ${this.name}" }
    }

    internal fun nyttVarsel(generasjon: Generasjon, varselRepository: VarselRepository) {
        generasjon.håndterSaksbehandlingsvarsel(UUID.randomUUID(), this, LocalDateTime.now(), varselRepository)
    }

    internal fun deaktiverFor(generasjon: Generasjon, varselRepository: VarselRepository) {
        generasjon.håndterDeaktivertVarsel(this.name, varselRepository)
    }

    override fun toString() = "${this.name}: $varseltekst"
}
