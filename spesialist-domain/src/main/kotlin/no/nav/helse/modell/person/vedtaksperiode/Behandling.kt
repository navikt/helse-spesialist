package no.nav.helse.modell.person.vedtaksperiode

import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderAktivtVarselOmAvvik
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmAvvik
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmNegativtBeløp
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmTilbakedatering
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmÅpenGosysOppgave
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class Behandling private constructor(
    private val id: UUID,
    private val vedtaksperiodeId: UUID,
    utbetalingId: UUID?,
    private var spleisBehandlingId: UUID?,
    private var skjæringstidspunkt: LocalDate,
    private var periode: Periode,
    private var tilstand: Tilstand,
    private var tags: List<String>,
    private val vedtakBegrunnelse: VedtakBegrunnelse?,
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

    private val varsler: MutableList<Varsel> = varsler.toMutableList()

    internal var utbetalingId: UUID? = utbetalingId
        private set

    internal fun spleisBehandlingId() = spleisBehandlingId

    internal fun skjæringstidspunkt() = skjæringstidspunkt

    fun vedtaksperiodeId() = vedtaksperiodeId

    fun varsler(): List<Varsel> = varsler.toList()

    internal fun unikId() = id

    internal fun hasterÅBehandle() = varsler.inneholderVarselOmNegativtBeløp()

    internal fun fom() = periode.fom()

    internal fun tom() = periode.tom()

    fun toDto(): BehandlingDto =
        BehandlingDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = periode.fom(),
            tom = periode.tom(),
            tilstand = tilstand.toDto(),
            tags = tags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            varsler = varsler.map(Varsel::toDto),
        )

    internal fun tilhører(dato: LocalDate): Boolean = periode.tom() <= dato

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

    internal fun håndterVedtakFattet() {
        tilstand.vedtakFattet(this)
    }

    internal fun avsluttetUtenVedtak() {
        tilstand.avsluttetUtenVedtak(this)
    }

    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        if (tags.isEmpty()) {
            sikkerlogg.error(
                "Ingen tags funnet for spleisBehandlingId: $spleisBehandlingId på vedtaksperiodeId: $vedtaksperiodeId",
            )
        }

        vedtakBuilder.tags(tags)
        vedtakBuilder.vedtaksperiodeId(vedtaksperiodeId)
        vedtakBuilder.spleisBehandlingId(behandlingId())
        vedtakBuilder.utbetalingId(utbetalingId())
        vedtakBuilder.skjæringstidspunkt(skjæringstidspunkt)
        vedtakBuilder.fom(fom())
        vedtakBuilder.tom(tom())
        vedtakBegrunnelse?.also { vedtakBuilder.vedtakBegrunnelse(it) }
    }

    private fun behandlingId(): UUID {
        return spleisBehandlingId ?: throw IllegalStateException("Forventer at spleisBehandlingId er satt")
    }

    private fun utbetalingId(): UUID {
        return utbetalingId ?: throw IllegalStateException("Forventer at utbetalingId er satt")
    }

    private fun nyTilstand(ny: Tilstand) {
        this.tilstand = ny
    }

    private fun nyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
    }

    private fun nyBehandling(spleisBehandling: SpleisBehandling): Behandling {
        val nyBehandling =
            Behandling(
                id = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiodeId,
                fom = spleisBehandling.fom,
                tom = spleisBehandling.tom,
                skjæringstidspunkt = skjæringstidspunkt,
                spleisBehandlingId = spleisBehandling.spleisBehandlingId,
            )
        flyttAktiveVarslerTil(nyBehandling)
        return nyBehandling
    }

    private fun flyttAktiveVarslerTil(behandling: Behandling) {
        val aktiveVarsler = varsler.filter(Varsel::erAktiv)
        this.varsler.removeAll(aktiveVarsler)
        behandling.varsler.addAll(aktiveVarsler)
        if (aktiveVarsler.isNotEmpty()) {
            sikkerlogg.info(
                "Flytter ${aktiveVarsler.size} varsler fra {} til {}. Gammel behandling har {}",
                kv("gammel_behandling", this.id),
                kv("ny_behandling", behandling.id),
                kv("utbetalingId", this.utbetalingId),
            )
        }
    }

    private fun nyttVarsel(varsel: Varsel) {
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

        fun avsluttetUtenVedtak(behandling: Behandling): Unit =
            throw IllegalStateException("Forventer ikke avsluttet_uten_vedtak i tilstand=${this::class.simpleName}")

        fun vedtakFattet(behandling: Behandling) {
            sikkerlogg.info("Forventet ikke vedtak_fattet i {}", kv("tilstand", this::class.simpleName))
        }

        fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            behandling: Behandling,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
        }

        fun nyUtbetaling(
            behandling: Behandling,
            utbetalingId: UUID,
        ) {
            sikkerlogg.error(
                "Mottatt ny utbetaling med {} for {} i {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("behandling", behandling),
                keyValue("tilstand", this::class.simpleName),
            )
            logg.error(
                "Mottatt ny utbetaling med {} i {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("tilstand", this::class.simpleName),
            )
        }

        fun invaliderUtbetaling(
            behandling: Behandling,
            utbetalingId: UUID,
        ) {
            logg.error(
                "Utbetaling med {} ble forsøkt forkastet, men det støttes ikke for {} som er i {}.",
                keyValue("Behandling", behandling),
                keyValue("utbetalingId", utbetalingId),
                keyValue("tilstand", this::class.simpleName),
            )
            sikkerlogg.error(
                "Utbetaling med {} ble forsøkt forkastet, men det støttes ikke for {} som er i {}.",
                keyValue("Behandling", behandling),
                keyValue("utbetalingId", utbetalingId),
                keyValue("tilstand", this::class.simpleName),
            )
        }

        fun nyttVarsel(behandling: Behandling) {}

        fun håndterGodkjenning(behandling: Behandling) {}

        fun oppdaterBehandlingsinformasjon(
            behandling: Behandling,
            tags: List<String>,
            spleisBehandlingId: UUID,
            utbetalingId: UUID,
        ): Unit = throw IllegalStateException("Mottatt godkjenningsbehov i tilstand=${navn()}")
    }

    internal data object VidereBehandlingAvklares : Tilstand {
        override fun navn(): String = "VidereBehandlingAvklares"

        override fun nyUtbetaling(
            behandling: Behandling,
            utbetalingId: UUID,
        ) {
            behandling.nyUtbetaling(utbetalingId)
            behandling.nyTilstand(KlarTilBehandling)
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            behandling: Behandling,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            behandling.spleisVedtaksperiode(spleisVedtaksperiode)
        }

        override fun avsluttetUtenVedtak(behandling: Behandling) {
            check(
                behandling.utbetalingId == null,
            ) { "Mottatt avsluttet_uten_vedtak på behandling som har utbetaling. Det gir ingen mening." }
            val nesteTilstand =
                when {
                    behandling.varsler.isNotEmpty() -> AvsluttetUtenVedtakMedVarsler
                    else -> AvsluttetUtenVedtak
                }
            behandling.nyTilstand(nesteTilstand)
        }
    }

    internal data object KlarTilBehandling : Tilstand {
        override fun navn(): String = "KlarTilBehandling"

        override fun vedtakFattet(behandling: Behandling) {
            checkNotNull(behandling.utbetalingId) { "Mottatt vedtak_fattet i tilstand=${navn()}, men mangler utbetalingId" }
            behandling.nyTilstand(VedtakFattet)
        }

        override fun oppdaterBehandlingsinformasjon(
            behandling: Behandling,
            tags: List<String>,
            spleisBehandlingId: UUID,
            utbetalingId: UUID,
        ) {
            behandling.tags = tags
            behandling.spleisBehandlingId = spleisBehandlingId
            behandling.utbetalingId = utbetalingId
        }

        override fun invaliderUtbetaling(
            behandling: Behandling,
            utbetalingId: UUID,
        ) {
            behandling.utbetalingId = null
            behandling.nyTilstand(VidereBehandlingAvklares)
        }

        override fun spleisVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            behandling: Behandling,
            spleisVedtaksperiode: SpleisVedtaksperiode,
        ) {
            behandling.spleisVedtaksperiode(spleisVedtaksperiode)
        }
    }

    data object VedtakFattet : Tilstand {
        override fun navn(): String = "VedtakFattet"
    }

    internal data object AvsluttetUtenVedtak : Tilstand {
        override fun navn(): String = "AvsluttetUtenVedtak"

        override fun nyttVarsel(behandling: Behandling) {
            sikkerlogg.warn("Mottar nytt varsel i tilstand ${navn()}")
            behandling.nyTilstand(AvsluttetUtenVedtakMedVarsler)
        }

        override fun vedtakFattet(behandling: Behandling) {}
    }

    internal data object AvsluttetUtenVedtakMedVarsler : Tilstand {
        override fun navn(): String = "AvsluttetUtenVedtakMedVarsler"

        override fun håndterGodkjenning(behandling: Behandling) {
            behandling.nyTilstand(AvsluttetUtenVedtak)
        }

        override fun vedtakFattet(behandling: Behandling) {}
    }

    override fun toString(): String = "spesialistBehandlingId=$id, vedtaksperiodeId=$vedtaksperiodeId"

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is Behandling &&
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
        val logg: Logger = LoggerFactory.getLogger(Behandling::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun List<Behandling>.finnBehandlingForVedtaksperiode(vedtaksperiodeId: UUID): Behandling? =
            this.find { it.vedtaksperiodeId == vedtaksperiodeId }

        internal fun List<Behandling>.finnBehandlingForSpleisBehandling(spleisBehandlingId: UUID): Behandling? =
            this.find { it.spleisBehandlingId == spleisBehandlingId }

        internal fun List<Behandling>.finnSisteBehandlingUtenSpleisBehandlingId(): Behandling? =
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
            vedtakBegrunnelse: VedtakBegrunnelse?,
        ) = Behandling(
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

        internal fun List<Behandling>.håndterNyttVarsel(varsler: List<Varsel>) {
            forEach { behandling ->
                varsler.forEach { behandling.håndterNyttVarsel(it) }
            }
        }

        fun List<Behandling>.forhindrerAutomatisering(tilOgMed: LocalDate): Boolean =
            this
                .filter {
                    it.tilhører(tilOgMed)
                }.any { it.forhindrerAutomatisering() }

        internal fun List<Behandling>.forhindrerAutomatisering(behandling: Behandling): Boolean =
            this
                .filter {
                    it.tilhører(behandling.periode.tom())
                }.any { it.forhindrerAutomatisering() }

        internal fun List<Behandling>.harKunGosysvarsel(behandling: Behandling): Boolean =
            this
                .filter {
                    it.tilhører(behandling.periode.tom())
                }.filter { it.varsler.isNotEmpty() }
                .all { it.harKunGosysvarsel() }

        internal fun List<Behandling>.harVarselOmManglendeInntektsmelding(behandling: Behandling): Boolean =
            filter { it.tilhører(behandling.periode.tom()) }
                .filter { it.varsler.isNotEmpty() }
                .any { it.harVarselOmManglendeInntektsmelding() }

        internal fun List<Behandling>.harMedlemskapsvarsel(vedtaksperiodeId: UUID): Boolean =
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).any {
                it.harMedlemskapsvarsel()
            }

        internal fun List<Behandling>.kreverSkjønnsfastsettelse(vedtaksperiodeId: UUID): Boolean =
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).any {
                it.kreverSkjønnsfastsettelse()
            }

        internal fun List<Behandling>.erTilbakedatert(vedtaksperiodeId: UUID): Boolean =
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).any {
                it.erTilbakedatert()
            }

        internal fun List<Behandling>.harÅpenGosysOppgave(vedtaksperiodeId: UUID): Boolean =
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).any {
                it.harKunÅpenGosysOppgave()
            }

        internal fun List<Behandling>.deaktiver(varsel: Varsel) {
            find { varsel.erRelevantFor(it.vedtaksperiodeId) }?.håndterDeaktivertVarsel(varsel)
        }

        internal fun List<Behandling>.flyttEventueltAvviksvarselTil(vedtaksperiodeId: UUID) {
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

        internal fun List<Behandling>.håndterGodkjent(vedtaksperiodeId: UUID) {
            overlapperMedEllerTidligereEnn(vedtaksperiodeId).forEach {
                it.håndterGodkjentAvSaksbehandler()
            }
        }

        private fun List<Behandling>.overlapperMedEllerTidligereEnn(vedtaksperiodeId: UUID): List<Behandling> {
            val gjeldende = find { it.vedtaksperiodeId == vedtaksperiodeId } ?: return emptyList()
            return sortedByDescending { it.periode.tom() }
                .filter { it.periode.fom() <= gjeldende.periode.tom() }
        }
    }
}
