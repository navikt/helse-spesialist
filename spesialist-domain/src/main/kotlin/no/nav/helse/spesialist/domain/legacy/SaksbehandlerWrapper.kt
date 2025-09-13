package no.nav.helse.spesialist.domain.legacy

import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.OpphevStans
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.domain.Saksbehandler

class SaksbehandlerWrapper(
    val saksbehandler: Saksbehandler,
) {
    private val observers = mutableListOf<SaksbehandlerObserver>()

    fun register(observer: SaksbehandlerObserver) {
        observers.add(observer)
    }

    internal fun håndter(hendelse: OverstyrtTidslinje) {
        val event = hendelse.byggEvent()
        val subsumsjoner = hendelse.byggSubsumsjoner(saksbehandler.epost).map { it.byggEvent() }
        subsumsjoner.forEach { subsumsjonEvent ->
            observers.forEach { it.nySubsumsjon(subsumsjonEvent.fødselsnummer, subsumsjonEvent) }
        }
        observers.forEach { it.tidslinjeOverstyrt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: OverstyrtInntektOgRefusjon) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id().value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident,
            )
        observers.forEach { it.inntektOgRefusjonOverstyrt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: OverstyrtArbeidsforhold) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id().value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident,
            )
        observers.forEach { it.arbeidsforholdOverstyrt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: SkjønnsfastsattSykepengegrunnlag) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id().value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident,
            )
        val subsumsjonEvent = hendelse.byggSubsumsjon(saksbehandler.epost).byggEvent()
        observers.forEach { it.nySubsumsjon(subsumsjonEvent.fødselsnummer, subsumsjonEvent) }
        observers.forEach { it.sykepengegrunnlagSkjønnsfastsatt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: MinimumSykdomsgrad) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id().value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident,
            )
        val subsumsjoner = hendelse.byggSubsumsjoner(saksbehandler.epost).map { it.byggEvent() }
        subsumsjoner.forEach { subsumsjonEvent ->
            observers.forEach { it.nySubsumsjon(subsumsjonEvent.fødselsnummer, subsumsjonEvent) }
        }
        observers.forEach { it.minimumSykdomsgradVurdert(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: Annullering) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id().value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident,
            )
        observers.forEach { it.utbetalingAnnullert(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: LeggPåVent) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id().value,
                ident = saksbehandler.ident,
            )
        observers.forEach { it.lagtPåVent(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: EndrePåVent) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id().value,
                ident = saksbehandler.ident,
            )
        observers.forEach { it.lagtPåVent(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: OpphevStans) {
        // TODO: Sende melding på kafka kanskje? Vil iSyfo vite at stans er opphevet?
        return
    }

    override fun toString(): String = "epostadresse=${saksbehandler.epost}, oid=${saksbehandler.id().value}"

    override fun equals(other: Any?) =
        this === other ||
            other is SaksbehandlerWrapper &&
            saksbehandler.epost == other.saksbehandler.epost &&
            saksbehandler.navn == other.saksbehandler.navn &&
            saksbehandler.id().value == other.saksbehandler.id().value &&
            saksbehandler.ident == other.saksbehandler.ident

    override fun hashCode(): Int {
        var result = saksbehandler.epost.hashCode()
        result = 31 * result + saksbehandler.id().value.hashCode()
        result = 31 * result + saksbehandler.navn.hashCode()
        result = 31 * result + saksbehandler.ident.hashCode()
        return result
    }
}
