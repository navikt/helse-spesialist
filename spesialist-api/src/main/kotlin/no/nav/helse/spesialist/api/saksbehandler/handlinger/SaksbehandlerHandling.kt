package no.nav.helse.spesialist.api.saksbehandler.handlinger

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtTidslinje
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtTidslinjedag

internal sealed interface SaksbehandlerHandling {
    fun loggnavn(): String
    fun utførAv(saksbehandler: Saksbehandler)
}

internal sealed interface OverstyringHandling: SaksbehandlerHandling {
    fun gjelderFødselsnummer(): String
}

@JsonIgnoreProperties
class OverstyrTidslinjeHandling(
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyrDagDto>
): OverstyringHandling {

    private val overstyrtTidslinje get() = OverstyrtTidslinje(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        dager = dager.map { OverstyrtTidslinjedag(it.dato, it.type, it.fraType, it.grad, it.fraGrad) },
        begrunnelse = begrunnelse
    )

    override fun loggnavn(): String = "overstyr_tidslinje"

    override fun gjelderFødselsnummer() = fødselsnummer

    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(overstyrtTidslinje)
    }

    @JsonIgnoreProperties
    class OverstyrDagDto(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?
    )
}