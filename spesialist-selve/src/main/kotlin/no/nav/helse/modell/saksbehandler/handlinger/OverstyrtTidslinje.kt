package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.dto.OverstyrtTidslinjeDto
import no.nav.helse.modell.saksbehandler.handlinger.dto.OverstyrtTidslinjedagDto
import no.nav.helse.spesialist.api.modell.OverstyrtTidslinjeEvent

class OverstyrtTidslinje(
    private val id: UUID = UUID.randomUUID(),
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val dager: List<OverstyrtTidslinjedag>,
    private val begrunnelse: String,
): Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

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
}

class OverstyrtTidslinjedag(
    private val dato: LocalDate,
    private val type: String,
    private val fraType: String,
    private val grad: Int?,
    private val fraGrad: Int?,
    private val subsumsjon: Subsumsjon?,
) {
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
        subsumsjon = subsumsjon?.toDto()
    )
}