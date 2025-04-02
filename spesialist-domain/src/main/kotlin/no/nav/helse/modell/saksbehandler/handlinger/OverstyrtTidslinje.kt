package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.OverstyrtTidslinjeEvent
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag.Companion.byggSubsumsjoner
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingOverstyrtTidslinje
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_BEREGNET
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OverstyrtTidslinje private constructor(
    id: OverstyringId?,
    override val eksternHendelseId: UUID,
    override val saksbehandlerOid: SaksbehandlerOid,
    override val fødselsnummer: String,
    override val aktørId: String,
    override val vedtaksperiodeId: UUID,
    override val opprettet: LocalDateTime,
    ferdigstilt: Boolean,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjedag>,
    val begrunnelse: String,
) : Overstyring(id, ferdigstilt) {
    override fun utførAv(legacySaksbehandler: LegacySaksbehandler) {
        legacySaksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_tidslinje"

    companion object {
        fun ny(
            saksbehandlerOid: SaksbehandlerOid,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            organisasjonsnummer: String,
            dager: List<OverstyrtTidslinjedag>,
            begrunnelse: String,
        ) = OverstyrtTidslinje(
            id = null,
            eksternHendelseId = UUID.randomUUID(),
            opprettet = LocalDateTime.now(),
            ferdigstilt = false,
            saksbehandlerOid = saksbehandlerOid,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            dager = dager,
            begrunnelse = begrunnelse,
        )

        fun fraLagring(
            id: OverstyringId,
            eksternHendelseId: UUID,
            opprettet: LocalDateTime,
            ferdigstilt: Boolean,
            saksbehandlerOid: SaksbehandlerOid,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            organisasjonsnummer: String,
            dager: List<OverstyrtTidslinjedag>,
            begrunnelse: String,
        ) = OverstyrtTidslinje(
            id = id,
            eksternHendelseId = eksternHendelseId,
            opprettet = opprettet,
            ferdigstilt = ferdigstilt,
            saksbehandlerOid = saksbehandlerOid,
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

data class OverstyrtTidslinjedag(
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
