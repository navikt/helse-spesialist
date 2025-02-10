package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.OverstyrtTidslinjeEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag.Companion.byggSubsumsjoner
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingOverstyrtTidslinje
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_BEREGNET
import java.time.LocalDate
import java.util.UUID

class OverstyrtTidslinje private constructor(
    id: Long?,
    override val eksternHendelseId: UUID,
    override val saksbehandler: Saksbehandler,
    override val fødselsnummer: String,
    override val aktørId: String,
    override val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjedag>,
    val begrunnelse: String,
) : Overstyring(id) {
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_tidslinje"

    companion object {
        fun ny(
            saksbehandler: Saksbehandler,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            organisasjonsnummer: String,
            dager: List<OverstyrtTidslinjedag>,
            begrunnelse: String,
        ) = OverstyrtTidslinje(
            id = null,
            eksternHendelseId = UUID.randomUUID(),
            saksbehandler = saksbehandler,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            dager = dager,
            begrunnelse = begrunnelse,
        )

        fun fraLagring(
            id: Long,
            eksternHendelseId: UUID,
            saksbehandler: Saksbehandler,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            organisasjonsnummer: String,
            dager: List<OverstyrtTidslinjedag>,
            begrunnelse: String,
        ) = OverstyrtTidslinje(
            id = id,
            eksternHendelseId = eksternHendelseId,
            saksbehandler = saksbehandler,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            dager = dager,
            begrunnelse = begrunnelse,
        )
    }

    fun byggEvent() =
        OverstyrtTidslinjeEvent(
            eksternHendelseId = eksternHendelseId,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            dager = dager.map(OverstyrtTidslinjedag::byggEvent),
        )

    internal fun byggSubsumsjoner(saksbehandlerEpost: String): List<Subsumsjon> {
        return dager.byggSubsumsjoner(
            overstyringId = eksternHendelseId,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            begrunnelse = begrunnelse,
            saksbehandlerEpost = saksbehandlerEpost,
        )
    }
}

class OverstyrtTidslinjedag(
    val dato: LocalDate,
    val type: String,
    val fraType: String,
    val grad: Int?,
    val fraGrad: Int?,
    val lovhjemmel: Lovhjemmel?,
) {
    internal companion object {
        internal fun List<OverstyrtTidslinjedag>.byggSubsumsjoner(
            overstyringId: UUID,
            vedtaksperiodeId: UUID,
            fødselsnummer: String,
            organisasjonsnummer: String,
            begrunnelse: String,
            saksbehandlerEpost: String,
        ): List<Subsumsjon> =
            this
                .mapNotNull { if (it.lovhjemmel != null) it.lovhjemmel to it else null }
                .groupBy({ it.first }, { it.second })
                .map { (lovhjemmel, dager) ->
                    Subsumsjon(
                        lovhjemmel = lovhjemmel,
                        fødselsnummer = fødselsnummer,
                        utfall = VILKAR_BEREGNET,
                        input =
                            mapOf(
                                "begrunnelseFraSaksbehandler" to begrunnelse,
                            ),
                        output =
                            mapOf(
                                "dager" to
                                    dager.map {
                                        mapOf(
                                            "dato" to it.dato,
                                            "type" to it.type,
                                            "fraType" to it.fraType,
                                            "grad" to it.grad,
                                            "fraGrad" to it.fraGrad,
                                        )
                                    },
                            ),
                        sporing =
                            SporingOverstyrtTidslinje(
                                vedtaksperioder = listOf(vedtaksperiodeId),
                                organisasjonsnummer = listOf(organisasjonsnummer),
                                saksbehandler = listOf(saksbehandlerEpost),
                                overstyrtTidslinjeId = overstyringId,
                            ),
                    )
                }
    }

    fun byggEvent() =
        OverstyrtTidslinjeEvent.OverstyrtTidslinjeEventDag(
            dato = dato,
            type = type,
            fraType = fraType,
            grad = grad,
            fraGrad = fraGrad,
        )
}
