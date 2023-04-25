package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.avvisAlleFor
import no.nav.helse.modell.varsel.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.varsel.Varsel.Companion.flyttVarslerFor
import no.nav.helse.modell.varsel.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.varsel.Varsel.Companion.godkjennAlleFor
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderMedlemskapsvarsel
import org.slf4j.LoggerFactory

internal class Generasjon private constructor(
    private val id: UUID,
    private val vedtaksperiodeId: UUID,
    private var utbetalingId: UUID?,
    private var låst: Boolean,
    private var skjæringstidspunkt: LocalDate,
    private var periode: Periode,
    private var tilstand: Tilstand,
    varsler: Set<Varsel>
) {
    internal constructor(
        id: UUID,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate
    ): this(id, vedtaksperiodeId, null, false, skjæringstidspunkt, Periode(fom, tom), Ulåst, emptySet())

    private val varsler: MutableList<Varsel> = varsler.toMutableList()
    private val observers = mutableSetOf<IVedtaksperiodeObserver>()

    private fun nyTilstand(gammel: Tilstand, ny: Tilstand, hendelseId: UUID) {
        observers.forEach { it.tilstandEndret(id, vedtaksperiodeId, gammel, ny, hendelseId) }
        this.tilstand = ny
    }

    internal fun registrer(vararg observer: IVedtaksperiodeObserver) {
        observers.addAll(observer)
        varsler.forEach { it.registrer(*observer) }
    }

    internal fun tilhører(dato: LocalDate): Boolean = periode.tom() <= dato

    internal fun forhindrerAutomatisering(): Boolean {
        return varsler.forhindrerAutomatisering()
    }

    internal fun håndterTidslinjeendring(fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate, hendelseId: UUID) {
        if (fom == periode.fom() && tom == periode.tom() && skjæringstidspunkt == this.skjæringstidspunkt) return
        tilstand.tidslinjeendring(this, fom, tom, skjæringstidspunkt, hendelseId)
    }

    internal fun håndterVedtaksperiodeEndret(
        hendelseId: UUID,
        id: UUID = UUID.randomUUID(),
        fom: LocalDate = this.periode.fom(),
        tom: LocalDate = this.periode.tom(),
        skjæringstidspunkt: LocalDate = this.skjæringstidspunkt
    ): Generasjon? {
        return tilstand.vedtaksperiodeEndret(this, id, hendelseId, fom, tom, skjæringstidspunkt)
    }

    private fun nyGenerasjon(
        hendelseId: UUID,
        id: UUID = UUID.randomUUID(),
        fom: LocalDate = this.periode.fom(),
        tom: LocalDate = this.periode.tom(),
        skjæringstidspunkt: LocalDate = this.skjæringstidspunkt
    ): Generasjon {
        val nesteGenerasjon = opprettNeste(id, hendelseId, fom, tom, skjæringstidspunkt)
        flyttAktiveVarsler(nesteGenerasjon)
        return nesteGenerasjon
    }

    private fun oppdaterTidslinje(fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
        this.periode = Periode(fom, tom)
        this.skjæringstidspunkt = skjæringstidspunkt
        observers.forEach {
            it.tidslinjeOppdatert(id, fom, tom, skjæringstidspunkt)
        }
    }

    private fun opprett(hendelseId: UUID) {
        observers.forEach {
            it.generasjonOpprettet(id, vedtaksperiodeId, hendelseId, periode.fom(), periode.tom(), skjæringstidspunkt, tilstand)
        }
    }

    private fun flyttAktiveVarsler(nyGenerasjon: Generasjon) {
        val aktiveVarsler = varsler.filter(Varsel::erAktiv)
        aktiveVarsler.flyttVarslerFor(this.id, nyGenerasjon.id)
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

    internal fun håndterNyUtbetaling(hendelseId: UUID, utbetalingId: UUID) {
        tilstand.nyUtbetaling(this, hendelseId, utbetalingId)
    }

    private fun håndterNyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
        observers.forEach { it.nyUtbetaling(id, utbetalingId) }
    }

    internal fun invaliderUtbetaling(utbetalingId: UUID) {
        if (utbetalingId != this.utbetalingId) return
        tilstand.invaliderUtbetaling(this, utbetalingId)
    }

    internal fun håndterDeaktivertVarsel(varsel: Varsel) {
        val funnetVarsel = varsler.finnEksisterendeVarsel(varsel) ?: return
        funnetVarsel.deaktiver(id)
    }

    internal fun håndterGodkjentAvSaksbehandler(ident: String, hendelseId: UUID) {
        varsler.godkjennAlleFor(id, ident)
        tilstand.håndterGodkjenning(this, ident, hendelseId)
    }

    internal fun håndterAvvistAvSaksbehandler(ident: String) {
        varsler.avvisAlleFor(id, ident)
    }

    internal fun håndterVedtakFattet(hendelseId: UUID) {
        tilstand.vedtakFattet(this, hendelseId)
    }

    internal fun opprettFørste(hendelseId: UUID) {
        observers.forEach {
            it.førsteGenerasjonOpprettet(id, vedtaksperiodeId, hendelseId, periode.fom(), periode.tom(), skjæringstidspunkt, tilstand)
        }
    }

    internal fun håndter(varsel: Varsel, hendelseId: UUID) {
        if (!varsel.erRelevantFor(vedtaksperiodeId)) return
        val eksisterendeVarsel = varsler.finnEksisterendeVarsel(varsel) ?: return nyttVarsel(varsel, hendelseId)
        if (eksisterendeVarsel.erAktiv()) return
        eksisterendeVarsel.reaktiver(id)
    }

    private fun nyttVarsel(varsel: Varsel, hendelseId: UUID) {
        varsel.registrer(*this.observers.toTypedArray())
        varsler.add(varsel)
        varsel.opprett(id)
        tilstand.nyttVarsel(this, varsel, hendelseId)
    }

    override fun toString(): String = "generasjonId=$id, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, låst=$låst, skjæringstidspunkt=$skjæringstidspunkt, periode=$periode"

    override fun equals(other: Any?): Boolean =
        this === other || (other is Generasjon
                && javaClass == other.javaClass
                && id == other.id
                && vedtaksperiodeId == other.vedtaksperiodeId
                && utbetalingId == other.utbetalingId
                && låst == other.låst
                && tilstand == other.tilstand
                && skjæringstidspunkt == other.skjæringstidspunkt
                && periode == other.periode)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + utbetalingId.hashCode()
        result = 31 * result + låst.hashCode()
        result = 31 * result + tilstand.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + periode.hashCode()
        return result
    }

    private fun kreverTotrinnsvurdering(): Boolean {
        val inneholderMedlemskapsvarsel = varsler.inneholderMedlemskapsvarsel()
        logg.info("$this harMedlemskapsvarsel: $inneholderMedlemskapsvarsel")
        return inneholderMedlemskapsvarsel
    }

    internal sealed interface Tilstand {

        fun navn(): String
        fun vedtaksperiodeEndret(generasjon: Generasjon, id: UUID, hendelseId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate): Generasjon? {
            return null
        }

        fun vedtakFattet(generasjon: Generasjon, hendelseId: UUID) {
            sikkerlogg.info("Forventet ikke vedtak_fattet i {}", kv("tilstand", this::class.simpleName))
        }

        fun tidslinjeendring(
            generasjon: Generasjon,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            hendelseId: UUID
        ) {}

        fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {}
        fun invaliderUtbetaling(generasjon: Generasjon, utbetalingId: UUID) {
            sikkerlogg.error(
                "{} er i {}. Utbetaling med {} forsøkt forkastet",
                keyValue("tilstand", this::class.simpleName),
                keyValue("Generasjon", generasjon),
                keyValue("utbetalingId", utbetalingId)
            )
        }

        fun nyttVarsel(generasjon: Generasjon, varsel: Varsel, hendelseId: UUID) {}
        fun håndterGodkjenning(generasjon: Generasjon, ident: String, hendelseId: UUID) {}
    }

    internal object Låst: Tilstand {
        override fun navn(): String = "Låst"

        override fun vedtaksperiodeEndret(
            generasjon: Generasjon,
            id: UUID,
            hendelseId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate
        ): Generasjon {
            return generasjon.nyGenerasjon(hendelseId, id, fom, tom, skjæringstidspunkt)
        }

        override fun tidslinjeendring(
            generasjon: Generasjon,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            hendelseId: UUID
        ) {
            generasjon.nyGenerasjon(hendelseId = hendelseId, fom = fom, tom = tom, skjæringstidspunkt = skjæringstidspunkt)
        }

        override fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {
            val nyGenerasjonId = UUID.randomUUID()
            sikkerlogg.info(
                "Kan ikke legge til ny utbetaling med {} for {}, da generasjonen er låst. Oppretter ny generasjon med {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("generasjon", generasjon),
                keyValue("generasjonId", nyGenerasjonId)
            )
            generasjon.nyGenerasjon(hendelseId, nyGenerasjonId).håndterNyUtbetaling(utbetalingId)
        }
    }

    internal object Ulåst: Tilstand {
        override fun navn(): String = "Ulåst"

        override fun vedtakFattet(generasjon: Generasjon, hendelseId: UUID) {
            generasjon.låst = true
            if (generasjon.utbetalingId == null)
                return generasjon.nyTilstand(this, AvsluttetUtenUtbetaling, hendelseId)
            generasjon.nyTilstand(this, Låst, hendelseId)
        }

        override fun tidslinjeendring(
            generasjon: Generasjon,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            hendelseId: UUID
        ) {
            generasjon.oppdaterTidslinje(fom, tom, skjæringstidspunkt)
        }

        override fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {
            generasjon.håndterNyUtbetaling(utbetalingId)
        }

        override fun invaliderUtbetaling(generasjon: Generasjon, utbetalingId: UUID) {
            generasjon.utbetalingId = null
            generasjon.observers.forEach { it.utbetalingForkastet(generasjon.id, utbetalingId) }
        }
    }

    internal object AvsluttetUtenUtbetaling: Tilstand {
        override fun navn(): String = "AvsluttetUtenUtbetaling"

        override fun vedtaksperiodeEndret(
            generasjon: Generasjon,
            id: UUID,
            hendelseId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate
        ): Generasjon {
            return generasjon.nyGenerasjon(hendelseId, id, fom, tom, skjæringstidspunkt)
        }

        override fun tidslinjeendring(
            generasjon: Generasjon,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            hendelseId: UUID
        ) {
            generasjon.nyGenerasjon(hendelseId = hendelseId, fom = fom, tom = tom, skjæringstidspunkt = skjæringstidspunkt)
        }

        override fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {
            val nyGenerasjonId = UUID.randomUUID()
            sikkerlogg.info(
                "Kan ikke legge til ny utbetaling med {} for {}, da generasjonen er låst. Oppretter ny generasjon med {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("generasjon", generasjon),
                keyValue("generasjonId", nyGenerasjonId)
            )
            generasjon.nyGenerasjon(hendelseId, nyGenerasjonId).håndterNyUtbetaling(utbetalingId)
        }

        override fun nyttVarsel(generasjon: Generasjon, varsel: Varsel, hendelseId: UUID) {
            generasjon.nyTilstand(this, UtenUtbetalingMåVurderes, hendelseId)
        }
    }

    internal object UtenUtbetalingMåVurderes: Tilstand {
        override fun navn(): String = "UtenUtbetalingMåVurderes"

        override fun håndterGodkjenning(generasjon: Generasjon, ident: String, hendelseId: UUID) {
            generasjon.nyTilstand(this, AvsluttetUtenUtbetaling, hendelseId)
        }

        override fun vedtaksperiodeEndret(
            generasjon: Generasjon,
            id: UUID,
            hendelseId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate
        ): Generasjon {
            generasjon.nyTilstand(this, AvsluttetUtenUtbetaling, hendelseId)
            return generasjon.nyGenerasjon(hendelseId, id, fom, tom, skjæringstidspunkt)
        }

        override fun tidslinjeendring(
            generasjon: Generasjon,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            hendelseId: UUID
        ) {
            generasjon.nyTilstand(this, AvsluttetUtenUtbetaling, hendelseId)
            generasjon.nyGenerasjon(hendelseId = hendelseId, fom = fom, tom = tom, skjæringstidspunkt = skjæringstidspunkt)
        }

        override fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {
            val nyGenerasjonId = UUID.randomUUID()
            sikkerlogg.info(
                "Kan ikke legge til ny utbetaling med {} for {}, da generasjonen er låst. Oppretter ny generasjon med {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("generasjon", generasjon),
                keyValue("generasjonId", nyGenerasjonId)
            )
            generasjon.nyTilstand(this, AvsluttetUtenUtbetaling, hendelseId)
            generasjon.nyGenerasjon(hendelseId, nyGenerasjonId).håndterNyUtbetaling(utbetalingId)
        }
    }

    internal companion object {

        private val logg = LoggerFactory.getLogger(Generasjon::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private fun Generasjon.opprettNeste(generasjonId: UUID, hendelseId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate): Generasjon {
            val nyGenerasjon = opprett(generasjonId, this.vedtaksperiodeId, fom, tom, skjæringstidspunkt)
            nyGenerasjon.registrer(*this.observers.toTypedArray())
            nyGenerasjon.opprett(hendelseId)

            return nyGenerasjon
        }

        internal fun opprettFørste(vedtaksperiodeId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate): Generasjon {
            return opprett(
                id = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiodeId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
            )
        }

        private fun opprett(id: UUID, vedtaksperiodeId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate): Generasjon {
            return Generasjon(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
            )
        }

        internal fun fraLagring(
            id: UUID,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID?,
            låst: Boolean,
            skjæringstidspunkt: LocalDate,
            fom: LocalDate,
            tom: LocalDate,
            tilstand: Tilstand,
            varsler: Set<Varsel>
        ) = Generasjon(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            låst = låst,
            skjæringstidspunkt = skjæringstidspunkt,
            periode = Periode(fom, tom),
            tilstand = tilstand,
            varsler = varsler
        )

        internal fun List<Generasjon>.håndterOppdateringer(
            vedtaksperiodeoppdateringer: List<VedtaksperiodeOppdatering>,
            hendelseId: UUID
        ) {
            forEach { generasjon ->
                val oppdatering = vedtaksperiodeoppdateringer.find { it.vedtaksperiodeId == generasjon.vedtaksperiodeId } ?: return@forEach
                generasjon.håndterTidslinjeendring(oppdatering.fom, oppdatering.tom, oppdatering.skjæringstidspunkt, hendelseId)
            }
        }

        internal fun List<Generasjon>.håndter(varsler: List<Varsel>, hendelseId: UUID) {
            forEach { generasjon ->
                varsler.forEach { generasjon.håndter(it, hendelseId) }
            }
        }

        internal fun List<Generasjon>.forhindrerAutomatisering(tilOgMed: LocalDate): Boolean {
            return this.filter { it.tilhører(tilOgMed) }.any { it.forhindrerAutomatisering() }
        }

        internal fun List<Generasjon>.kreverTotrinnsvurdering(vedtaksperiodeId: UUID): Boolean {
            return overlapperMedEllerTidligereEnn(vedtaksperiodeId).any { it.kreverTotrinnsvurdering() }
        }

        internal fun List<Generasjon>.deaktiver(varsel: Varsel) {
            find { varsel.erRelevantFor(it.vedtaksperiodeId) }?.håndterDeaktivertVarsel(varsel)
        }

        internal fun List<Generasjon>.håndterGodkjent(
            saksbehandlerIdent: String,
            vedtaksperiodeId: UUID,
            hendelseId: UUID
        ) {
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).forEach {
                it.håndterGodkjentAvSaksbehandler(saksbehandlerIdent, hendelseId)
            }
        }

        internal fun List<Generasjon>.håndterAvvist(saksbehandlerIdent: String, vedtaksperiodeId: UUID) {
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).forEach {
                it.håndterAvvistAvSaksbehandler(saksbehandlerIdent)
            }
        }
        private fun List<Generasjon>.overlapperMedEllerTidligereEnn(vedtaksperiodeId: UUID): List<Generasjon> {
            val gjeldende = find { it.vedtaksperiodeId == vedtaksperiodeId } ?: return emptyList()
            return sortedByDescending { it.periode.tom() }
                .filter { it.periode.fom() <= gjeldende.periode.tom() }
        }
    }
}
