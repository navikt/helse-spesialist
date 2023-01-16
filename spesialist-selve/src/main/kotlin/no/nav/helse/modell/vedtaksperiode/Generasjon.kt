package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.avvisAlleFor
import no.nav.helse.modell.varsel.Varsel.Companion.deaktiverFor
import no.nav.helse.modell.varsel.Varsel.Companion.godkjennAlleFor
import no.nav.helse.modell.varsel.Varsel.Companion.godkjennFor
import no.nav.helse.modell.varsel.Varsel.Companion.reaktiverFor
import no.nav.helse.modell.varsel.VarselRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class Generasjon private constructor(
    private val id: UUID,
    private val vedtaksperiodeId: UUID,
    private var utbetalingId: UUID?,
    private var låst: Boolean,
    varsler: Set<Varsel>,
    private val generasjonRepository: GenerasjonRepository
) {
    internal constructor(
        id: UUID,
        vedtaksperiodeId: UUID,
        generasjonRepository: GenerasjonRepository
    ): this(id, vedtaksperiodeId, null, false, emptySet(), generasjonRepository)

    internal constructor(
        id: UUID,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID?,
        låst: Boolean,
        varsler: Set<Varsel>,
        dataSource: DataSource
    ): this(id, vedtaksperiodeId, utbetalingId, låst, varsler, ActualGenerasjonRepository(dataSource))

    private val varsler: MutableList<Varsel> = varsler.toMutableList()

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun håndterNyGenerasjon(
        hendelseId: UUID,
        id: UUID = UUID.randomUUID(),
    ): Generasjon? {
        if (!låst) {
            sikkerlogg.info(
                "Oppretter ikke ny generasjon for {} da nåværende generasjon med {} er ulåst",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("generasjonId", this.id)
            )
            return null
        }
        return generasjonRepository.opprettNeste(id, vedtaksperiodeId, hendelseId)
    }

    internal fun håndterNyUtbetaling(hendelseId: UUID, utbetalingId: UUID) {
        if (låst) return run {
            val nyGenerasjonId = UUID.randomUUID()
            sikkerlogg.info(
                "Kan ikke legge til ny utbetaling med {} for {}, da generasjonen er låst. Oppretter ny generasjon med {}",
                keyValue("utbetalingId", utbetalingId),
                keyValue("generasjon", this),
                keyValue("generasjonId", nyGenerasjonId)
            )
            håndterNyGenerasjon(hendelseId, nyGenerasjonId)?.håndterNyUtbetaling(utbetalingId)
        }
        håndterNyUtbetaling(utbetalingId)
    }

    private fun håndterNyUtbetaling(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
        generasjonRepository.utbetalingFor(id, utbetalingId)
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
        generasjonRepository.fjernUtbetalingFor(id)
    }

    internal fun håndterVarsel(varselId: UUID, varselkode: String, opprettet: LocalDateTime, varselRepository: VarselRepository) {
        if (låst) return sikkerlogg.info(
            "Kan ikke lagre varsel {} på låst generasjon {}",
            keyValue("varselId", varselId),
            keyValue("generasjon", this)
        )
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
        val utbetalingId = checkNotNull(utbetalingId) { "Mangler utbetalingId for generasjon. Det skal ikke være mulig." }
        val allePerioderForUtbetaling = generasjonRepository.tilhørendeFor(utbetalingId)
        allePerioderForUtbetaling.forEach {
            it.varsler.godkjennAlleFor(it.id, ident, varselRepository)
        }
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
        generasjonRepository.låsFor(id, hendelseId)
    }

    override fun toString(): String = "generasjonId=$id, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, låst=$låst"

    override fun equals(other: Any?): Boolean =
        this === other || (other is Generasjon
                && javaClass == other.javaClass
                && id == other.id
                && vedtaksperiodeId == other.vedtaksperiodeId
                && låst == other.låst
                && utbetalingId == other.utbetalingId)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + utbetalingId.hashCode()
        result = 31 * result + låst.hashCode()
        return result
    }

}