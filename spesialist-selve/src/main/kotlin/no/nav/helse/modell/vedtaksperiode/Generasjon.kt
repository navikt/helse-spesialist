package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.avvisAlleFor
import no.nav.helse.modell.varsel.Varsel.Companion.deaktiverFor
import no.nav.helse.modell.varsel.Varsel.Companion.flyttVarslerFor
import no.nav.helse.modell.varsel.Varsel.Companion.godkjennAlleFor
import no.nav.helse.modell.varsel.Varsel.Companion.godkjennFor
import no.nav.helse.modell.varsel.Varsel.Companion.reaktiverFor
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varselkode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class Generasjon(
    private val id: UUID,
    private val vedtaksperiodeId: UUID,
    private var utbetalingId: UUID?,
    private var låst: Boolean,
    private var skjæringstidspunkt: LocalDate?,
    private var periode: Periode?,
    varsler: Set<Varsel>
) {
    internal constructor(
        id: UUID,
        vedtaksperiodeId: UUID
    ): this(id, vedtaksperiodeId, null, false, null, null, emptySet())

    private val varsler: MutableList<Varsel> = varsler.toMutableList()
    private val observers = mutableSetOf<IVedtaksperiodeObserver>()

    internal fun registrer(vararg observer: IVedtaksperiodeObserver) {
        observers.addAll(observer)
    }

    internal fun tilhører(dato: LocalDate): Boolean = periode?.let { it.tom() <= dato } ?: false

    internal fun harAktiveVarsler(): Boolean {
        return varsler.any { it.erAktiv() }
    }

    internal fun håndterTidslinjeendring(fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
        if (låst) return
        if (fom == periode?.fom() && tom == periode?.tom() && skjæringstidspunkt == this.skjæringstidspunkt) return
        this.periode = Periode(fom, tom)
        this.skjæringstidspunkt = skjæringstidspunkt
        observers.forEach {
            it.tidslinjeOppdatert(id, fom, tom, skjæringstidspunkt)
        }
    }

    internal fun håndterNyGenerasjon(
        varselRepository: VarselRepository,
        hendelseId: UUID,
        id: UUID = UUID.randomUUID(),
    ): Generasjon? {
        if (!låst) return null
        val nesteGenerasjon = opprettNeste(id, hendelseId)
        flyttAktiveVarsler(nesteGenerasjon, varselRepository)
        return nesteGenerasjon
    }

    private fun opprett(hendelseId: UUID) {
        observers.forEach {
            it.generasjonOpprettet(id, vedtaksperiodeId, hendelseId, periode?.fom(), periode?.tom(), skjæringstidspunkt)
        }
    }

    private fun flyttAktiveVarsler(nyGenerasjon: Generasjon, varselRepository: VarselRepository) {
        val aktiveVarsler = varsler.filter(Varsel::erAktiv)
        aktiveVarsler.flyttVarslerFor(this.id, nyGenerasjon.id, varselRepository)
        this.varsler.removeAll(aktiveVarsler)
        nyGenerasjon.varsler.addAll(aktiveVarsler)
        if (aktiveVarsler.isNotEmpty())
            sikkerlogg.info(
                "Flytter ${aktiveVarsler.size} varsler fra {} til {}. Gammel generasjon har {}",
                kv("gammel_generasjon", this.id),
                kv("ny_generasjon", nyGenerasjon.id),
                kv("utbetalingId", this.utbetalingId),
            )
    }

    internal fun håndterNyUtbetaling(hendelseId: UUID, utbetalingId: UUID, varselRepository: VarselRepository) {
        if (!låst) return håndterNyUtbetaling(utbetalingId)
        val nyGenerasjonId = UUID.randomUUID()
        sikkerlogg.info(
            "Kan ikke legge til ny utbetaling med {} for {}, da generasjonen er låst. Oppretter ny generasjon med {}",
            keyValue("utbetalingId", utbetalingId),
            keyValue("generasjon", this),
            keyValue("generasjonId", nyGenerasjonId)
        )
        håndterNyGenerasjon(varselRepository, hendelseId, nyGenerasjonId)?.håndterNyUtbetaling(utbetalingId)
    }

    private fun håndterNyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
        observers.forEach { it.nyUtbetaling(id, utbetalingId) }
    }

    internal fun invaliderUtbetaling(utbetalingId: UUID) {
        if (utbetalingId != this.utbetalingId) return sikkerlogg.error(
                "{} sin utbetalingId samsvarer ikke med den forkastede utbetalingIden {}. Dette skal ikke kunne skje",
                keyValue("Generasjon", this),
                keyValue("utbetalingId", utbetalingId)
            )
        if (låst) return sikkerlogg.error(
            "{} er låst. Utbetaling med {} forsøkt forkastet",
            keyValue("Generasjon", this),
            keyValue("utbetalingId", utbetalingId)
        )
        this.utbetalingId = null
        observers.forEach { it.utbetalingForkastet(id, utbetalingId) }
    }

    internal fun håndterRegelverksvarsel(hendelseId: UUID, varselId: UUID, varselkode: String, opprettet: LocalDateTime, varselRepository: VarselRepository) {
        if (!låst) return håndterVarsel(varselId, varselkode, opprettet, varselRepository)
        val nyGenerasjon = håndterNyGenerasjon(varselRepository = varselRepository, hendelseId = hendelseId)
            ?: throw IllegalStateException("Forventer å kunne opprette ny generasjon da gjeldende generasjon = $this er låst.")
        nyGenerasjon.håndterRegelverksvarsel(hendelseId, varselId, varselkode, opprettet, varselRepository)
        sikkerlogg.info(
            "Oppretter ny {} for {} som følge av nytt varsel {}, {}",
            keyValue("generasjon", nyGenerasjon),
            keyValue("vedtaksperiodeId", nyGenerasjon.vedtaksperiodeId),
            keyValue("varselId", varselId),
            keyValue("varselkode", varselkode)
        )
    }

    internal fun håndterSaksbehandlingsvarsel(varselId: UUID, varselkode: Varselkode, opprettet: LocalDateTime, varselRepository: VarselRepository) {
        if (låst) {
            throw IllegalStateException("Forsøker å håndtere varselkode = $varselkode for generasjon = $this som er låst. Det skal ikke være mulig.")
        }
        håndterVarsel(varselId, varselkode.name, opprettet, varselRepository)
    }

    private fun håndterVarsel(varselId: UUID, varselkode: String, opprettet: LocalDateTime, varselRepository: VarselRepository) {
        varsler.reaktiverFor(this.id, varselkode, varselRepository) ?: run {
            varsler.add(Varsel(varselId, varselkode, opprettet, vedtaksperiodeId))
            varselRepository.lagreVarsel(varselId, this.id, varselkode, opprettet, vedtaksperiodeId)
        }
    }

    internal fun håndterGodkjentVarsel(varselkode: String, ident: String, varselRepository: VarselRepository) {
        varsler.godkjennFor(id, varselkode, ident, varselRepository)
    }

    internal fun håndterDeaktivertVarsel(varselkode: String, varselRepository: VarselRepository) {
        varsler.deaktiverFor(id, varselkode, varselRepository)
    }

    internal fun håndterGodkjentAvSaksbehandler(ident: String, varselRepository: VarselRepository) {
        varsler.godkjennAlleFor(id, ident, varselRepository)
    }

    internal fun håndterAvvistAvSaksbehandler(ident: String, varselRepository: VarselRepository) {
        varsler.avvisAlleFor(id, ident, varselRepository)
    }

    internal fun håndterVedtakFattet(hendelseId: UUID) {
        if (låst) return sikkerlogg.warn(
            "Siste {} er allerede låst. Forsøkt låst av {}",
            keyValue("generasjon", this),
            keyValue("hendelseId", hendelseId)
        )
        låst = true
        observers.forEach { it.vedtakFattet(id, hendelseId) }
    }

    internal fun opprettFørste(hendelseId: UUID) {
        observers.forEach { it.førsteGenerasjonOpprettet(id, vedtaksperiodeId, hendelseId, periode?.fom(), periode?.tom(), skjæringstidspunkt) }
    }
    override fun toString(): String = "generasjonId=$id, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, låst=$låst, skjæringstidspunkt=$skjæringstidspunkt, periode=$periode"

    override fun equals(other: Any?): Boolean =
        this === other || (other is Generasjon
                && javaClass == other.javaClass
                && id == other.id
                && vedtaksperiodeId == other.vedtaksperiodeId
                && utbetalingId == other.utbetalingId
                && låst == other.låst
                && skjæringstidspunkt == other.skjæringstidspunkt
                && periode == other.periode)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + utbetalingId.hashCode()
        result = 31 * result + låst.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + periode.hashCode()
        return result
    }

    internal companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

        private fun Generasjon.opprettNeste(generasjonId: UUID, hendelseId: UUID): Generasjon {
            val nyGenerasjon = Generasjon(generasjonId, this.vedtaksperiodeId, null, false, this.skjæringstidspunkt, this.periode, emptySet())
            nyGenerasjon.registrer(*this.observers.toTypedArray())
            nyGenerasjon.opprett(hendelseId)

            return nyGenerasjon
        }

        internal fun opprettFørste(vedtaksperiodeId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate): Generasjon {
            return Generasjon(
                UUID.randomUUID(),
                vedtaksperiodeId,
                null,
                false,
                skjæringstidspunkt,
                Periode(fom, tom),
                emptySet()
            )
        }
    }
}
