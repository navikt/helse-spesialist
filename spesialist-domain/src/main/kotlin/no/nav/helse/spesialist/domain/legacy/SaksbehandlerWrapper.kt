package no.nav.helse.spesialist.domain.legacy

import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
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
                oid = saksbehandler.id.value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident.value,
            )
        observers.forEach { it.inntektOgRefusjonOverstyrt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: OverstyrtArbeidsforhold) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id.value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident.value,
            )
        observers.forEach { it.arbeidsforholdOverstyrt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: SkjønnsfastsattSykepengegrunnlag) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id.value,
                navn = saksbehandler.navn,
                epost = saksbehandler.epost,
                ident = saksbehandler.ident.value,
            )
        val subsumsjonEvent = hendelse.byggSubsumsjon(saksbehandler.epost).byggEvent()
        observers.forEach { it.nySubsumsjon(subsumsjonEvent.fødselsnummer, subsumsjonEvent) }
        observers.forEach { it.sykepengegrunnlagSkjønnsfastsatt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: LeggPåVent) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id.value,
                ident = saksbehandler.ident.value,
            )
        observers.forEach { it.lagtPåVent(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: EndrePåVent) {
        val event =
            hendelse.byggEvent(
                oid = saksbehandler.id.value,
                ident = saksbehandler.ident.value,
            )
        observers.forEach { it.lagtPåVent(event.fødselsnummer, event) }
    }

    override fun toString(): String = "epostadresse=${saksbehandler.epost}, oid=${saksbehandler.id.value}"

    override fun equals(other: Any?) =
        this === other ||
            other is SaksbehandlerWrapper &&
            saksbehandler.epost == other.saksbehandler.epost &&
            saksbehandler.navn == other.saksbehandler.navn &&
            saksbehandler.id.value == other.saksbehandler.id.value &&
            saksbehandler.ident.value == other.saksbehandler.ident.value

    override fun hashCode(): Int {
        var result = saksbehandler.epost.hashCode()
        result = 31 * result + saksbehandler.id.value.hashCode()
        result = 31 * result + saksbehandler.navn.hashCode()
        result = 31 * result + saksbehandler.ident.value.hashCode()
        return result
    }
}
