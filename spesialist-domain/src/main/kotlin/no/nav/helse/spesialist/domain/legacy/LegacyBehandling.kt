package no.nav.helse.spesialist.domain.legacy

import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderAktivtVarselOmAvvik
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmAvvik
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmNegativtBeløp
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmTilbakedatering
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmÅpenGosysOppgave
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.Periode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class LegacyBehandling private constructor(
    private val id: UUID,
    val vedtaksperiodeId: UUID,
    utbetalingId: UUID?,
    spleisBehandlingId: UUID?,
    skjæringstidspunkt: LocalDate,
    private var periode: Periode,
    private var tilstand: Tilstand,
    tags: List<String>,
    val vedtakBegrunnelse: VedtakBegrunnelse?,
    varsler: Set<Varsel>,
) {
    constructor(
        id: UUID,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        spleisBehandlingId: UUID? = null,
        utbetalingId: UUID? = null,
    ) : this(
        id = id,
        vedtaksperiodeId = vedtaksperiodeId,
        utbetalingId = utbetalingId,
        spleisBehandlingId = spleisBehandlingId,
        skjæringstidspunkt = skjæringstidspunkt,
        periode = Periode(fom, tom),
        tilstand = VidereBehandlingAvklares,
        tags = emptyList(),
        vedtakBegrunnelse = null,
        varsler = emptySet(),
    )

    var spleisBehandlingId: UUID? = spleisBehandlingId
        private set

    var skjæringstidspunkt: LocalDate = skjæringstidspunkt
        private set

    var tags: List<String> = tags
        private set

    private val varsler: MutableList<Varsel> = varsler.toMutableList()

    internal var utbetalingId: UUID? = utbetalingId
        private set

    internal fun spleisBehandlingId() = spleisBehandlingId

    fun skjæringstidspunkt() = skjæringstidspunkt

    fun vedtaksperiodeId() = vedtaksperiodeId

    fun varsler(): List<Varsel> = varsler.toList()

    internal fun unikId() = id

    internal fun hasterÅBehandle() = varsler.inneholderVarselOmNegativtBeløp()

    fun fom() = periode.fom

    fun tom() = periode.tom

    fun toDto(): BehandlingDto =
        BehandlingDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = periode.fom,
            tom = periode.tom,
            tilstand = tilstand.toDto(),
            tags = tags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            varsler = varsler.map(Varsel::toDto),
        )

    internal fun tilhører(dato: LocalDate): Boolean = periode.tom <= dato

    internal fun nySpleisBehandling(spleisBehandling: SpleisBehandling) = nyBehandling(spleisBehandling)

    internal fun forhindrerAutomatisering(): Boolean = varsler.forhindrerAutomatisering()

    internal fun harKunGosysvarsel() = varsler.size == 1 && varsler.single().erGosysvarsel()

    internal fun harVarselOmManglendeInntektsmelding() = varsler.any { it.erVarselOmManglendeInntektsmelding() }

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

    fun håndterNyUtbetaling(utbetalingId: UUID) {
        tilstand.nyUtbetaling(this, utbetalingId)
    }

    internal fun håndterForkastetUtbetaling(utbetalingId: UUID) {
        if (utbetalingId != this.utbetalingId) return
        tilstand.invaliderUtbetaling(this, utbetalingId)
    }

    fun håndterNyttVarsel(varsel: Varsel) {
        if (!varsel.erRelevantFor(vedtaksperiodeId)) return
        val eksisterendeVarsel = varsler.finnEksisterendeVarsel(varsel) ?: return nyttVarsel(varsel)
        if (varsel.erVarselOmAvvik() && varsler.inneholderVarselOmAvvik()) {
            varsler.remove(eksisterendeVarsel)
            logg.info("Slettet eksisterende varsel ({}) for behandling med id {}", varsel.toString(), id)
            nyttVarsel(varsel)
        }
        if (eksisterendeVarsel.erAktiv()) return
        eksisterendeVarsel.reaktiver()
    }

    internal fun håndterDeaktivertVarsel(varsel: Varsel) {
        val funnetVarsel = varsler.finnEksisterendeVarsel(varsel) ?: return
        funnetVarsel.deaktiver()
    }

    internal fun deaktiverVarsel(varselkode: String) {
        val funnetVarsel = varsler.finnEksisterendeVarsel(varselkode) ?: return
        sikkerlogg.info("Deaktiverer varsel: {}", funnetVarsel)
        funnetVarsel.deaktiver()
    }

    internal fun oppdaterBehandlingsinformasjon(
        tags: List<String>,
        spleisBehandlingId: UUID,
        utbetalingId: UUID,
    ) {
        tilstand.oppdaterBehandlingsinformasjon(this, tags, spleisBehandlingId, utbetalingId)
    }

    internal fun håndterGodkjentAvSaksbehandler() {
        tilstand.håndterGodkjenning(this)
    }

    fun håndterVedtakFattet() {
        tilstand.vedtakFattet(this)
    }

    internal fun avsluttetUtenVedtak() {
        tilstand.avsluttetUtenVedtak(this)
    }

    fun behandlingId(): UUID = spleisBehandlingId ?: throw IllegalStateException("Forventer at spleisBehandlingId er satt")

    fun utbetalingId(): UUID = utbetalingId ?: throw IllegalStateException("Forventer at utbetalingId er satt")

    private fun nyTilstand(ny: Tilstand) {
        this.tilstand = ny
    }

    private fun nyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
    }

    private fun nyBehandling(spleisBehandling: SpleisBehandling): LegacyBehandling {
        val nyLegacyBehandling =
            LegacyBehandling(
                id = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiodeId,
                fom = spleisBehandling.fom,
                tom = spleisBehandling.tom,
                skjæringstidspunkt = skjæringstidspunkt,
                spleisBehandlingId = spleisBehandling.spleisBehandlingId,
            )
        flyttAktiveVarslerTil(nyLegacyBehandling)
        return nyLegacyBehandling
    }

    private fun flyttAktiveVarslerTil(legacyBehandling: LegacyBehandling) {
        val aktiveVarsler = varsler.filter(Varsel::erAktiv)
        this.varsler.removeAll(aktiveVarsler)
        legacyBehandling.varsler.addAll(aktiveVarsler)
        if (aktiveVarsler.isNotEmpty()) {
            sikkerlogg.info(
                "Flytter ${aktiveVarsler.size} varsler fra {} til {}. Gammel behandling har {}",
                kv("gammel_behandling", this.id),
                kv("ny_behandling", legacyBehandling.id),
                kv("utbetalingId", this.utbetalingId),
            )
        }
    }

    private fun nyttVarsel(varsel: Varsel) {
        logg.info("Legger til varsel $varsel")
        varsler.add(varsel)
        tilstand.nyttVarsel(this)
    }

    private fun harMedlemskapsvarsel(): Boolean {
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

    private fun harKunÅpenGosysOppgave(): Boolean {
        val inneholderKunÅpenGosysOppgaveVarsel = varsler.inneholderVarselOmÅpenGosysOppgave() && varsler.size == 1
        logg.info("$this harKunÅpenGosysOppgavevarsel: $inneholderKunÅpenGosysOppgaveVarsel")
        return inneholderKunÅpenGosysOppgaveVarsel
    }

    internal sealed interface Tilstand {
        fun navn(): String

        fun toDto(): TilstandDto =
            when (this) {
                AvsluttetUtenVedtak -> TilstandDto.AvsluttetUtenVedtak
                VedtakFattet -> TilstandDto.VedtakFattet
                VidereBehandlingAvklares -> TilstandDto.VidereBehandlingAvklares
                AvsluttetUtenVedtakMedVarsler -> TilstandDto.AvsluttetUtenVedtakMedVarsler
                KlarTilBehandling -> TilstandDto.KlarTilBehandling
            }

        fun avsluttetUtenVedtak(legacyBehandling: LegacyBehandling): Unit = throw IllegalStateException("Forventer ikke avsluttet_uten_vedtak i tilstand=${this::class.simpleName}")

        fun vedtakFattet(legacyBehandling: LegacyBehandling) {
            sikkerlogg.info("Forventet ikke vedtak_fattet i {}", kv("tilstand", this::class.simpleName))
        }

        fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            legacyBehandling: LegacyBehandling,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
        }

        fun nyUtbetaling(
            legacyBehandling: LegacyBehandling,
            utbetalingId: UUID,
        ) {
            sikkerlogg.error(
                "Mottatt ny utbetaling med {} for {} i {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("behandling", legacyBehandling),
                keyValue("tilstand", this::class.simpleName),
            )
            logg.error(
                "Mottatt ny utbetaling med {} i {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("tilstand", this::class.simpleName),
            )
        }

        fun invaliderUtbetaling(
            legacyBehandling: LegacyBehandling,
            utbetalingId: UUID,
        ) {
            logg.error(
                "Utbetaling med {} ble forsøkt forkastet, men det støttes ikke for {} som er i {}.",
                keyValue("Behandling", legacyBehandling),
                keyValue("utbetalingId", utbetalingId),
                keyValue("tilstand", this::class.simpleName),
            )
            sikkerlogg.error(
                "Utbetaling med {} ble forsøkt forkastet, men det støttes ikke for {} som er i {}.",
                keyValue("Behandling", legacyBehandling),
                keyValue("utbetalingId", utbetalingId),
                keyValue("tilstand", this::class.simpleName),
            )
        }

        fun nyttVarsel(legacyBehandling: LegacyBehandling) {}

        fun håndterGodkjenning(legacyBehandling: LegacyBehandling) {}

        fun oppdaterBehandlingsinformasjon(
            legacyBehandling: LegacyBehandling,
            tags: List<String>,
            spleisBehandlingId: UUID,
            utbetalingId: UUID,
        ): Unit = throw IllegalStateException("Mottatt godkjenningsbehov i tilstand=${navn()}")
    }

    internal data object VidereBehandlingAvklares : Tilstand {
        override fun navn(): String = "VidereBehandlingAvklares"

        override fun nyUtbetaling(
            legacyBehandling: LegacyBehandling,
            utbetalingId: UUID,
        ) {
            legacyBehandling.nyUtbetaling(utbetalingId)
            legacyBehandling.nyTilstand(KlarTilBehandling)
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            legacyBehandling: LegacyBehandling,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            legacyBehandling.spleisVedtaksperiode(spleisVedtaksperiode)
        }

        override fun avsluttetUtenVedtak(legacyBehandling: LegacyBehandling) {
            check(
                legacyBehandling.utbetalingId == null,
            ) { "Mottatt avsluttet_uten_vedtak på behandling som har utbetaling. Det gir ingen mening." }
            val nesteTilstand =
                when {
                    legacyBehandling.varsler.isNotEmpty() -> AvsluttetUtenVedtakMedVarsler
                    else -> AvsluttetUtenVedtak
                }
            legacyBehandling.nyTilstand(nesteTilstand)
        }
    }

    internal data object KlarTilBehandling : Tilstand {
        override fun navn(): String = "KlarTilBehandling"

        override fun vedtakFattet(legacyBehandling: LegacyBehandling) {
            checkNotNull(legacyBehandling.utbetalingId) { "Mottatt vedtak_fattet i tilstand=${navn()}, men mangler utbetalingId" }
            legacyBehandling.nyTilstand(VedtakFattet)
        }

        override fun oppdaterBehandlingsinformasjon(
            legacyBehandling: LegacyBehandling,
            tags: List<String>,
            spleisBehandlingId: UUID,
            utbetalingId: UUID,
        ) {
            legacyBehandling.tags = tags
            legacyBehandling.spleisBehandlingId = spleisBehandlingId
            legacyBehandling.utbetalingId = utbetalingId
        }

        override fun invaliderUtbetaling(
            legacyBehandling: LegacyBehandling,
            utbetalingId: UUID,
        ) {
            legacyBehandling.utbetalingId = null
            legacyBehandling.nyTilstand(VidereBehandlingAvklares)
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            legacyBehandling: LegacyBehandling,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            legacyBehandling.spleisVedtaksperiode(spleisVedtaksperiode)
        }
    }

    data object VedtakFattet : Tilstand {
        override fun navn(): String = "VedtakFattet"
    }

    internal data object AvsluttetUtenVedtak : Tilstand {
        override fun navn(): String = "AvsluttetUtenVedtak"

        override fun nyttVarsel(legacyBehandling: LegacyBehandling) {
            sikkerlogg.warn("Mottar nytt varsel i tilstand ${navn()}")
            legacyBehandling.nyTilstand(AvsluttetUtenVedtakMedVarsler)
        }

        override fun vedtakFattet(legacyBehandling: LegacyBehandling) {}
    }

    internal data object AvsluttetUtenVedtakMedVarsler : Tilstand {
        override fun navn(): String = "AvsluttetUtenVedtakMedVarsler"

        override fun håndterGodkjenning(legacyBehandling: LegacyBehandling) {
            legacyBehandling.nyTilstand(AvsluttetUtenVedtak)
        }

        override fun vedtakFattet(legacyBehandling: LegacyBehandling) {}
    }

    override fun toString(): String = "spesialistBehandlingId=$id, vedtaksperiodeId=$vedtaksperiodeId"

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is LegacyBehandling &&
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

    companion object {
        val logg: Logger = LoggerFactory.getLogger(LegacyBehandling::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun List<LegacyBehandling>.finnBehandlingForVedtaksperiode(vedtaksperiodeId: UUID): LegacyBehandling? = this.find { it.vedtaksperiodeId == vedtaksperiodeId }

        internal fun List<LegacyBehandling>.finnBehandlingForSpleisBehandling(spleisBehandlingId: UUID): LegacyBehandling? = this.find { it.spleisBehandlingId == spleisBehandlingId }

        internal fun List<LegacyBehandling>.finnSisteBehandlingUtenSpleisBehandlingId(): LegacyBehandling? = this.lastOrNull { it.spleisBehandlingId == null }

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
            vedtakBegrunnelse: VedtakBegrunnelse?,
        ) = LegacyBehandling(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            periode = Periode(fom, tom),
            tilstand = tilstand,
            tags = tags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            varsler = varsler,
        )

        internal fun List<LegacyBehandling>.håndterNyttVarsel(varsler: List<Varsel>) {
            forEach { behandling ->
                varsler.forEach { behandling.håndterNyttVarsel(it) }
            }
        }

        fun List<LegacyBehandling>.forhindrerAutomatisering(tilOgMed: LocalDate): Boolean =
            this
                .filter {
                    it.tilhører(tilOgMed)
                }.any { it.forhindrerAutomatisering() }

        internal fun List<LegacyBehandling>.forhindrerAutomatisering(legacyBehandling: LegacyBehandling): Boolean =
            this
                .filter {
                    it.tilhører(legacyBehandling.periode.tom)
                }.any { it.forhindrerAutomatisering() }

        internal fun List<LegacyBehandling>.harKunGosysvarsel(legacyBehandling: LegacyBehandling): Boolean =
            this
                .filter {
                    it.tilhører(legacyBehandling.periode.tom)
                }.filter { it.varsler.isNotEmpty() }
                .all { it.harKunGosysvarsel() }

        internal fun List<LegacyBehandling>.harVarselOmManglendeInntektsmelding(legacyBehandling: LegacyBehandling): Boolean =
            filter { it.tilhører(legacyBehandling.periode.tom) }
                .filter { it.varsler.isNotEmpty() }
                .any { it.harVarselOmManglendeInntektsmelding() }

        internal fun List<LegacyBehandling>.harVarselOmManglendeInntektsmelding(vedtaksperiodeId: UUID): Boolean = finnBehandlingForVedtaksperiode(vedtaksperiodeId)?.harVarselOmManglendeInntektsmelding() == true

        internal fun List<LegacyBehandling>.harMedlemskapsvarsel(vedtaksperiodeId: UUID): Boolean =
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).any {
                it.harMedlemskapsvarsel()
            }

        internal fun List<LegacyBehandling>.kreverSkjønnsfastsettelse(vedtaksperiodeId: UUID): Boolean =
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).any {
                it.kreverSkjønnsfastsettelse()
            }

        internal fun List<LegacyBehandling>.erTilbakedatert(vedtaksperiodeId: UUID): Boolean =
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).any {
                it.erTilbakedatert()
            }

        internal fun List<LegacyBehandling>.harÅpenGosysOppgave(vedtaksperiodeId: UUID): Boolean =
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).any {
                it.harKunÅpenGosysOppgave()
            }

        internal fun List<LegacyBehandling>.deaktiver(varsel: Varsel) {
            find { varsel.erRelevantFor(it.vedtaksperiodeId) }?.håndterDeaktivertVarsel(varsel)
        }

        internal fun List<LegacyBehandling>.flyttEventueltAvviksvarselTil(vedtaksperiodeId: UUID) {
            val behandlingForPeriodeTilGodkjenning =
                finnBehandlingForVedtaksperiode(vedtaksperiodeId) ?: run {
                    logg.warn("Finner ikke behandling for vedtaksperiode $vedtaksperiodeId, sjekker ikke om avviksvarsel skal flyttes")
                    return
                }
            val varsel =
                filterNot {
                    it == behandlingForPeriodeTilGodkjenning
                }.flatMap { it.varsler }.find { it.erVarselOmAvvik() && it.erAktiv() } ?: return

            val behandlingMedVarsel = first { behandling -> behandling.varsler.contains(varsel) }
            logg.info(
                "Flytter et ikke-vurdert avviksvarsel fra vedtaksperiode ${behandlingMedVarsel.vedtaksperiodeId} til vedtaksperiode $vedtaksperiodeId",
            )
            behandlingMedVarsel.varsler.remove(varsel)
            behandlingForPeriodeTilGodkjenning.varsler.add(varsel)
        }

        internal fun List<LegacyBehandling>.håndterGodkjent(vedtaksperiodeId: UUID) {
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).forEach {
                it.håndterGodkjentAvSaksbehandler()
            }
        }

        private fun List<LegacyBehandling>.overlapperMedEllerTidligereEnn(vedtaksperiodeId: UUID): List<LegacyBehandling> {
            val gjeldende = find { it.vedtaksperiodeId == vedtaksperiodeId } ?: return emptyList()
            return sortedByDescending { it.periode.tom }
                .filter { it.periode.fom <= gjeldende.periode.tom }
        }
    }
}
