package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.tellInaktivtVarsel
import no.nav.helse.tellVarsel

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

    SB_BO_1(""),
    SB_EX_1(""),
    SB_EX_2(""),
    SB_EX_3(""),
    SB_EX_4("Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys"),
    SB_IK_1(""),
    SB_RV_1(""),
    SB_RV_2("");

    init {
        require(this.name.matches(regex)) { "Ugyldig varselkode-format: ${this.name}" }
    }

    internal fun nyttVarsel(vedtaksperiodeId: UUID, varselRepository: VarselRepository) {
        if (erAktivFor(vedtaksperiodeId, varselRepository)) return
        varselRepository.lagreVarsel(UUID.randomUUID(), this.name, LocalDateTime.now(), vedtaksperiodeId)
        tellVarsel(this.name)
    }

    internal fun deaktiverFor(vedtaksperiodeId: UUID, varselRepository: VarselRepository) {
        if (!erAktivFor(vedtaksperiodeId, varselRepository)) return
        varselRepository.deaktiverFor(vedtaksperiodeId, this.name)
        tellInaktivtVarsel(this.name)
    }

    private fun erAktivFor(vedtaksperiodeId: UUID, varselRepository: VarselRepository): Boolean {
        return varselRepository.erAktivFor(vedtaksperiodeId, this.name)
    }

    override fun toString() = "${this.name}: $varseltekst"

    internal companion object {
        internal val aktiveVarselkoder = values().filterNot { it.avviklet }
    }
}
