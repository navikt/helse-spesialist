package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.util.*
import kotlin.reflect.KMutableProperty0

@JvmInline
value class GraderteAndreYtelserId(
    val value: UUID,
) : ValueObject

class GraderteAndreYtelser private constructor(
    opprettetEvent: GraderteAndreYtelserOpprettetEvent,
) : AggregateRoot<GraderteAndreYtelserId>(opprettetEvent.metadata.graderteAndreYtelserId) {
    private val _events: MutableList<GraderteAndreYtelserEvent> = mutableListOf(opprettetEvent)
    val events: List<GraderteAndreYtelserEvent> get() = _events

    val identitetsnummer: Identitetsnummer = Identitetsnummer.fraString(opprettetEvent.fødselsnummer)
    var totrinnsvurderingId: TotrinnsvurderingId = opprettetEvent.metadata.totrinnsvurderingId
        private set
    var perioder: List<GraderteAndreYtelserPeriode> = opprettetEvent.graderteAndreYtelserPerioder
        private set
    var graderteAndreYtelserType: GraderteAndreYtelserType = opprettetEvent.graderteAndreYtelserType
        private set
    var fjernet: Boolean = false
        private set
    var versjon: Int = opprettetEvent.metadata.sekvensnummer
        private set

    fun endreTil(
        graderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
        graderteAndreYtelserType: GraderteAndreYtelserType,
        saksbehandlerIdent: NAVIdent,
        notatTilBeslutter: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ) {
        val perioderEndring = muligEndring(fra = this.perioder, til = graderteAndreYtelserPerioder)
        val typeEndring = muligEndring(fra = this.graderteAndreYtelserType, til = graderteAndreYtelserType)
        if (perioderEndring != null || typeEndring != null) {
            apply(
                GraderteAndreYtelserEndretEvent(
                    metadata =
                        GraderteAndreYtelserEvent.Metadata(
                            graderteAndreYtelserId = id,
                            sekvensnummer = versjon + 1,
                            tidspunkt = Instant.now(),
                            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                            notatTilBeslutter = notatTilBeslutter,
                            totrinnsvurderingId = totrinnsvurderingId,
                        ),
                    endringer =
                        GraderteAndreYtelserEvent.Endringer(
                            graderteAndreYtelserPerioder = perioderEndring,
                            graderteAndreYtelserType = typeEndring,
                        ),
                ),
            )
        }
    }

    fun fjern(
        saksbehandlerIdent: NAVIdent,
        notatTilBeslutter: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ) {
        apply(
            GraderteAndreYtelserFjernetEvent(
                metadata =
                    GraderteAndreYtelserEvent.Metadata(
                        graderteAndreYtelserId = id,
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
        graderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
        graderteAndreYtelserType: GraderteAndreYtelserType,
        saksbehandlerIdent: NAVIdent,
        notatTilBeslutter: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ) {
        apply(
            GraderteAndreYtelserGjenopprettetEvent(
                metadata =
                    GraderteAndreYtelserEvent.Metadata(
                        graderteAndreYtelserId = id,
                        sekvensnummer = versjon + 1,
                        tidspunkt = Instant.now(),
                        utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                        notatTilBeslutter = notatTilBeslutter,
                        totrinnsvurderingId = totrinnsvurderingId,
                    ),
                endringer =
                    GraderteAndreYtelserEvent.Endringer(
                        graderteAndreYtelserPerioder = muligEndring(fra = this.perioder, til = graderteAndreYtelserPerioder),
                        graderteAndreYtelserType = muligEndring(fra = this.graderteAndreYtelserType, til = graderteAndreYtelserType),
                    ),
            ),
        )
    }

    private fun <T> muligEndring(
        fra: T,
        til: T,
    ): Endring<T>? = if (fra != til) Endring(fra = fra, til = til) else null

    private fun apply(event: GraderteAndreYtelserEvent) {
        håndterEvent(event)
        when (event) {
            is GraderteAndreYtelserOpprettetEvent -> {
                error("Kan ikke håndtere opphavsevent inni et eksisterende objekt")
            }

            is GraderteAndreYtelserEndretEvent -> {
                håndterEndringer(event.endringer)
            }

            is GraderteAndreYtelserFjernetEvent -> {
                if (fjernet) error("Prøvde å fjerne graderte andre ytelser som allerede var fjernet!")
                fjernet = true
            }

            is GraderteAndreYtelserGjenopprettetEvent -> {
                if (!fjernet) error("Prøvde å gjenopprette graderte andre ytelser som ikke var fjernet!")
                fjernet = false
                håndterEndringer(event.endringer)
            }
        }
    }

    private fun håndterEvent(event: GraderteAndreYtelserEvent) {
        if (event.metadata.sekvensnummer != this.versjon + 1) {
            error("Fikk events ute av rekkefølge: $versjon -> ${event.metadata.sekvensnummer}")
        }
        this.totrinnsvurderingId = event.metadata.totrinnsvurderingId
        this.versjon = event.metadata.sekvensnummer
        this._events.add(event)
    }

    private fun håndterEndringer(endringer: GraderteAndreYtelserEvent.Endringer) {
        håndterEndring(endringer.graderteAndreYtelserPerioder, this::perioder)
        håndterEndring(endringer.graderteAndreYtelserType, this::graderteAndreYtelserType)
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
        fun fraLagring(events: List<GraderteAndreYtelserEvent>): GraderteAndreYtelser =
            GraderteAndreYtelser(events.first() as GraderteAndreYtelserOpprettetEvent)
                .also { graderteAndreYtelser -> events.drop(1).forEach(graderteAndreYtelser::apply) }

        fun ny(
            identitetsnummer: Identitetsnummer,
            saksbehandlerIdent: NAVIdent,
            notatTilBeslutter: String,
            totrinnsvurderingId: TotrinnsvurderingId,
            graderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
            graderteAndreYtelserType: GraderteAndreYtelserType,
        ) = GraderteAndreYtelser(
            GraderteAndreYtelserOpprettetEvent(
                metadata =
                    GraderteAndreYtelserEvent.Metadata(
                        graderteAndreYtelserId = GraderteAndreYtelserId(UUID.randomUUID()),
                        sekvensnummer = 1,
                        tidspunkt = Instant.now(),
                        utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                        notatTilBeslutter = notatTilBeslutter,
                        totrinnsvurderingId = totrinnsvurderingId,
                    ),
                fødselsnummer = identitetsnummer.value,
                graderteAndreYtelserPerioder = graderteAndreYtelserPerioder,
                graderteAndreYtelserType = graderteAndreYtelserType,
            ),
        )
    }
}

enum class GraderteAndreYtelserType {
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    OMSORGSPENGER,
    PLEIEPENGER,
    OPPLARINGSPENGER,
}
