package no.nav.helse.modell.vedtaksperiode

import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.automatiskGodkjennSpesialsakvarsler
import no.nav.helse.modell.varsel.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.varsel.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderAktivtVarselOmAvvik
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderSvartelistedeVarsler
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderVarselOmAvvik
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderVarselOmNegativtBeløp
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderVarselOmTilbakedatering
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

internal class Generasjon private constructor(
    private val id: UUID,
    private val vedtaksperiodeId: UUID,
    private var utbetalingId: UUID?,
    private var spleisBehandlingId: UUID?,
    private var skjæringstidspunkt: LocalDate,
    private var periode: Periode,
    private var tilstand: Tilstand,
    private var tags: List<String>,
    varsler: Set<Varsel>,
) {
    internal constructor(
        id: UUID,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        spleisBehandlingId: UUID? = null,
    ) : this(
        id,
        vedtaksperiodeId,
        null,
        spleisBehandlingId,
        skjæringstidspunkt,
        Periode(fom, tom),
        VidereBehandlingAvklares,
        emptyList(),
        emptySet(),
    )

    private val varsler: MutableList<Varsel> = varsler.toMutableList()
    private val observers = mutableSetOf<IVedtaksperiodeObserver>()

    internal fun skjæringstidspunkt() = skjæringstidspunkt

    internal fun unikId() = id

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
            varsler = varsler.map(Varsel::toDto),
        )
    }

    internal fun tilhører(dato: LocalDate): Boolean = periode.tom() <= dato

    internal fun håndter(avsluttetUtenVedtak: no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetUtenVedtak) {
        val vedtakBuilder = SykepengevedtakBuilder()
        avsluttetUtenVedtak.byggMelding(vedtakBuilder)
        observers.forEach { it.vedtakFattet(vedtakBuilder.build()) }
    }

    internal fun nySpleisBehandling(
        vedtaksperiode: Vedtaksperiode,
        spleisBehandling: SpleisBehandling,
    ) {
        tilstand.nySpleisBehandling(this, vedtaksperiode, spleisBehandling)
    }

    internal fun forhindrerAutomatisering(): Boolean = varsler.forhindrerAutomatisering()

    internal fun håndter(
        vedtaksperiode: Vedtaksperiode,
        spleisVedtaksperiode: SpleisVedtaksperiode,
    ) {
        tilstand.spleisVedtaksperiode(vedtaksperiode, this, spleisVedtaksperiode)
    }

    private fun spleisVedtaksperiode(spleisVedtaksperiode: SpleisVedtaksperiode) {
        this.periode = Periode(spleisVedtaksperiode.fom, spleisVedtaksperiode.tom)
        this.skjæringstidspunkt = spleisVedtaksperiode.skjæringstidspunkt
        this.spleisBehandlingId = spleisVedtaksperiode.spleisBehandlingId
    }

    internal fun erSpesialsakSomKanAutomatiseres() = !varsler.inneholderSvartelistedeVarsler()

    internal fun automatiskGodkjennSpesialsakvarsler() = varsler.automatiskGodkjennSpesialsakvarsler(this.id)

    internal fun håndterNyUtbetaling(
        hendelseId: UUID,
        utbetalingId: UUID,
    ) {
        tilstand.nyUtbetaling(this, hendelseId, utbetalingId)
    }

    internal fun håndterForkastetUtbetaling(utbetalingId: UUID) {
        if (utbetalingId != this.utbetalingId) return
        tilstand.invaliderUtbetaling(this, utbetalingId)
    }

    internal fun håndterNyttVarsel(
        varsel: Varsel,
        hendelseId: UUID,
    ) {
        if (!varsel.erRelevantFor(vedtaksperiodeId)) return
        val eksisterendeVarsel = varsler.finnEksisterendeVarsel(varsel) ?: return nyttVarsel(varsel, hendelseId)
        if (varsel.erVarselOmAvvik() && varsler.inneholderVarselOmAvvik()) {
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

    internal fun oppdaterBehandlingsinformasjon(
        tags: List<String>,
        spleisBehandlingId: UUID,
        utbetalingId: UUID,
    ) {
        tilstand.oppdaterBehandlingsinformasjon(this, tags, spleisBehandlingId, utbetalingId)
    }

    internal fun håndterGodkjentAvSaksbehandler(
        ident: String,
        hendelseId: UUID,
    ) {
        tilstand.håndterGodkjenning(this, ident, hendelseId)
    }

    internal fun håndterVedtakFattet(hendelseId: UUID) {
        tilstand.vedtakFattet(this, hendelseId)
    }

    internal fun avsluttetUtenVedtak(
        avsluttetUtenVedtak: no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetUtenVedtak,
        sykepengevedtakBuilder: SykepengevedtakBuilder,
    ) {
        if (spleisBehandlingId == null) spleisBehandlingId = avsluttetUtenVedtak.spleisBehandlingId()
        tilstand.avsluttetUtenVedtak(this, sykepengevedtakBuilder)
    }

    private fun nyTilstand(
        gammel: Tilstand,
        ny: Tilstand,
        hendelseId: UUID,
    ) {
        observers.forEach { it.tilstandEndret(id, vedtaksperiodeId, gammel.navn(), ny.navn(), hendelseId) }
        this.tilstand = ny
    }

    private fun supplerAvsluttetUtenVedtak(sykepengevedtakBuilder: SykepengevedtakBuilder) {
        spleisBehandlingId?.let { sykepengevedtakBuilder.spleisBehandlingId(it) }
        sykepengevedtakBuilder
            .tags(tags)
            .skjæringstidspunkt(skjæringstidspunkt)
            .fom(fom())
            .tom(tom())
    }

    private fun nyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
    }

    private fun nyBehandling(spleisBehandlingId: UUID): Generasjon {
        val nyGenerasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, fom(), tom(), skjæringstidspunkt, spleisBehandlingId)
        flyttAktiveVarslerTil(nyGenerasjon)
        return nyGenerasjon
    }

    private fun nyGenerasjonFraGodkjenningsbehov(
        tilstand: Tilstand,
        vedtaksperiode: Vedtaksperiode,
        spleisVedtaksperiode: SpleisVedtaksperiode,
    ) {
        sikkerlogg.warn(
            "Oppretter ny generasjon fra godkjenningsbehov fordi gjeldende generasjon er i tilstand=${tilstand.navn()}",
            kv("vedtaksperiodeId", vedtaksperiodeId),
            kv("spleisBehandlingId", spleisBehandlingId),
            kv("utbetalingId", utbetalingId),
        )
        val nyGenerasjon = this.nyBehandling(spleisVedtaksperiode.spleisBehandlingId)
        nyGenerasjon.spleisVedtaksperiode(spleisVedtaksperiode)
        vedtaksperiode.nyGenerasjon(nyGenerasjon)
    }

    private fun flyttAktiveVarslerTil(generasjon: Generasjon) {
        val aktiveVarsler = varsler.filter(Varsel::erAktiv)
        this.varsler.removeAll(aktiveVarsler)
        generasjon.varsler.addAll(aktiveVarsler)
        if (aktiveVarsler.isNotEmpty()) {
            sikkerlogg.info(
                "Flytter ${aktiveVarsler.size} varsler fra {} til {}. Gammel generasjon har {}",
                kv("gammel_generasjon", this.id),
                kv("ny_generasjon", generasjon.id),
                kv("utbetalingId", this.utbetalingId),
            )
        }
    }

    private fun nyttVarsel(
        varsel: Varsel,
        hendelseId: UUID,
    ) {
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
                AvsluttetUtenVedtak -> TilstandDto.AvsluttetUtenVedtak
                VedtakFattet -> TilstandDto.VedtakFattet
                VidereBehandlingAvklares -> TilstandDto.VidereBehandlingAvklares
                AvsluttetUtenVedtakMedVarsler -> TilstandDto.AvsluttetUtenVedtakMedVarsler
                KlarTilBehandling -> TilstandDto.KlarTilBehandling
            }
        }

        fun avsluttetUtenVedtak(
            generasjon: Generasjon,
            sykepengevedtakBuilder: SykepengevedtakBuilder,
        ) {
            throw IllegalStateException("Forventer ikke avsluttet_uten_vedtak i tilstand=${this::class.simpleName}")
        }

        fun vedtakFattet(
            generasjon: Generasjon,
            hendelseId: UUID,
        ) {
            sikkerlogg.info("Forventet ikke vedtak_fattet i {}", kv("tilstand", this::class.simpleName))
        }

        fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            generasjon: Generasjon,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        )

        fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling,
        ) {}

        fun nyUtbetaling(
            generasjon: Generasjon,
            hendelseId: UUID,
            utbetalingId: UUID,
        ) {
            sikkerlogg.error(
                "Mottatt ny utbetaling med {} for {} i {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("generasjon", generasjon),
                keyValue("tilstand", this::class.simpleName),
            )
            logg.error(
                "Mottatt ny utbetaling med {} i {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("tilstand", this::class.simpleName),
            )
        }

        fun invaliderUtbetaling(
            generasjon: Generasjon,
            utbetalingId: UUID,
        ) {
            logg.error(
                "{} er i {}. Utbetaling med {} forsøkt forkastet",
                keyValue("Generasjon", generasjon),
                keyValue("tilstand", this::class.simpleName),
                keyValue("utbetalingId", utbetalingId),
            )
            sikkerlogg.error(
                "{} er i {}. Utbetaling med {} forsøkt forkastet",
                keyValue("Generasjon", generasjon),
                keyValue("tilstand", this::class.simpleName),
                keyValue("utbetalingId", utbetalingId),
            )
        }

        fun nyttVarsel(
            generasjon: Generasjon,
            varsel: Varsel,
            hendelseId: UUID,
        ) {}

        fun håndterGodkjenning(
            generasjon: Generasjon,
            ident: String,
            hendelseId: UUID,
        ) {}

        fun oppdaterBehandlingsinformasjon(
            generasjon: Generasjon,
            tags: List<String>,
            spleisBehandlingId: UUID,
            utbetalingId: UUID,
        ) {
            throw IllegalStateException("Mottatt godkjenningsbehov i tilstand=${navn()}")
        }
    }

    internal data object VidereBehandlingAvklares : Tilstand {
        override fun navn(): String = "VidereBehandlingAvklares"

        override fun nyUtbetaling(
            generasjon: Generasjon,
            hendelseId: UUID,
            utbetalingId: UUID,
        ) {
            generasjon.nyUtbetaling(utbetalingId)
            generasjon.nyTilstand(this, KlarTilBehandling, hendelseId)
        }

        override fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling,
        ) {
            sikkerlogg.warn("Forventer ikke ny Spleis-behandling, gjeldende generasjon i Spesialist er ikke lukket")
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            generasjon: Generasjon,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            generasjon.spleisVedtaksperiode(spleisVedtaksperiode)
        }

        override fun avsluttetUtenVedtak(
            generasjon: Generasjon,
            sykepengevedtakBuilder: SykepengevedtakBuilder,
        ) {
            check(
                generasjon.utbetalingId == null,
            ) { "Mottatt avsluttet_uten_vedtak på generasjon som har utbetaling. Det gir ingen mening." }
            val nesteTilstand =
                when {
                    generasjon.varsler.isNotEmpty() -> AvsluttetUtenVedtakMedVarsler
                    else -> AvsluttetUtenVedtak
                }
            generasjon.nyTilstand(this, nesteTilstand, UUID.randomUUID())
            generasjon.supplerAvsluttetUtenVedtak(sykepengevedtakBuilder)
        }
    }

    internal data object KlarTilBehandling : Tilstand {
        override fun navn(): String = "KlarTilBehandling"

        override fun vedtakFattet(
            generasjon: Generasjon,
            hendelseId: UUID,
        ) {
            checkNotNull(generasjon.utbetalingId) { "Mottatt vedtak_fattet i tilstand=${navn()}, men mangler utbetalingId" }
            generasjon.nyTilstand(this, VedtakFattet, hendelseId)
        }

        override fun oppdaterBehandlingsinformasjon(
            generasjon: Generasjon,
            tags: List<String>,
            spleisBehandlingId: UUID,
            utbetalingId: UUID,
        ) {
            generasjon.tags = tags
            generasjon.spleisBehandlingId = spleisBehandlingId
            generasjon.utbetalingId = utbetalingId
        }

        override fun invaliderUtbetaling(
            generasjon: Generasjon,
            utbetalingId: UUID,
        ) {
            generasjon.utbetalingId = null
            generasjon.nyTilstand(this, VidereBehandlingAvklares, UUID.randomUUID())
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            generasjon: Generasjon,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            generasjon.spleisVedtaksperiode(spleisVedtaksperiode)
        }
    }

    internal data object VedtakFattet : Tilstand {
        override fun navn(): String = "VedtakFattet"

        override fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling,
        ) {
            vedtaksperiode.nyGenerasjon(generasjon.nyBehandling(spleisBehandling.spleisBehandlingId))
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            generasjon: Generasjon,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            if (generasjon.spleisBehandlingId == null || generasjon.spleisBehandlingId == spleisVedtaksperiode.spleisBehandlingId) return
            generasjon.nyGenerasjonFraGodkjenningsbehov(this, vedtaksperiode, spleisVedtaksperiode)
        }
    }

    internal data object AvsluttetUtenVedtak : Tilstand {
        override fun navn(): String = "AvsluttetUtenVedtak"

        override fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling,
        ) {
            vedtaksperiode.nyGenerasjon(generasjon.nyBehandling(spleisBehandling.spleisBehandlingId))
        }

        override fun nyttVarsel(
            generasjon: Generasjon,
            varsel: Varsel,
            hendelseId: UUID,
        ) {
            sikkerlogg.warn("Mottar nytt varsel i tilstand ${navn()}")
            generasjon.nyTilstand(this, AvsluttetUtenVedtakMedVarsler, hendelseId)
        }

        override fun avsluttetUtenVedtak(
            generasjon: Generasjon,
            sykepengevedtakBuilder: SykepengevedtakBuilder,
        ) {
            sikkerlogg.warn("Spesialist mottar avsluttet_uten_vedtak når den allerede er i tilstand ${navn()}")
            generasjon.supplerAvsluttetUtenVedtak(sykepengevedtakBuilder)
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            generasjon: Generasjon,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            if (generasjon.spleisBehandlingId == null || generasjon.spleisBehandlingId == spleisVedtaksperiode.spleisBehandlingId) return
            generasjon.nyGenerasjonFraGodkjenningsbehov(this, vedtaksperiode, spleisVedtaksperiode)
        }
    }

    internal data object AvsluttetUtenVedtakMedVarsler : Tilstand {
        override fun navn(): String = "AvsluttetUtenVedtakMedVarsler"

        override fun håndterGodkjenning(
            generasjon: Generasjon,
            ident: String,
            hendelseId: UUID,
        ) {
            generasjon.nyTilstand(this, AvsluttetUtenVedtak, hendelseId)
        }

        override fun avsluttetUtenVedtak(
            generasjon: Generasjon,
            sykepengevedtakBuilder: SykepengevedtakBuilder,
        ) {
            sikkerlogg.warn("Spesialist mottar avsluttet_uten_vedtak når den allerede er i tilstand AvsluttetUtenVedtakMedVarsler")
            generasjon.supplerAvsluttetUtenVedtak(sykepengevedtakBuilder)
        }

        override fun nySpleisBehandling(
            generasjon: Generasjon,
            vedtaksperiode: Vedtaksperiode,
            spleisBehandling: SpleisBehandling,
        ) {
            vedtaksperiode.nyGenerasjon(generasjon.nyBehandling(spleisBehandling.spleisBehandlingId))
            generasjon.nyTilstand(this, AvsluttetUtenVedtak, UUID.randomUUID())
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            generasjon: Generasjon,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            if (generasjon.spleisBehandlingId == null || generasjon.spleisBehandlingId == spleisVedtaksperiode.spleisBehandlingId) return
            generasjon.nyGenerasjonFraGodkjenningsbehov(this, vedtaksperiode, spleisVedtaksperiode)
            generasjon.nyTilstand(this, AvsluttetUtenVedtak, UUID.randomUUID())
        }
    }

    override fun toString(): String = "generasjonId=$id, vedtaksperiodeId=$vedtaksperiodeId"

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is Generasjon &&
                javaClass == other.javaClass &&
                id == other.id &&
                vedtaksperiodeId == other.vedtaksperiodeId &&
                utbetalingId == other.utbetalingId &&
                spleisBehandlingId == other.spleisBehandlingId &&
                tilstand == other.tilstand &&
                skjæringstidspunkt == other.skjæringstidspunkt &&
                periode == other.periode
        )

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
        val logg: Logger = LoggerFactory.getLogger(Generasjon::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun List<Generasjon>.finnGenerasjonForVedtaksperiode(vedtaksperiodeId: UUID): Generasjon? =
            this.find { it.vedtaksperiodeId == vedtaksperiodeId }

        internal fun List<Generasjon>.finnGenerasjonForSpleisBehandling(spleisBehandlingId: UUID): Generasjon? =
            this.find { it.spleisBehandlingId == spleisBehandlingId }

        internal fun List<Generasjon>.finnSisteGenerasjonUtenSpleisBehandlingId(): Generasjon? =
            this.lastOrNull { it.spleisBehandlingId == null }

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
            varsler: Set<Varsel>,
        ) = Generasjon(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            periode = Periode(fom, tom),
            tilstand = tilstand,
            tags = tags,
            varsler = varsler,
        )

        internal fun List<Generasjon>.håndterNyttVarsel(
            varsler: List<Varsel>,
            hendelseId: UUID,
        ) {
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
            hendelseId: UUID,
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
