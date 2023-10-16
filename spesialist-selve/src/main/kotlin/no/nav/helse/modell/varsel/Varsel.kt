package no.nav.helse.modell.varsel

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varsel.Status.VURDERT
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class Varsel(
    private val id: UUID,
    private val varselkode: String,
    private val opprettet: LocalDateTime,
    private val vedtaksperiodeId: UUID,
    private var status: Status = AKTIV
) {

    internal enum class Status {
        AKTIV,
        INAKTIV,
        GODKJENT,
        VURDERT,
        AVVIST;
        internal fun toDto(): VarselStatusDto {
            return when (this) {
                AKTIV -> VarselStatusDto.AKTIV
                INAKTIV -> VarselStatusDto.INAKTIV
                GODKJENT -> VarselStatusDto.GODKJENT
                VURDERT -> VarselStatusDto.VURDERT
                AVVIST -> VarselStatusDto.AVVIST
            }
        }
    }

    private val observers = mutableSetOf<IVedtaksperiodeObserver>()

    internal fun registrer(vararg observer: IVedtaksperiodeObserver) {
        observers.addAll(observer)
    }

    internal fun toDto(): VarselDto {
        return VarselDto(id, varselkode, opprettet, vedtaksperiodeId, status.toDto())
    }

    internal fun erAktiv(): Boolean = this.status == AKTIV

    internal fun opprett(generasjonId: UUID) {
        observers.forEach { it.varselOpprettet(id, vedtaksperiodeId, generasjonId, varselkode, opprettet) }
    }

    internal fun reaktiver(generasjonId: UUID) {
        if (status != INAKTIV) return
        this.status = AKTIV
        observers.forEach { it.varselReaktivert(id, varselkode, generasjonId, vedtaksperiodeId) }
    }

    internal fun deaktiver(generasjonId: UUID) {
        if(status != AKTIV) return
        this.status = INAKTIV
        observers.forEach { it.varselDeaktivert(id, varselkode, generasjonId, vedtaksperiodeId) }
    }

    private fun oppdaterGenerasjon(gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {
        observers.forEach { it.varselFlyttet(this.id, gammelGenerasjonId, nyGenerasjonId) }
    }

    override fun toString(): String {
        return "varselkode=$varselkode, vedtaksperiodeId=$vedtaksperiodeId, status=${status.name}"
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Varsel
        && javaClass == other.javaClass
        && id == other.id
        && vedtaksperiodeId == other.vedtaksperiodeId
        && opprettet.withNano(0) == other.opprettet.withNano(0)
        && varselkode == other.varselkode)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + varselkode.hashCode()
        result = 31 * result + opprettet.withNano(0).hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        return result
    }

    internal fun erRelevantFor(vedtaksperiodeId: UUID): Boolean = this.vedtaksperiodeId == vedtaksperiodeId

    internal companion object {
        internal fun List<Varsel>.flyttVarslerFor(gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {
            forEach { it.oppdaterGenerasjon(gammelGenerasjonId, nyGenerasjonId) }
        }

        internal fun List<Varsel>.finnEksisterendeVarsel(varsel: Varsel): Varsel? {
            return find { it.varselkode == varsel.varselkode }
        }
        internal fun List<Varsel>.inneholderMedlemskapsvarsel(): Boolean {
            return any { it.status == AKTIV && it.varselkode == "RV_MV_1" }
        }

        internal fun List<Varsel>.inneholderVarselOmNegativtBeløp(): Boolean {
            return any { it.status == AKTIV && it.varselkode == "RV_UT_23" }
        }

        internal fun List<Varsel>.inneholderSvartelistedeVarsler(): Boolean {
            return any { it.varselkode in svartelistedeVarsler }
        }

        private val svartelistedeVarsler = listOf(
            "RV_AY_3", "RV_AY_4", "RV_AY_5", "RV_AY_6", "RV_AY_7", "RV_AY_8", "RV_IT_3", "RV_OS_2", "RV_OS_3",
            "RV_SI_3", "RV_UT_21", "RV_UT_23", "RV_VV_8", "SB_RV_2"
        )

        internal fun List<Varsel>.forhindrerAutomatisering() = any { it.status in listOf(VURDERT, AKTIV, AVVIST) }

        internal fun JsonNode.varsler(): List<Varsel> {
            return this
                .filter { it["nivå"].asText() == "VARSEL" && it["varselkode"]?.asText() != null }
                .filter { it["kontekster"].any { kontekst -> kontekst["konteksttype"].asText() == "Vedtaksperiode" } }
                .map { jsonNode ->
                    val vedtaksperiodeId =
                        UUID.fromString(
                            jsonNode["kontekster"]
                                .find { it["konteksttype"].asText() == "Vedtaksperiode" }!!["kontekstmap"]
                                .get("vedtaksperiodeId").asText()
                        )
                    Varsel(
                        UUID.fromString(jsonNode["id"].asText()),
                        jsonNode["varselkode"].asText(),
                        jsonNode["tidsstempel"].asLocalDateTime(),
                        vedtaksperiodeId
                    )
                }
        }
    }
}