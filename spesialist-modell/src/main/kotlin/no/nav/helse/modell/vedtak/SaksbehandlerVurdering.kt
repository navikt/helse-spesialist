package no.nav.helse.modell.vedtak

import no.nav.helse.modell.vedtak.SaksbehandlerVurderingDto.VurderingDto

sealed class SaksbehandlerVurdering private constructor(
    private val vurdering: Vurdering,
) {
    abstract val begrunnelse: String?

    class Innvilgelse(private var innvilgelsesbegrunnelse: String? = null) : SaksbehandlerVurdering(Vurdering.INNVILGELSE) {
        override val begrunnelse: String? get() = innvilgelsesbegrunnelse
    }

    class Avslag(private var avslagsbegrunnelse: String) : SaksbehandlerVurdering(Vurdering.AVSLAG) {
        override val begrunnelse: String get() = avslagsbegrunnelse
    }

    class DelvisAvslag(private var delvisAvslagsbegrunnelse: String) : SaksbehandlerVurdering(Vurdering.DELVIS_AVSLAG) {
        override val begrunnelse: String get() = delvisAvslagsbegrunnelse
    }

    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.saksbehandlerVurderingData(this)
    }

    fun toDto() =
        when (this) {
            is Avslag -> SaksbehandlerVurderingDto.Avslag(begrunnelse)
            is DelvisAvslag -> SaksbehandlerVurderingDto.DelvisAvslag(begrunnelse)
            is Innvilgelse -> SaksbehandlerVurderingDto.Innvilgelse(begrunnelse)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SaksbehandlerVurdering
        if (vurdering != other.vurdering) return false
        if (begrunnelse != other.begrunnelse) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vurdering.hashCode()
        result = 31 * result + begrunnelse.hashCode()
        return result
    }
}

enum class Vurdering {
    AVSLAG,
    DELVIS_AVSLAG,
    INNVILGELSE,
    ;

    internal fun toDto() =
        when (this) {
            AVSLAG -> VurderingDto.AVSLAG
            DELVIS_AVSLAG -> VurderingDto.DELVIS_AVSLAG
            INNVILGELSE -> VurderingDto.INNVILGELSE
        }
}
