package no.nav.helse.modell.periodehistorikk

import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Saksbehandler
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface Historikkinnslag {
    val dialogRef: Long?
    val saksbehandler: Saksbehandler?
    val tidspunkt: LocalDateTime

    fun detaljer(): Map<String, Any?> = emptyMap()

    companion object {
        fun lagtPåVentInnslag(
            notattekst: String?,
            saksbehandler: Saksbehandler,
            årsaker: List<PåVentÅrsak>,
            frist: LocalDate,
            dialogRef: Long,
        ): LagtPåVent =
            LagtPåVent(
                notattekst = notattekst,
                saksbehandler = saksbehandler,
                årsaker = årsaker,
                tidspunkt = LocalDateTime.now(),
                frist = frist,
                dialogRef = dialogRef,
            )

        fun fjernetFraPåVentInnslag(saksbehandler: Saksbehandler): FjernetFraPåVent =
            FjernetFraPåVent(
                saksbehandler = saksbehandler,
                tidspunkt = LocalDateTime.now(),
            )

        fun endrePåVentInnslag(
            notattekst: String?,
            saksbehandler: Saksbehandler,
            årsaker: List<PåVentÅrsak>,
            frist: LocalDate,
            dialogRef: Long,
        ): EndrePåVent =
            EndrePåVent(
                notattekst = notattekst,
                saksbehandler = saksbehandler,
                årsaker = årsaker,
                tidspunkt = LocalDateTime.now(),
                frist = frist,
                dialogRef = dialogRef,
            )

        fun totrinnsvurderingFerdigbehandletInnslag(saksbehandler: Saksbehandler): TotrinnsvurderingFerdigbehandlet = TotrinnsvurderingFerdigbehandlet(saksbehandler = saksbehandler, tidspunkt = LocalDateTime.now())

        fun avventerTotrinnsvurdering(saksbehandler: Saksbehandler): AvventerTotrinnsvurdering = AvventerTotrinnsvurdering(saksbehandler = saksbehandler, tidspunkt = LocalDateTime.now())

        fun totrinnsvurderingRetur(
            notattekst: String,
            saksbehandler: Saksbehandler,
            dialogRef: Long,
        ): TotrinnsvurderingRetur =
            TotrinnsvurderingRetur(
                notattekst = notattekst,
                saksbehandler = saksbehandler,
                tidspunkt = LocalDateTime.now(),
                dialogRef = dialogRef,
            )

        fun totrinnsvurderingAutomatiskRetur(): TotrinnsvurderingAutomatiskRetur = TotrinnsvurderingAutomatiskRetur(tidspunkt = LocalDateTime.now())

        fun automatiskBehandlingStanset(): AutomatiskBehandlingStanset = AutomatiskBehandlingStanset(tidspunkt = LocalDateTime.now())

        fun automatiskBehandlingStansetAvSaksbehandler(
            saksbehandler: Saksbehandler,
            begrunnelse: String,
            dialogId: DialogId,
        ): AutomatiskBehandlingStansetAvSaksbehandler =
            AutomatiskBehandlingStansetAvSaksbehandler(
                tidspunkt = LocalDateTime.now(),
                saksbehandler = saksbehandler,
                dialogRef = dialogId.value,
                notattekst = begrunnelse,
            )

        fun opphevStansAvSaksbehandler(
            saksbehandler: Saksbehandler,
            begrunnelse: String,
            dialogId: DialogId,
        ): OpphevStansAvSaksbehandler =
            OpphevStansAvSaksbehandler(
                tidspunkt = LocalDateTime.now(),
                saksbehandler = saksbehandler,
                dialogRef = dialogId.value,
                notattekst = begrunnelse,
            )

        fun vedtaksperiodeReberegnet(): VedtaksperiodeReberegnet = VedtaksperiodeReberegnet(tidspunkt = LocalDateTime.now())
    }
}

data class LagtPåVent(
    override val saksbehandler: Saksbehandler,
    override val tidspunkt: LocalDateTime,
    override val dialogRef: Long,
    val årsaker: List<PåVentÅrsak>,
    val notattekst: String?,
    val frist: LocalDate,
) : Historikkinnslag {
    override fun detaljer(): Map<String, Any?> =
        mapOf(
            "årsaker" to årsaker.map { it },
            "frist" to frist,
            "notattekst" to notattekst,
        )
}

data class FjernetFraPåVent(
    override val saksbehandler: Saksbehandler,
    override val tidspunkt: LocalDateTime,
) : Historikkinnslag {
    override val dialogRef: Long? = null
}

data class EndrePåVent(
    override val saksbehandler: Saksbehandler,
    override val tidspunkt: LocalDateTime,
    override val dialogRef: Long,
    val årsaker: List<PåVentÅrsak>,
    val notattekst: String?,
    val frist: LocalDate,
) : Historikkinnslag {
    override fun detaljer(): Map<String, Any?> =
        mapOf(
            "årsaker" to årsaker.map { it },
            "frist" to frist,
            "notattekst" to notattekst,
        )
}

data class TotrinnsvurderingFerdigbehandlet(
    override val saksbehandler: Saksbehandler,
    override val tidspunkt: LocalDateTime,
) : Historikkinnslag {
    override val dialogRef: Long? = null
}

data class AvventerTotrinnsvurdering(
    override val saksbehandler: Saksbehandler,
    override val tidspunkt: LocalDateTime,
) : Historikkinnslag {
    override val dialogRef: Long? = null
}

data class TotrinnsvurderingRetur(
    override val saksbehandler: Saksbehandler,
    override val tidspunkt: LocalDateTime,
    override val dialogRef: Long,
    val notattekst: String,
) : Historikkinnslag {
    override fun detaljer(): Map<String, Any?> =
        mapOf(
            "notattekst" to notattekst,
        )
}

data class TotrinnsvurderingAutomatiskRetur(
    override val tidspunkt: LocalDateTime,
) : Historikkinnslag {
    override val dialogRef: Long? = null
    override val saksbehandler: Saksbehandler? = null
}

data class AutomatiskBehandlingStanset(
    override val tidspunkt: LocalDateTime,
) : Historikkinnslag {
    override val dialogRef: Long? = null
    override val saksbehandler: Saksbehandler? = null
}

data class AutomatiskBehandlingStansetAvSaksbehandler(
    override val tidspunkt: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val dialogRef: Long,
    val notattekst: String,
) : Historikkinnslag {
    override fun detaljer(): Map<String, Any?> =
        mapOf(
            "notattekst" to notattekst,
        )
}

data class OpphevStansAvSaksbehandler(
    override val tidspunkt: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val dialogRef: Long,
    val notattekst: String,
) : Historikkinnslag {
    override fun detaljer(): Map<String, Any?> =
        mapOf(
            "notattekst" to notattekst,
        )
}

data class VedtaksperiodeReberegnet(
    override val tidspunkt: LocalDateTime,
) : Historikkinnslag {
    override val dialogRef: Long? = null
    override val saksbehandler: Saksbehandler? = null
}
