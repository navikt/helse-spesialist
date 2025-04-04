package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
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
) : AggregateRoot<TilkommenInntektId>(opprettetEvent.tilkommenInntektId) {
    private val events: MutableList<TilkommenInntektEvent> = mutableListOf(opprettetEvent)

    fun alleEvents(): List<TilkommenInntektEvent> = events

    var organisasjonsnummer: String = opprettetEvent.organisasjonsnummer
        private set
    var periode: Periode = opprettetEvent.periode
        private set
    var periodebeløp: BigDecimal = opprettetEvent.periodebeløp
        private set
    var dager: Set<LocalDate> = opprettetEvent.dager
        private set
    var fjernet: Boolean = false
        private set
    var versjon: Int = opprettetEvent.sekvensNummer
        private set

    fun dagbeløp(): BigDecimal = periodebeløp.setScale(4).divide(dager.size.toBigDecimal(), RoundingMode.HALF_UP)

    fun endreTil(
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        dager: Set<LocalDate>,
        saksbehandlerOid: SaksbehandlerOid,
        notatTilBeslutter: String,
    ) {
        apply(
            TilkommenInntektEndretEvent(
                tilkommenInntektId = id(),
                sekvensNummer = versjon + 1,
                tidspunkt = Instant.now(),
                utførtAvSaksbehandlerOid = saksbehandlerOid,
                notatTilBeslutter = notatTilBeslutter,
                endringer =
                    TilkommenInntektEvent.Endringer(
                        organisasjonsnummer = muligEndring(fra = this.organisasjonsnummer, til = organisasjonsnummer),
                        periode = muligEndring(fra = periode, til = Periode(fom = fom, tom = tom)),
                        periodebeløp = muligEndring(fra = this.periodebeløp, til = periodebeløp),
                        dager = muligEndring(fra = this.dager, til = dager),
                    ),
            ),
        )
    }

    fun fjern(
        saksbehandlerOid: SaksbehandlerOid,
        notatTilBeslutter: String,
    ) {
        apply(
            TilkommenInntektFjernetEvent(
                tilkommenInntektId = id(),
                sekvensNummer = versjon + 1,
                tidspunkt = Instant.now(),
                utførtAvSaksbehandlerOid = saksbehandlerOid,
                notatTilBeslutter = notatTilBeslutter,
            ),
        )
    }

    fun gjenopprett(
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        dager: Set<LocalDate>,
        saksbehandlerOid: SaksbehandlerOid,
        notatTilBeslutter: String,
    ) {
        apply(
            TilkommenInntektGjenopprettetEvent(
                tilkommenInntektId = id(),
                sekvensNummer = versjon + 1,
                tidspunkt = Instant.now(),
                utførtAvSaksbehandlerOid = saksbehandlerOid,
                notatTilBeslutter = notatTilBeslutter,
                endringer =
                    TilkommenInntektEvent.Endringer(
                        organisasjonsnummer = muligEndring(fra = this.organisasjonsnummer, til = organisasjonsnummer),
                        periode = muligEndring(fra = periode, til = Periode(fom = fom, tom = tom)),
                        periodebeløp = muligEndring(fra = this.periodebeløp, til = periodebeløp),
                        dager = muligEndring(fra = this.dager, til = dager),
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
        if (event.sekvensNummer != this.versjon + 1) error("Fikk events ute av rekkefølge: $versjon -> ${event.sekvensNummer}")
        this.versjon = event.sekvensNummer
        this.events.add(event)
    }

    private fun håndterEndringer(endringer: TilkommenInntektEvent.Endringer) {
        håndterEndring(endringer.organisasjonsnummer, this::organisasjonsnummer)
        håndterEndring(endringer.periode, this::periode)
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

    companion object {
        fun ny(
            fødselsnummer: String,
            saksbehandlerOid: SaksbehandlerOid,
            notatTilBeslutter: String,
            organisasjonsnummer: String,
            fom: LocalDate,
            tom: LocalDate,
            periodebeløp: BigDecimal,
            dager: Set<LocalDate>,
        ) = TilkommenInntekt(
            TilkommenInntektOpprettetEvent(
                tilkommenInntektId = TilkommenInntektId.ny(fødselsnummer),
                sekvensNummer = 1,
                tidspunkt = Instant.now(),
                utførtAvSaksbehandlerOid = saksbehandlerOid,
                notatTilBeslutter = notatTilBeslutter,
                organisasjonsnummer = organisasjonsnummer,
                periode = Periode(fom, tom),
                periodebeløp = periodebeløp,
                dager = dager,
            ),
        )

        fun fraLagring(events: List<TilkommenInntektEvent>): TilkommenInntekt =
            TilkommenInntekt(events.first() as TilkommenInntektOpprettetEvent)
                .also { tilkommenInntekt -> events.drop(1).forEach(tilkommenInntekt::apply) }
    }
}
