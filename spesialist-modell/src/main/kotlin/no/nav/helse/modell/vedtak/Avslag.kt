package no.nav.helse.modell.vedtak

class Avslag(
    private val type: Avslagstype,
    private val begrunnelse: String,
) {
    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.avslagData(type, begrunnelse)
    }

    fun toDto() =
        AvslagDto(
            type = type.toDto(),
            begrunnelse = begrunnelse,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Avslag

        if (type != other.type) return false
        if (begrunnelse != other.begrunnelse) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + begrunnelse.hashCode()
        return result
    }
}

enum class Avslagstype {
    AVSLAG,
    DELVIS_AVSLAG,
    ;

    internal fun toDto() =
        when (this) {
            AVSLAG -> AvslagstypeDto.AVSLAG
            DELVIS_AVSLAG -> AvslagstypeDto.DELVIS_AVSLAG
        }
}
