package no.nav.helse.modell.saksbehandler

import java.util.UUID
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.api.modell.SaksbehandlerObserver
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

class Saksbehandler(
    private val epostadresse: String,
    private val oid: UUID,
    private val navn: String,
    private val ident: String
) {
    private val observers = mutableListOf<SaksbehandlerObserver>()

    fun register(observer: SaksbehandlerObserver) {
        observers.add(observer)
    }

    fun ident(): String = ident
    fun oid(): UUID = oid

    fun persister(saksbehandlerDao: SaksbehandlerDao) {
        saksbehandlerDao.opprettSaksbehandler(oid = oid, navn = navn, epost = epostadresse, ident = ident)
    }

    internal fun håndter(hendelse: OverstyrtTidslinje) {
        val event = hendelse.byggEvent(oid, navn, epostadresse, ident)
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
        observers.forEach { it.sykepengegrunnlagSkjønnsfastsatt(event.fødselsnummer, event) }
    }

    internal fun håndter(hendelse: Annullering) {
        val event = hendelse.byggEvent(oid, navn, epostadresse, ident)
        observers.forEach { it.utbetalingAnnullert(event.fødselsnummer, event) }
    }

    fun toDto() = SaksbehandlerFraApi(oid = oid, navn = navn, epost = epostadresse, ident = ident)

    override fun toString(): String = "epostadresse=$epostadresse, oid=$oid"

    override fun equals(other: Any?) = this === other || (
        other is Saksbehandler &&
        epostadresse == other.epostadresse &&
        navn == other.navn &&
        oid == other.oid &&
        ident == other.ident
    )

    override fun hashCode(): Int {
        var result = epostadresse.hashCode()
        result = 31 * result + oid.hashCode()
        result = 31 * result + navn.hashCode()
        result = 31 * result + ident.hashCode()
        return result
    }
}