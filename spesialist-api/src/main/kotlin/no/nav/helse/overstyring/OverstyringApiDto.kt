package no.nav.helse.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

abstract class OverstyringApiDto {
    abstract var hendelseId: UUID
    abstract var begrunnelse: String
    abstract var timestamp: LocalDateTime
    abstract var saksbehandlerNavn: String
    abstract var saksbehandlerIdent: String?
    abstract var type: OverstyringType
}

data class OverstyringApiDagerDto(
    override var hendelseId: UUID,
    override var begrunnelse: String,
    override var timestamp: LocalDateTime,
    override var saksbehandlerNavn: String,
    override var saksbehandlerIdent: String?,
    val overstyrteDager: List<OverstyrtDagApiDto>,
): OverstyringApiDto() {
    override var type: OverstyringType = OverstyringType.Dager
}

data class OverstyringApiInntektDto(
    override var hendelseId: UUID,
    override var begrunnelse: String,
    override var timestamp: LocalDateTime,
    override var saksbehandlerNavn: String,
    override var saksbehandlerIdent: String?,
    val overstyrtInntekt: OverstyrtInntektApiDto,
): OverstyringApiDto() {
    override var type: OverstyringType = OverstyringType.Inntekt
}

data class OverstyringApiArbeidsforholdDto(
    override var hendelseId: UUID,
    override var begrunnelse: String,
    override var timestamp: LocalDateTime,
    override var saksbehandlerNavn: String,
    override var saksbehandlerIdent: String?,
    val overstyrtArbeidsforhold: OverstyrtArbeidsforholdApiDto,
): OverstyringApiDto() {
    override var type: OverstyringType = OverstyringType.Arbeidsforhold
}

data class OverstyrtDagApiDto(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?
)

data class OverstyrtInntektApiDto(
    val forklaring: String,
    val månedligInntekt: Double,
    val skjæringstidspunkt: LocalDate
)

data class OverstyrtArbeidsforholdApiDto(
    val forklaring: String,
    val deaktivert: Boolean,
    val skjæringstidspunkt: LocalDate
)

enum class OverstyringType {
    Dager, Inntekt, Arbeidsforhold
}
