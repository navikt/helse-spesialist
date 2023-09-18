package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.saksbehandler.OverstyrtTidslinjeEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag.Companion.byggSubsumsjoner
import no.nav.helse.modell.saksbehandler.handlinger.Subsumsjon.Sporing
import no.nav.helse.modell.saksbehandler.handlinger.Subsumsjon.Utfall.VILKAR_BEREGNET
import no.nav.helse.modell.saksbehandler.handlinger.dto.OverstyrtTidslinjeDto
import no.nav.helse.modell.saksbehandler.handlinger.dto.OverstyrtTidslinjedagDto

class OverstyrtTidslinje(
    private val id: UUID = UUID.randomUUID(),
    private val vedtaksperiodeId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val dager: List<OverstyrtTidslinjedag>,
    private val begrunnelse: String,
) : Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "overstyr_tidslinje"

    fun byggEvent() = OverstyrtTidslinjeEvent(
        id = id,
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        organisasjonsnummer = organisasjonsnummer,
        dager = dager.map(OverstyrtTidslinjedag::byggEvent),
    )

    fun toDto() = OverstyrtTidslinjeDto(
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        dager = dager.map(OverstyrtTidslinjedag::toDto),
        begrunnelse = begrunnelse
    )

    internal fun byggSubsumsjoner(oid: UUID): List<Subsumsjon> {
        return dager.byggSubsumsjoner(
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            begrunnelse = begrunnelse,
            oid = oid
        )
    }
}

class OverstyrtTidslinjedag(
    private val dato: LocalDate,
    private val type: String,
    private val fraType: String,
    private val grad: Int?,
    private val fraGrad: Int?,
    private val lovhjemmel: Lovhjemmel?,
) {

    internal companion object {
        internal fun List<OverstyrtTidslinjedag>.byggSubsumsjoner(
            vedtaksperiodeId: UUID,
            fødselsnummer: String,
            organisasjonsnummer: String,
            begrunnelse: String,
            oid: UUID,
        ): List<Subsumsjon> = this
            .mapNotNull { if (it.lovhjemmel != null) it.lovhjemmel to it else null }
            .groupBy({ it.first }, { it.second })
            .map { (lovhjemmel, dager) ->
                Subsumsjon(
                    lovhjemmel = lovhjemmel,
                    fødselsnummer = fødselsnummer,
                    utfall = VILKAR_BEREGNET,
                    input = mapOf(
                        "begrunnelseFraSaksbehandler" to begrunnelse,
                    ),
                    output = mapOf(
                        "dager" to dager.map {
                            mapOf(
                                "dato" to it.dato,
                                "type" to it.type,
                                "fraType" to it.fraType,
                                "grad" to it.grad,
                                "fraGrad" to it.fraGrad,
                            )
                        }
                    ),
                    sporing = Sporing(
                        vedtaksperioder = listOf(vedtaksperiodeId),
                        organisasjonsnummer = listOf(organisasjonsnummer),
                        saksbehandler = oid
                    )
                )
            }
    }

    fun byggEvent() = OverstyrtTidslinjeEvent.OverstyrtTidslinjeEventDag(
        dato = dato,
        type = type,
        fraType = fraType,
        grad = grad,
        fraGrad = fraGrad,
    )

    fun toDto() = OverstyrtTidslinjedagDto(
        dato = dato,
        type = type,
        fraType = fraType,
        grad = grad,
        fraGrad = fraGrad,
        subsumsjon = lovhjemmel?.toDto()
    )
}