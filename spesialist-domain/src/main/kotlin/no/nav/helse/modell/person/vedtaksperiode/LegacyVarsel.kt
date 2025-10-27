package no.nav.helse.modell.person.vedtaksperiode

import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Status.AKTIV
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Status.AVVIST
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Status.INAKTIV
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Status.VURDERT
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class LegacyVarsel(
    val id: UUID,
    val varselkode: String,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    status: Status = AKTIV,
) {
    var status: Status = status
        private set

    enum class Status {
        AKTIV,
        INAKTIV,
        GODKJENT,
        VURDERT,
        AVVIST,
        AVVIKLET,
        ;

        fun toDto(): VarselStatusDto =
            when (this) {
                AKTIV -> VarselStatusDto.AKTIV
                INAKTIV -> VarselStatusDto.INAKTIV
                GODKJENT -> VarselStatusDto.GODKJENT
                VURDERT -> VarselStatusDto.VURDERT
                AVVIST -> VarselStatusDto.AVVIST
                AVVIKLET -> VarselStatusDto.AVVIKLET
            }
    }

    fun toDto(): VarselDto = VarselDto(id, varselkode, opprettet, vedtaksperiodeId, status.toDto())

    internal fun erAktiv(): Boolean = this.status == AKTIV

    internal fun erVarselOmAvvik(): Boolean = this.varselkode == "RV_IV_2"

    internal fun reaktiver() {
        if (status != INAKTIV) return
        logg.info("Reaktiverer varsel $varselkode for vedtaksperiode $vedtaksperiodeId")
        this.status = AKTIV
    }

    internal fun deaktiver() {
        if (status != AKTIV) return
        logg.info("Deaktiverer varsel $varselkode")
        this.status = INAKTIV
    }

    internal fun erGosysvarsel() = varselkode == "SB_EX_1"

    internal fun erVarselOmManglendeInntektsmelding() = varselkode == "RV_IV_10"

    override fun toString(): String = "varselkode=$varselkode, vedtaksperiodeId=$vedtaksperiodeId, status=${status.name}"

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is LegacyVarsel &&
                    javaClass == other.javaClass &&
                    id == other.id &&
                    vedtaksperiodeId == other.vedtaksperiodeId &&
                    opprettet.withNano(0) == other.opprettet.withNano(0) &&
                    varselkode == other.varselkode
            )

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + varselkode.hashCode()
        result = 31 * result + opprettet.withNano(0).hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        return result
    }

    internal fun erRelevantFor(vedtaksperiodeId: UUID): Boolean = this.vedtaksperiodeId == vedtaksperiodeId

    companion object {
        internal fun List<LegacyVarsel>.finnEksisterendeVarsel(varsel: LegacyVarsel): LegacyVarsel? = find { it.varselkode == varsel.varselkode }

        internal fun List<LegacyVarsel>.finnEksisterendeVarsel(varselkode: String): LegacyVarsel? = find { it.varselkode == varselkode }

        internal fun List<LegacyVarsel>.inneholderMedlemskapsvarsel(): Boolean = any { it.status == AKTIV && it.varselkode == "RV_MV_1" }

        internal fun List<LegacyVarsel>.inneholderVarselOmNegativtBeløp(): Boolean = any { it.status == AKTIV && it.varselkode == "RV_UT_23" }

        internal fun List<LegacyVarsel>.inneholderAktivtVarselOmAvvik(): Boolean = any { it.status == AKTIV && it.varselkode == "RV_IV_2" }

        fun List<LegacyVarsel>.inneholderVarselOmAvvik(): Boolean = any { it.varselkode == "RV_IV_2" }

        internal fun List<LegacyVarsel>.inneholderVarselOmTilbakedatering(): Boolean = any { it.status == AKTIV && it.varselkode == "RV_SØ_3" }

        internal fun List<LegacyVarsel>.inneholderVarselOmÅpenGosysOppgave(): Boolean = any { it.status == AKTIV && it.varselkode == "SB_EX_1" }

        internal fun List<LegacyVarsel>.forhindrerAutomatisering() = any { it.status in listOf(VURDERT, AKTIV, AVVIST) }
    }
}

private val logg = LoggerFactory.getLogger(LegacyVarsel::class.java)
