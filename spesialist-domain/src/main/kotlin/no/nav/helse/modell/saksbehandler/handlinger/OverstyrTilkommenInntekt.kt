package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OverstyrTilkommenInntekt private constructor(
    id: OverstyringId?,
    override val fødselsnummer: String,
    override val saksbehandlerOid: SaksbehandlerOid,
    override val eksternHendelseId: UUID,
    override val aktørId: String,
    override val vedtaksperiodeId: UUID,
    override val opprettet: LocalDateTime,
    override var ferdigstilt: Boolean,
    val nyEllerEndredeInntekter: List<NyEllerEndretInntekt>,
    val fjernedeInntekter: List<FjernetInntekt>,
    kobledeVedtaksperioder: List<UUID>,
) : Overstyring(id) {
    override val kobledeVedtaksperioder: MutableList<UUID> = kobledeVedtaksperioder.toMutableList()

    override fun utførAv(legacySaksbehandler: LegacySaksbehandler) {}

    override fun loggnavn(): String = "overstyr_tilkommen_inntekt"

    data class NyEllerEndretInntekt(val organisasjonsnummer: String, val perioder: List<PeriodeMedBeløp>) {
        data class PeriodeMedBeløp(val fom: LocalDate, val tom: LocalDate, val periodeBeløp: Double)
    }

    data class FjernetInntekt(val organisasjonsnummer: String, val perioder: List<PeriodeUtenBeløp>) {
        data class PeriodeUtenBeløp(val fom: LocalDate, val tom: LocalDate)
    }

    companion object {
        fun ny(
            fødselsnummer: String,
            saksbehandlerOid: SaksbehandlerOid,
            aktørId: String,
            vedtaksperiodeId: UUID,
            nyEllerEndredeInntekter: List<NyEllerEndretInntekt>,
            fjernedeInntekter: List<FjernetInntekt>,
        ): OverstyrTilkommenInntekt {
            return OverstyrTilkommenInntekt(
                id = null,
                fødselsnummer = fødselsnummer,
                saksbehandlerOid = saksbehandlerOid,
                eksternHendelseId = UUID.randomUUID(),
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = LocalDateTime.now(),
                ferdigstilt = false,
                nyEllerEndredeInntekter = nyEllerEndredeInntekter,
                fjernedeInntekter = fjernedeInntekter,
                kobledeVedtaksperioder = emptyList(),
            )
        }

        fun fraLagring(
            id: OverstyringId,
            fødselsnummer: String,
            saksbehandlerOid: SaksbehandlerOid,
            eksternHendelseId: UUID,
            aktørId: String,
            vedtaksperiodeId: UUID,
            opprettet: LocalDateTime,
            nyeEllerEndredeInntekter: List<NyEllerEndretInntekt>,
            fjernedeInntekter: List<FjernetInntekt>,
            ferdigstilt: Boolean,
            kobledeVedtaksperioder: List<UUID>,
        ): OverstyrTilkommenInntekt {
            return OverstyrTilkommenInntekt(
                id = id,
                fødselsnummer = fødselsnummer,
                saksbehandlerOid = saksbehandlerOid,
                eksternHendelseId = eksternHendelseId,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = opprettet,
                ferdigstilt = ferdigstilt,
                nyEllerEndredeInntekter = nyeEllerEndredeInntekter,
                fjernedeInntekter = fjernedeInntekter,
                kobledeVedtaksperioder = kobledeVedtaksperioder.toMutableList(),
            )
        }
    }
}
