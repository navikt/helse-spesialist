package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.automatiskGodkjennSpesialsakvarsler
import no.nav.helse.modell.varsel.Varsel.Companion.erVarselOmAvvik
import no.nav.helse.modell.varsel.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.varsel.Varsel.Companion.flyttVarslerFor
import no.nav.helse.modell.varsel.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderAktivtVarselOmAvvik
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderSvartelistedeVarsler
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderVarselOmAvvik
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderVarselOmNegativtBeløp
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderVarselOmTilbakedatering
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtaksperiode.vedtak.SykepengevedtakBuilder
import org.slf4j.LoggerFactory

internal class Generasjon private constructor(
    private val id: UUID,
    private val vedtaksperiodeId: UUID,
    private var utbetalingId: UUID?,
    private var spleisBehandlingId: UUID?,
    private var skjæringstidspunkt: LocalDate,
    private var periode: Periode,
    private var tilstand: Tilstand,
    private var tags: List<String>,
    varsler: Set<Varsel>
) {
    internal constructor(
        id: UUID,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        spleisBehandlingId: UUID? = null
    ): this(id, vedtaksperiodeId, null, spleisBehandlingId, skjæringstidspunkt, Periode(fom, tom), Ulåst, emptyList(), emptySet())

    private val varsler: MutableList<Varsel> = varsler.toMutableList()
    private val observers = mutableSetOf<IVedtaksperiodeObserver>()

    internal fun skjæringstidspunkt() = skjæringstidspunkt

    internal fun utbetalingId() = utbetalingId

    internal fun hasterÅBehandle() = varsler.inneholderVarselOmNegativtBeløp()

    internal fun fom() = periode.fom()
    internal fun tom() = periode.tom()

    internal fun registrer(vararg observer: IVedtaksperiodeObserver) {
        observers.addAll(observer)
        varsler.forEach { it.registrer(*observer) }
    }

    internal fun toDto(): GenerasjonDto {
        return GenerasjonDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = periode.fom(),
            tom = periode.tom(),
            tilstand = tilstand.toDto(),
            tags = tags,
            varsler = varsler.map(Varsel::toDto)
        )
    }

    internal fun tilhører(dato: LocalDate): Boolean = periode.tom() <= dato

    internal fun håndter(avsluttetUtenVedtak: AvsluttetUtenVedtak) {
        val vedtakBuilder = SykepengevedtakBuilder()
        avsluttetUtenVedtak.byggMelding(vedtakBuilder)
        observers.forEach { it.vedtakFattet(vedtakBuilder.build()) }
    }

    internal fun nySpleisBehandling(vedtaksperiode: Vedtaksperiode, spleisBehandling: SpleisBehandling) {
        tilstand.nySpleisBehandling(this, vedtaksperiode, spleisBehandling)
    }

    internal fun forhindrerAutomatisering(): Boolean = varsler.forhindrerAutomatisering()

    internal fun håndter(spleisVedtaksperiode: SpleisVedtaksperiode) {
        this.periode = Periode(spleisVedtaksperiode.fom, spleisVedtaksperiode.tom)
        this.skjæringstidspunkt = spleisVedtaksperiode.skjæringstidspunkt
        this.spleisBehandlingId = spleisVedtaksperiode.spleisBehandlingId
    }

    internal fun erSpesialsakSomKanAutomatiseres() = !varsler.inneholderSvartelistedeVarsler()

    internal fun automatiskGodkjennSpesialsakvarsler() = varsler.automatiskGodkjennSpesialsakvarsler(this.id)

    internal fun håndterNyUtbetaling(hendelseId: UUID, utbetalingId: UUID) {
        tilstand.nyUtbetaling(this, hendelseId, utbetalingId)
    }

    internal fun håndterForkastetUtbetaling(utbetalingId: UUID) {
        if (utbetalingId != this.utbetalingId) return
        tilstand.invaliderUtbetaling(this, utbetalingId)
    }

    internal fun håndterNyttVarsel(varsel: Varsel, hendelseId: UUID) {
        if (!varsel.erRelevantFor(vedtaksperiodeId)) return
        val eksisterendeVarsel = varsler.finnEksisterendeVarsel(varsel) ?: return nyttVarsel(varsel, hendelseId)
        if (varsel.erVarselOmAvvik() && varsler.inneholderVarselOmAvvik()) {
            eksisterendeVarsel.slett(id)
            varsler.remove(eksisterendeVarsel)
            logg.info("Slettet eksisterende varsel ({}) for generasjon med id {}", varsel.toString(), id)
            nyttVarsel(varsel, hendelseId)
        }
        if (eksisterendeVarsel.erAktiv()) return
        eksisterendeVarsel.reaktiver(id)
    }

    internal fun håndterDeaktivertVarsel(varsel: Varsel) {
        val funnetVarsel = varsler.finnEksisterendeVarsel(varsel) ?: return
        funnetVarsel.deaktiver(id)
    }

    internal fun deaktiverVarsel(varselkode: String) {
        val funnetVarsel = varsler.finnEksisterendeVarsel(varselkode) ?: return
        funnetVarsel.deaktiver(id)
    }

    internal fun oppdaterBehandlingsinformasjon(tags: List<String>, spleisBehandlingId: UUID) {
        this.tags = tags
        this.spleisBehandlingId = spleisBehandlingId
    }

    internal fun håndterGodkjentAvSaksbehandler(ident: String, hendelseId: UUID) {
        tilstand.håndterGodkjenning(this, ident, hendelseId)
    }

    internal fun håndterVedtakFattet(hendelseId: UUID) {
        tilstand.vedtakFattet(this, hendelseId)
    }

    private fun nyTilstand(gammel: Tilstand, ny: Tilstand, hendelseId: UUID) {
        observers.forEach { it.tilstandEndret(id, vedtaksperiodeId, gammel, ny, hendelseId) }
        this.tilstand = ny
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

    private fun nyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
        observers.forEach { it.nyUtbetaling(id, utbetalingId) }
    }

    private fun opprett(hendelseId: UUID) {
        observers.forEach {
            it.generasjonOpprettet(id, vedtaksperiodeId, hendelseId, periode.fom(), periode.tom(), skjæringstidspunkt, tilstand)
        }
    }

    private fun nyBehandling(spleisBehandlingId: UUID): Generasjon {
        val nyGenerasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, fom(), tom(), skjæringstidspunkt, spleisBehandlingId)
        flyttAktiveVarslerTil(nyGenerasjon)
        return nyGenerasjon
    }

    private fun flyttAktiveVarslerTil(generasjon: Generasjon) {
        val aktiveVarsler = varsler.filter(Varsel::erAktiv)
        this.varsler.removeAll(aktiveVarsler)
        generasjon.varsler.addAll(aktiveVarsler)
        if (aktiveVarsler.isNotEmpty())
            sikkerlogg.info(
                "Flytter ${aktiveVarsler.size} varsler fra {} til {}. Gammel generasjon har {}",
                kv("gammel_generasjon", this.id),
                kv("ny_generasjon", generasjon.id),
                kv("utbetalingId", this.utbetalingId),
            )
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

    private fun nyttVarsel(varsel: Varsel, hendelseId: UUID) {
        varsel.registrer(*this.observers.toTypedArray())
        varsler.add(varsel)
        varsel.opprett(id)
        tilstand.nyttVarsel(this, varsel, hendelseId)
    }

    private fun kreverTotrinnsvurdering(): Boolean {
        val inneholderMedlemskapsvarsel = varsler.inneholderMedlemskapsvarsel()
        logg.info("$this harMedlemskapsvarsel: $inneholderMedlemskapsvarsel")
        return inneholderMedlemskapsvarsel
    }

    private fun kreverSkjønnsfastsettelse(): Boolean {
        val inneholderAvviksvarsel = varsler.inneholderAktivtVarselOmAvvik()
        logg.info("$this harAvviksvarsel: $inneholderAvviksvarsel")
        return inneholderAvviksvarsel
    }

    private fun erTilbakedatert(): Boolean {
        val inneholderTilbakedateringsvarsel = varsler.inneholderVarselOmTilbakedatering()
        logg.info("$this harTilbakedateringsvarsel: $inneholderTilbakedateringsvarsel")
        return inneholderTilbakedateringsvarsel
    }

    internal sealed interface Tilstand {
        fun navn(): String

        fun toDto(): TilstandDto {
            return when (this) {
                AvsluttetUtenUtbetaling -> TilstandDto.AvsluttetUtenUtbetaling
                Låst -> TilstandDto.Låst
                Ulåst -> TilstandDto.Ulåst
                UtenUtbetalingMåVurderes -> TilstandDto.UtenUtbetalingMåVurderes
            }
        }

        fun vedtakFattet(generasjon: Generasjon, hendelseId: UUID) {
            sikkerlogg.info("Forventet ikke vedtak_fattet i {}", kv("tilstand", this::class.simpleName))
        }

        fun nySpleisBehandling(generasjon: Generasjon, vedtaksperiode: Vedtaksperiode, spleisBehandling: SpleisBehandling) {}

        fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {}

        fun invaliderUtbetaling(generasjon: Generasjon, utbetalingId: UUID) {
            logg.error(
                "{} er i {}. Utbetaling med {} forsøkt forkastet",
                keyValue("Generasjon", generasjon),
                keyValue("tilstand", this::class.simpleName),
                keyValue("utbetalingId", utbetalingId)
            )
            sikkerlogg.error(
                "{} er i {}. Utbetaling med {} forsøkt forkastet",
                keyValue("Generasjon", generasjon),
                keyValue("tilstand", this::class.simpleName),
                keyValue("utbetalingId", utbetalingId)
            )
        }
        fun nyttVarsel(generasjon: Generasjon, varsel: Varsel, hendelseId: UUID) {}

        fun håndterGodkjenning(generasjon: Generasjon, ident: String, hendelseId: UUID) {}
    }

    internal data object Ulåst: Tilstand {
        override fun navn(): String = "Ulåst"
        override fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {
            generasjon.nyUtbetaling(utbetalingId)
        }

        override fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling
        ) {
            sikkerlogg.warn("Forventer ikke ny Spleis-behandling, gjeldende generasjon i Spesialist er ikke lukket")
        }

        override fun invaliderUtbetaling(generasjon: Generasjon, utbetalingId: UUID) {
            generasjon.utbetalingId = null
            generasjon.observers.forEach { it.utbetalingForkastet(generasjon.id, utbetalingId) }
        }

        override fun vedtakFattet(generasjon: Generasjon, hendelseId: UUID) {
            if (generasjon.utbetalingId == null)
                return generasjon.nyTilstand(this, AvsluttetUtenUtbetaling, hendelseId)
            generasjon.nyTilstand(this, Låst, hendelseId)
        }
    }

    internal data object Låst: Tilstand {
        override fun navn(): String = "Låst"

        override fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {
            val nyGenerasjonId = UUID.randomUUID()
            generasjon.nyGenerasjon(hendelseId, nyGenerasjonId).nyUtbetaling(utbetalingId)
        }

        override fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling
        ) {
            vedtaksperiode.nyGenerasjon(generasjon.nyBehandling(spleisBehandling.spleisBehandlingId))
        }
    }

    internal data object AvsluttetUtenUtbetaling: Tilstand {
        override fun navn(): String = "AvsluttetUtenUtbetaling"

        override fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {
            val nyGenerasjonId = UUID.randomUUID()
            generasjon.nyGenerasjon(hendelseId, nyGenerasjonId).nyUtbetaling(utbetalingId)
        }

        override fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling
        ) {
            vedtaksperiode.nyGenerasjon(generasjon.nyBehandling(spleisBehandling.spleisBehandlingId))
        }

        override fun nyttVarsel(generasjon: Generasjon, varsel: Varsel, hendelseId: UUID) {
            generasjon.nyTilstand(this, UtenUtbetalingMåVurderes, hendelseId)
        }
    }

    internal data object UtenUtbetalingMåVurderes: Tilstand {
        override fun navn(): String = "UtenUtbetalingMåVurderes"

        override fun nyUtbetaling(generasjon: Generasjon, hendelseId: UUID, utbetalingId: UUID) {
            val nyGenerasjonId = UUID.randomUUID()
            generasjon.nyTilstand(this, AvsluttetUtenUtbetaling, hendelseId)
            generasjon.nyGenerasjon(hendelseId, nyGenerasjonId).nyUtbetaling(utbetalingId)
        }

        override fun håndterGodkjenning(generasjon: Generasjon, ident: String, hendelseId: UUID) {
            generasjon.nyTilstand(this, AvsluttetUtenUtbetaling, hendelseId)
        }

        override fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling
        ) {
            vedtaksperiode.nyGenerasjon(generasjon.nyBehandling(spleisBehandling.spleisBehandlingId))
        }
    }

    override fun toString(): String = "generasjonId=$id, vedtaksperiodeId=$vedtaksperiodeId"

    override fun equals(other: Any?): Boolean =
        this === other || (other is Generasjon
                && javaClass == other.javaClass
                && id == other.id
                && vedtaksperiodeId == other.vedtaksperiodeId
                && utbetalingId == other.utbetalingId
                && spleisBehandlingId == other.spleisBehandlingId
                && tilstand == other.tilstand
                && skjæringstidspunkt == other.skjæringstidspunkt
                && periode == other.periode)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + utbetalingId.hashCode()
        result = 31 * result + spleisBehandlingId.hashCode()
        result = 31 * result + tilstand.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + periode.hashCode()
        return result
    }

    internal companion object {

        private val logg = LoggerFactory.getLogger(Generasjon::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun nyVedtaksperiode(
            generasjonId: UUID = UUID.randomUUID(),
            vedtaksperiodeId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
        ): Generasjon {
            return opprett(
                id = generasjonId,
                vedtaksperiodeId = vedtaksperiodeId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
            )
        }

        internal fun List<Generasjon>.finnGenerasjon(vedtaksperiodeId: UUID): Generasjon? =
            this.find { it.vedtaksperiodeId == vedtaksperiodeId }

        internal fun fraLagring(
            id: UUID,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID?,
            spleisBehandlingId: UUID?,
            skjæringstidspunkt: LocalDate,
            fom: LocalDate,
            tom: LocalDate,
            tilstand: Tilstand,
            tags: List<String>,
            varsler: Set<Varsel>
        ) = Generasjon(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            periode = Periode(fom, tom),
            tilstand = tilstand,
            tags = tags,
            varsler = varsler
        )

        private fun opprett(
            id: UUID,
            vedtaksperiodeId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
        ): Generasjon {
            return Generasjon(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
            )
        }

        private fun Generasjon.opprettNeste(generasjonId: UUID, hendelseId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate): Generasjon {
            val nyGenerasjon = opprett(generasjonId, this.vedtaksperiodeId, fom, tom, skjæringstidspunkt)
            nyGenerasjon.registrer(*this.observers.toTypedArray())
            nyGenerasjon.opprett(hendelseId)

            return nyGenerasjon
        }

        internal fun List<Generasjon>.håndterNyttVarsel(varsler: List<Varsel>, hendelseId: UUID) {
            forEach { generasjon ->
                varsler.forEach { generasjon.håndterNyttVarsel(it, hendelseId) }
            }
        }

        internal fun List<Generasjon>.forhindrerAutomatisering(tilOgMed: LocalDate): Boolean {
            return this.filter { it.tilhører(tilOgMed) }.any { it.forhindrerAutomatisering() }
        }

        internal fun List<Generasjon>.forhindrerAutomatisering(generasjon: Generasjon): Boolean {
            return this.filter { it.tilhører(generasjon.periode.tom()) }.any { it.forhindrerAutomatisering() }
        }

        internal fun List<Generasjon>.kreverTotrinnsvurdering(vedtaksperiodeId: UUID): Boolean {
            return overlapperMedEllerTidligereEnn(vedtaksperiodeId).any { it.kreverTotrinnsvurdering() }
        }

        internal fun List<Generasjon>.kreverSkjønnsfastsettelse(vedtaksperiodeId: UUID): Boolean {
            return overlapperMedEllerTidligereEnn(vedtaksperiodeId).any { it.kreverSkjønnsfastsettelse() }
        }

        internal fun List<Generasjon>.erTilbakedatert(vedtaksperiodeId: UUID): Boolean {
            return overlapperMedEllerTidligereEnn(vedtaksperiodeId).any { it.erTilbakedatert() }
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

        private fun List<Generasjon>.overlapperMedEllerTidligereEnn(vedtaksperiodeId: UUID): List<Generasjon> {
            val gjeldende = find { it.vedtaksperiodeId == vedtaksperiodeId } ?: return emptyList()
            return sortedByDescending { it.periode.tom() }
                .filter { it.periode.fom() <= gjeldende.periode.tom() }
        }
    }
}
