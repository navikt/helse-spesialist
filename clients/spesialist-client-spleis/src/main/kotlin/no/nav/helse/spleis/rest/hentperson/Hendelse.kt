package no.nav.helse.spleis.rest.hentperson

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime

internal enum class Hendelsetype {
    Inntektsmelding,
    InntektFraAOrdningen,
    SendtSoknadNav,
    SendtSoknadArbeidsgiver,
    SendtSoknadFrilans,
    SendtSoknadSelvstendig,
    SendtSoknadArbeidsledig,
    NySoknad,
    Ukjent,
}

/**
 * Hendelser har allerede et [type]-felt som entydig identifiserer subtypen, så det brukes som
 * diskriminator i stedet for et eget felt (speiler `no.nav.helse.spleis.rest.dto.Hendelse` i
 * sykepenger-api-rest).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = Inntektsmelding::class, name = "Inntektsmelding"),
    JsonSubTypes.Type(value = InntektFraAOrdningen::class, name = "InntektFraAOrdningen"),
    JsonSubTypes.Type(value = SoknadNav::class, name = "SendtSoknadNav"),
    JsonSubTypes.Type(value = SoknadArbeidsgiver::class, name = "SendtSoknadArbeidsgiver"),
    JsonSubTypes.Type(value = SoknadFrilans::class, name = "SendtSoknadFrilans"),
    JsonSubTypes.Type(value = SoknadSelvstendig::class, name = "SendtSoknadSelvstendig"),
    JsonSubTypes.Type(value = SoknadArbeidsledig::class, name = "SendtSoknadArbeidsledig"),
    JsonSubTypes.Type(value = Sykmelding::class, name = "NySoknad"),
)
internal sealed interface Hendelse {
    val id: String
    val eksternDokumentId: String
    val type: Hendelsetype
}

internal data class Inntektsmelding(
    override val id: String,
    override val eksternDokumentId: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double,
    override val type: Hendelsetype = Hendelsetype.Inntektsmelding,
) : Hendelse

internal data class InntektFraAOrdningen(
    override val id: String,
    override val eksternDokumentId: String,
    val mottattDato: LocalDateTime,
    override val type: Hendelsetype = Hendelsetype.InntektFraAOrdningen,
) : Hendelse

internal data class SoknadNav(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    override val type: Hendelsetype = Hendelsetype.SendtSoknadNav,
) : Hendelse

internal data class SoknadFrilans(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    override val type: Hendelsetype = Hendelsetype.SendtSoknadFrilans,
) : Hendelse

internal data class SoknadSelvstendig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    override val type: Hendelsetype = Hendelsetype.SendtSoknadSelvstendig,
) : Hendelse

internal data class SoknadArbeidsledig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    override val type: Hendelsetype = Hendelsetype.SendtSoknadArbeidsledig,
) : Hendelse

internal data class SoknadArbeidsgiver(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime,
    override val type: Hendelsetype = Hendelsetype.SendtSoknadArbeidsgiver,
) : Hendelse

internal data class Sykmelding(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    override val type: Hendelsetype = Hendelsetype.NySoknad,
) : Hendelse
