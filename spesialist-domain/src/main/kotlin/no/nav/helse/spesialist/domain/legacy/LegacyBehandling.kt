package no.nav.helse.spesialist.domain.legacy

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderAktivtVarselOmAvvik
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderVarselOmAvvik
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderVarselOmNegativtBeløp
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderVarselOmTilbakedatering
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderVarselOmÅpenGosysOppgave
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
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

    fun skjæringstidspunkt() = skjæringstidspunkt

    fun vedtaksperiodeId() = vedtaksperiodeId

    fun varsler(): List<LegacyVarsel> = varsler.toList()

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
            varsler = varsler.map(LegacyVarsel::toDto),
            yrkesaktivitetstype = yrkesaktivitetstype,
        )

    internal fun tilhører(dato: LocalDate): Boolean = periode.tom <= dato

    internal fun forhindrerAutomatisering(): Boolean = varsler.forhindrerAutomatisering()

    internal fun harKunGosysvarsel() = varsler.size == 1 && varsler.single().erGosysvarsel()

    internal fun harVarselOmManglendeInntektsmelding() = varsler.any { it.erVarselOmManglendeInntektsmelding() }

    internal fun håndter(
        spleisVedtaksperiode: SpleisVedtaksperiode,
    ) {
        this.periode = Periode(spleisVedtaksperiode.fom, spleisVedtaksperiode.tom)
        this.skjæringstidspunkt = spleisVedtaksperiode.skjæringstidspunkt
        this.spleisBehandlingId = spleisVedtaksperiode.spleisBehandlingId
    }

    fun håndterNyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
        this.tilstand = Tilstand.KlarTilBehandling
    }

    internal fun håndterForkastetUtbetaling(utbetalingId: UUID) {
        if (utbetalingId != this.utbetalingId) return
        this.utbetalingId = null
        this.tilstand = Tilstand.VidereBehandlingAvklares
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

    internal fun håndterDeaktivertVarsel(varsel: LegacyVarsel) {
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
        this.tags = tags
        this.spleisBehandlingId = spleisBehandlingId
        this.utbetalingId = utbetalingId
    }

    fun håndterVedtakFattet() {
        this.tilstand = Tilstand.VedtakFattet
    }

    fun behandlingId(): UUID = spleisBehandlingId ?: throw IllegalStateException("Forventer at spleisBehandlingId er satt")

    fun utbetalingId(): UUID = utbetalingId ?: throw IllegalStateException("Forventer at utbetalingId er satt")

    private fun nyttVarsel(varsel: LegacyVarsel) {
        logg.info("Legger til varsel $varsel")
        varsler.add(varsel)
        if (tilstand == Tilstand.AvsluttetUtenVedtak) {
            sikkerlogg.warn("Mottar nytt varsel i tilstand ${tilstand.name}")
            tilstand = Tilstand.AvsluttetUtenVedtakMedVarsler
        }
    }

    private fun harMedlemskapsvarsel(): Boolean {
        val inneholderMedlemskapsvarsel = varsler.inneholderMedlemskapsvarsel()
        logg.info("Behandling $this har medlemskapsvarsel: $inneholderMedlemskapsvarsel")
        return inneholderMedlemskapsvarsel
    }

    private fun kreverSkjønnsfastsettelse(): Boolean {
        val inneholderAvviksvarsel = varsler.inneholderAktivtVarselOmAvvik()
        logg.info("Behandling $this har varsel om avvik: $inneholderAvviksvarsel")
        return inneholderAvviksvarsel
    }

    private fun erTilbakedatert(): Boolean {
        val inneholderTilbakedateringsvarsel = varsler.inneholderVarselOmTilbakedatering()
        logg.info("Behandling $this har varsel om tilbakedatering: $inneholderTilbakedateringsvarsel")
        return inneholderTilbakedateringsvarsel
    }

    private fun harKunVarselOmÅpenGosysOppgave(): Boolean {
        val inneholderKunÅpenGosysOppgaveVarsel = varsler.inneholderVarselOmÅpenGosysOppgave() && varsler.size == 1
        logg.info("Behandling $this har kun varsel om åpen Gosys-oppgave: $inneholderKunÅpenGosysOppgaveVarsel")
        return inneholderKunÅpenGosysOppgaveVarsel
    }

    enum class Tilstand {
        VidereBehandlingAvklares,
        KlarTilBehandling,
        VedtakFattet,
        AvsluttetUtenVedtak,
        AvsluttetUtenVedtakMedVarsler,
        ;

        fun toDto(): TilstandDto =
            when (this) {
                AvsluttetUtenVedtak -> TilstandDto.AvsluttetUtenVedtak
                VedtakFattet -> TilstandDto.VedtakFattet
                VidereBehandlingAvklares -> TilstandDto.VidereBehandlingAvklares
                AvsluttetUtenVedtakMedVarsler -> TilstandDto.AvsluttetUtenVedtakMedVarsler
                KlarTilBehandling -> TilstandDto.KlarTilBehandling
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

        internal fun List<LegacyBehandling>.finnBehandlingForSpleisBehandling(spleisBehandlingId: UUID): LegacyBehandling? = this.find { it.spleisBehandlingId == spleisBehandlingId }

        internal fun List<LegacyBehandling>.finnSisteBehandlingUtenSpleisBehandlingId(): LegacyBehandling? = this.lastOrNull { it.spleisBehandlingId == null }

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

        internal fun List<LegacyBehandling>.håndterNyttVarsel(varsler: List<LegacyVarsel>) {
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
                it.harKunVarselOmÅpenGosysOppgave()
            }

        internal fun List<LegacyBehandling>.deaktiver(varsel: LegacyVarsel) {
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

        private fun List<LegacyBehandling>.overlapperMedEllerTidligereEnn(vedtaksperiodeId: UUID): List<LegacyBehandling> {
            val gjeldende = find { it.vedtaksperiodeId == vedtaksperiodeId } ?: return emptyList()
            return sortedByDescending { it.periode.tom }
                .filter { it.periode.fom <= gjeldende.periode.tom }
        }
    }
}
