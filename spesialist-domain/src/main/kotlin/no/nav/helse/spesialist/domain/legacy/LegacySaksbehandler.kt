package no.nav.helse.spesialist.domain.legacy

import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
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
import java.util.UUID

class LegacySaksbehandler(
    val epostadresse: String,
    val oid: UUID,
    val navn: String,
    private val ident: String,
) {
    private val observers = mutableListOf<SaksbehandlerObserver>()

    fun register(observer: SaksbehandlerObserver) {
        observers.add(observer)
    }

    fun ident(): String = ident

    fun oid(): UUID = oid

    fun epostadresse(): String = epostadresse

    internal fun håndter(hendelse: OverstyrtTidslinje) {
        val event = hendelse.byggEvent()
        val subsumsjoner = hendelse.byggSubsumsjoner(this.epostadresse).map { it.byggEvent() }
        subsumsjoner.forEach { subsumsjonEvent ->
            observers.forEach { it.nySubsumsjon(subsumsjonEvent.fødselsnummer, subsumsjonEvent) }
        }
        observers.forEach { it.tidslinjeOverstyrt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: OverstyrtInntektOgRefusjon) {
        val event = hendelse.byggEvent(oid, navn, epostadresse, ident)
        observers.forEach { it.inntektOgRefusjonOverstyrt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: OverstyrtArbeidsforhold) {
        val event = hendelse.byggEvent(oid, navn, epostadresse, ident)
        observers.forEach { it.arbeidsforholdOverstyrt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: SkjønnsfastsattSykepengegrunnlag) {
        val event = hendelse.byggEvent(oid, navn, epostadresse, ident)
        val subsumsjonEvent = hendelse.byggSubsumsjon(this.epostadresse).byggEvent()
        observers.forEach { it.nySubsumsjon(subsumsjonEvent.fødselsnummer, subsumsjonEvent) }
        observers.forEach { it.sykepengegrunnlagSkjønnsfastsatt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: MinimumSykdomsgrad) {
        val event = hendelse.byggEvent(oid, navn, epostadresse, ident)
        val subsumsjoner = hendelse.byggSubsumsjoner(this.epostadresse).map { it.byggEvent() }
        subsumsjoner.forEach { subsumsjonEvent ->
            observers.forEach { it.nySubsumsjon(subsumsjonEvent.fødselsnummer, subsumsjonEvent) }
        }
        observers.forEach { it.minimumSykdomsgradVurdert(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: Annullering) {
        val event = hendelse.byggEvent(oid, navn, epostadresse, ident)
        observers.forEach { it.utbetalingAnnullert(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: LeggPåVent) {
        val event = hendelse.byggEvent(oid, ident)
        observers.forEach { it.lagtPåVent(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: EndrePåVent) {
        val event = hendelse.byggEvent(oid, ident)
        observers.forEach { it.lagtPåVent(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: OpphevStans) {
        // TODO: Sende melding på kafka kanskje? Vil iSyfo vite at stans er opphevet?
        return
    }

    override fun toString(): String = "epostadresse=$epostadresse, oid=$oid"

    override fun equals(other: Any?) =
        this === other ||
            other is LegacySaksbehandler &&
            epostadresse == other.epostadresse &&
            navn == other.navn &&
            oid == other.oid &&
            ident == other.ident

    override fun hashCode(): Int {
        var result = epostadresse.hashCode()
        result = 31 * result + oid.hashCode()
        result = 31 * result + navn.hashCode()
        result = 31 * result + ident.hashCode()
        return result
    }

    companion object {
        fun SaksbehandlerDto.gjenopprett(): LegacySaksbehandler =
            LegacySaksbehandler(
                epostadresse = epostadresse,
                oid = oid,
                navn = navn,
                ident = ident,
            )

        fun LegacySaksbehandler.toDto(): SaksbehandlerDto =
            SaksbehandlerDto(
                epostadresse = epostadresse,
                oid = oid,
                navn = navn,
                ident = ident,
            )

        fun no.nav.helse.spesialist.domain.Saksbehandler.tilLegacy(): LegacySaksbehandler =
            LegacySaksbehandler(
                epostadresse = epost,
                oid = id().value,
                navn = navn,
                ident = ident,
            )
    }
}
