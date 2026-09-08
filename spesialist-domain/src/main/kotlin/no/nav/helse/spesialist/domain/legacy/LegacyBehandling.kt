package no.nav.helse.spesialist.domain.legacy

import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderVarselOmAvvik
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
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
    periode: Periode,
    tilstand: Tilstand,
    tags: List<String>,
    varsler: Set<LegacyVarsel>,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
) {
    constructor(
        id: UUID,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        spleisBehandlingId: UUID? = null,
        utbetalingId: UUID? = null,
        yrkesaktivitetstype: Yrkesaktivitetstype,
    ) : this(
        id = id,
        vedtaksperiodeId = vedtaksperiodeId,
        utbetalingId = utbetalingId,
        spleisBehandlingId = spleisBehandlingId,
        skjæringstidspunkt = skjæringstidspunkt,
        periode = Periode(fom, tom),
        tilstand = Tilstand.VidereBehandlingAvklares,
        tags = emptyList(),
        varsler = emptySet(),
        yrkesaktivitetstype = yrkesaktivitetstype,
    )

    var spleisBehandlingId: UUID? = spleisBehandlingId
        private set

    var skjæringstidspunkt: LocalDate = skjæringstidspunkt
        private set

    var periode: Periode = periode
        private set

    var tilstand: Tilstand = tilstand
        private set

    var tags: List<String> = tags
        private set

    private val varsler: MutableList<LegacyVarsel> = varsler.toMutableList()

    var utbetalingId: UUID? = utbetalingId
        private set

    internal fun spleisBehandlingId() = spleisBehandlingId

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
            varsler = varsler.map(LegacyVarsel::toDto),
            yrkesaktivitetstype = yrkesaktivitetstype,
        )

    internal fun tilhører(dato: LocalDate): Boolean = periode.tom <= dato

    fun håndterNyUtbetaling(utbetalingId: UUID) {
        when (tilstand) {
            Tilstand.VidereBehandlingAvklares -> {
                nyUtbetaling(utbetalingId)
                nyTilstand(Tilstand.KlarTilBehandling)
            }
            else -> {
                sikkerlogg.error(
                    "Mottatt ny utbetaling med {} for {} i {}",
                    keyValue("utbetalingId", utbetalingId),
                    keyValue("behandling", this),
                    keyValue("tilstand", tilstand.navn()),
                )
                logg.error(
                    "Mottatt ny utbetaling med {} i {}",
                    keyValue("utbetalingId", utbetalingId),
                    keyValue("tilstand", tilstand.navn()),
                )
            }
        }
    }

    internal fun håndterForkastetUtbetaling(utbetalingId: UUID) {
        if (utbetalingId != this.utbetalingId) return
        when (tilstand) {
            Tilstand.KlarTilBehandling -> {
                this.utbetalingId = null
                nyTilstand(Tilstand.VidereBehandlingAvklares)
            }
            else -> {
                logg.error(
                    "Utbetaling med {} ble forsøkt forkastet, men det støttes ikke for {} som er i {}.",
                    keyValue("Behandling", this),
                    keyValue("utbetalingId", utbetalingId),
                    keyValue("tilstand", tilstand.navn()),
                )
                sikkerlogg.error(
                    "Utbetaling med {} ble forsøkt forkastet, men det støttes ikke for {} som er i {}.",
                    keyValue("Behandling", this),
                    keyValue("utbetalingId", utbetalingId),
                    keyValue("tilstand", tilstand.navn()),
                )
            }
        }
    }

    fun håndterNyttVarsel(varsel: LegacyVarsel) {
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

    internal fun oppdaterBehandlingsinformasjon(
        tags: List<String>,
        spleisBehandlingId: UUID,
        utbetalingId: UUID,
    ) {
        when (tilstand) {
            Tilstand.KlarTilBehandling -> {
                this.tags = tags
                this.spleisBehandlingId = spleisBehandlingId
                this.utbetalingId = utbetalingId
            }
            else -> throw IllegalStateException("Mottatt godkjenningsbehov i tilstand=${tilstand.navn()}")
        }
    }

    fun håndterVedtakFattet() {
        when (tilstand) {
            Tilstand.KlarTilBehandling -> {
                checkNotNull(utbetalingId) { "Mottatt vedtak_fattet i tilstand=${tilstand.navn()}, men mangler utbetalingId" }
                nyTilstand(Tilstand.VedtakFattet)
            }
            Tilstand.AvsluttetUtenVedtak, Tilstand.AvsluttetUtenVedtakMedVarsler -> {}
            else -> sikkerlogg.info("Forventet ikke vedtak_fattet i {}", kv("tilstand", tilstand.navn()))
        }
    }

    private fun nyTilstand(ny: Tilstand) {
        this.tilstand = ny
    }

    private fun nyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
    }

    private fun nyttVarsel(varsel: LegacyVarsel) {
        logg.info("Legger til varsel $varsel")
        varsler.add(varsel)
        if (tilstand == Tilstand.AvsluttetUtenVedtak) {
            sikkerlogg.warn("Mottar nytt varsel i tilstand ${tilstand.navn()}")
            nyTilstand(Tilstand.AvsluttetUtenVedtakMedVarsler)
        }
    }

    enum class Tilstand {
        VidereBehandlingAvklares,
        KlarTilBehandling,
        VedtakFattet,
        AvsluttetUtenVedtak,
        AvsluttetUtenVedtakMedVarsler,
        ;

        fun navn(): String =
            when (this) {
                VidereBehandlingAvklares -> "VidereBehandlingAvklares"
                KlarTilBehandling -> "KlarTilBehandling"
                VedtakFattet -> "VedtakFattet"
                AvsluttetUtenVedtak -> "AvsluttetUtenVedtak"
                AvsluttetUtenVedtakMedVarsler -> "AvsluttetUtenVedtakMedVarsler"
            }

        fun toDto(): TilstandDto =
            when (this) {
                VidereBehandlingAvklares -> TilstandDto.VidereBehandlingAvklares
                KlarTilBehandling -> TilstandDto.KlarTilBehandling
                VedtakFattet -> TilstandDto.VedtakFattet
                AvsluttetUtenVedtak -> TilstandDto.AvsluttetUtenVedtak
                AvsluttetUtenVedtakMedVarsler -> TilstandDto.AvsluttetUtenVedtakMedVarsler
            }
    }

    override fun toString(): String = "LegacyBehandling(spesialistBehandlingId=$id, vedtaksperiodeId=$vedtaksperiodeId, spleisBehandlingId=$spleisBehandlingId, fom=${periode.fom}, tom=${periode.tom}, skjæringstidspunkt=$skjæringstidspunkt)"

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

        fun fraLagring(
            id: UUID,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID?,
            spleisBehandlingId: UUID?,
            skjæringstidspunkt: LocalDate,
            fom: LocalDate,
            tom: LocalDate,
            tilstand: Tilstand,
            tags: List<String>,
            varsler: Set<LegacyVarsel>,
            yrkesaktivitetstype: Yrkesaktivitetstype,
        ) = LegacyBehandling(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            periode = Periode(fom, tom),
            tilstand = tilstand,
            tags = tags,
            varsler = varsler,
            yrkesaktivitetstype = yrkesaktivitetstype,
        )
    }
}
