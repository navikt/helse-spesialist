package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.SortedSet
import java.util.UUID
import kotlin.reflect.KMutableProperty0

data class TilkommenInntektId(val fødselsnummer: String, val uuid: UUID) {
    companion object {
        fun ny(fødselsnummer: String): TilkommenInntektId = fra(fødselsnummer, UUID.randomUUID())

        fun fra(
            fødselsnummer: String,
            uuid: UUID,
        ): TilkommenInntektId = TilkommenInntektId(fødselsnummer, uuid)
    }
}

class TilkommenInntekt private constructor(
    opprettetEvent: TilkommenInntektOpprettetEvent,
) : AggregateRoot<TilkommenInntektId>(opprettetEvent.metadata.tilkommenInntektId) {
    private val _events: MutableList<TilkommenInntektEvent> = mutableListOf(opprettetEvent)
    val events: List<TilkommenInntektEvent> get() = _events

    var totrinnsvurderingId: TotrinnsvurderingId = opprettetEvent.metadata.totrinnsvurderingId
        private set
    var organisasjonsnummer: String = opprettetEvent.organisasjonsnummer
        private set
    var periode: Periode = opprettetEvent.periode
        private set
    var periodebeløp: BigDecimal = opprettetEvent.periodebeløp
        private set
    var dager: SortedSet<LocalDate> = opprettetEvent.dager
        private set
    var fjernet: Boolean = false
        private set
    var versjon: Int = opprettetEvent.metadata.sekvensnummer
        private set

    fun dagbeløp(): BigDecimal = periodebeløp.setScale(4).divide(dager.size.toBigDecimal(), RoundingMode.HALF_UP)

    fun endreTil(
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        dager: Set<LocalDate>,
        saksbehandlerIdent: String,
        notatTilBeslutter: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ) {
        apply(
            TilkommenInntektEndretEvent(
                TilkommenInntektEvent.Metadata(
                    tilkommenInntektId = id(),
                    sekvensnummer = versjon + 1,
                    tidspunkt = Instant.now(),
                    utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                    notatTilBeslutter = notatTilBeslutter,
                    totrinnsvurderingId = totrinnsvurderingId,
                ),
                endringer =
                    TilkommenInntektEvent.Endringer(
                        organisasjonsnummer = muligEndring(fra = this.organisasjonsnummer, til = organisasjonsnummer),
                        fom = muligEndring(fra = periode.fom, til = fom),
                        tom = muligEndring(fra = periode.tom, til = tom),
                        periodebeløp = muligEndring(fra = this.periodebeløp, til = periodebeløp),
                        dager = muligEndring(fra = this.dager, til = dager.toSortedSet()),
                    ),
            ),
        )
    }

    fun fjern(
        saksbehandlerIdent: String,
        notatTilBeslutter: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ) {
        apply(
            TilkommenInntektFjernetEvent(
                TilkommenInntektEvent.Metadata(
                    tilkommenInntektId = id(),
                    sekvensnummer = versjon + 1,
                    tidspunkt = Instant.now(),
                    utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                    notatTilBeslutter = notatTilBeslutter,
                    totrinnsvurderingId = totrinnsvurderingId,
                ),
            ),
        )
    }

    fun gjenopprett(
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        dager: Set<LocalDate>,
        saksbehandlerIdent: String,
        notatTilBeslutter: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ) {
        apply(
            TilkommenInntektGjenopprettetEvent(
                TilkommenInntektEvent.Metadata(
                    tilkommenInntektId = id(),
                    sekvensnummer = versjon + 1,
                    tidspunkt = Instant.now(),
                    utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                    notatTilBeslutter = notatTilBeslutter,
                    totrinnsvurderingId = totrinnsvurderingId,
                ),
                endringer =
                    TilkommenInntektEvent.Endringer(
                        organisasjonsnummer = muligEndring(fra = this.organisasjonsnummer, til = organisasjonsnummer),
                        fom = muligEndring(fra = periode.fom, til = fom),
                        tom = muligEndring(fra = periode.tom, til = tom),
                        periodebeløp = muligEndring(fra = this.periodebeløp, til = periodebeløp),
                        dager = muligEndring(fra = this.dager, til = dager.toSortedSet()),
                    ),
            ),
        )
    }

    private fun <T> muligEndring(
        fra: T,
        til: T,
    ): Endring<T>? = if (fra != til) Endring(fra = fra, til = til) else null

    private fun apply(event: TilkommenInntektEvent) {
        håndterEvent(event)
        when (event) {
            is TilkommenInntektOpprettetEvent -> error("Kan ikke håndtere opphavsevent inni et eksisterende objekt")

            is TilkommenInntektEndretEvent -> {
                håndterEndringer(event.endringer)
            }

            is TilkommenInntektFjernetEvent -> {
                if (fjernet) error("Prøvde å fjerne tilkommen inntekt som allerede var fjernet!")
                fjernet = true
            }

            is TilkommenInntektGjenopprettetEvent -> {
                if (!fjernet) error("Prøvde å gjenopprette tilkommen inntekt som ikke var fjernet!")
                fjernet = false
                håndterEndringer(event.endringer)
            }
        }
    }

    private fun håndterEvent(event: TilkommenInntektEvent) {
        if (event.metadata.sekvensnummer != this.versjon + 1) {
            error(
                "Fikk events ute av rekkefølge: $versjon -> ${event.metadata.sekvensnummer}",
            )
        }
        this.totrinnsvurderingId = event.metadata.totrinnsvurderingId
        this.versjon = event.metadata.sekvensnummer
        this._events.add(event)
    }

    private fun håndterEndringer(endringer: TilkommenInntektEvent.Endringer) {
        håndterEndring(endringer.organisasjonsnummer, this::organisasjonsnummer)
        håndterEndring(endringer.fom, periode.fom) { periode = periode.copy(fom = it) }
        håndterEndring(endringer.tom, periode.tom) { periode = periode.copy(tom = it) }
        håndterEndring(endringer.periodebeløp, this::periodebeløp)
        håndterEndring(endringer.dager, this::dager)
    }

    private fun <T> håndterEndring(
        endring: Endring<T>?,
        prop: KMutableProperty0<T>,
    ) {
        if (endring != null) {
            if (endring.fra != prop.get()) {
                error("Fikk event med endring med feil fra-verdi for ${prop.name}!")
            } else {
                prop.set(endring.til)
            }
        }
    }

    private fun <T> håndterEndring(
        endring: Endring<T>?,
        currentValue: T,
        updater: (T) -> Unit,
    ) {
        if (endring != null) {
            if (endring.fra != currentValue) {
                error("Fikk event med endring med feil fra-verdi!")
            } else {
                updater(endring.til)
            }
        }
    }

    companion object {
        fun ny(
            fødselsnummer: String,
            saksbehandlerIdent: String,
            notatTilBeslutter: String,
            totrinnsvurderingId: TotrinnsvurderingId,
            organisasjonsnummer: String,
            fom: LocalDate,
            tom: LocalDate,
            periodebeløp: BigDecimal,
            dager: Set<LocalDate>,
        ) = TilkommenInntekt(
            TilkommenInntektOpprettetEvent(
                TilkommenInntektEvent.Metadata(
                    tilkommenInntektId = TilkommenInntektId.ny(fødselsnummer),
                    sekvensnummer = 1,
                    tidspunkt = Instant.now(),
                    utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                    notatTilBeslutter = notatTilBeslutter,
                    totrinnsvurderingId = totrinnsvurderingId,
                ),
                organisasjonsnummer = organisasjonsnummer,
                periode = Periode(fom, tom),
                periodebeløp = periodebeløp,
                dager = dager.toSortedSet(),
            ),
        )

        fun validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
            fom: LocalDate,
            tom: LocalDate,
            organisasjonsnummer: String,
            alleTilkomneInntekterForFødselsnummer: List<TilkommenInntekt>,
        ) {
            val alleTilkomneInntekterForInntektskilde =
                alleTilkomneInntekterForFødselsnummer.filter { it.organisasjonsnummer == organisasjonsnummer }
            if (alleTilkomneInntekterForInntektskilde.any {
                    it.periode overlapper
                        Periode(
                            fom = fom,
                            tom = tom,
                        )
                }
            ) {
                error("Kan ikke legge til tilkommen inntekt som overlapper med en annen tilkommen inntekt for samme inntektskilde")
            }
        }

        fun fraLagring(events: List<TilkommenInntektEvent>): TilkommenInntekt =
            TilkommenInntekt(events.first() as TilkommenInntektOpprettetEvent)
                .also { tilkommenInntekt -> events.drop(1).forEach(tilkommenInntekt::apply) }
    }
}
