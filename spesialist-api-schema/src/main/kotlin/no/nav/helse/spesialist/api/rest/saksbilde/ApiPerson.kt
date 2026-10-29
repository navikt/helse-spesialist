@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
    YearMonth::class,
)

package no.nav.helse.spesialist.api.rest.saksbilde

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Serializable
data class ApiAvslag(
    val type: ApiAvslagstype,
    val begrunnelse: String,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
    val invalidert: Boolean,
)

@Serializable
data class ApiVedtakBegrunnelse(
    val utfall: ApiVedtakUtfall,
    val begrunnelse: String?,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
)

@Serializable
data class ApiAnnetFodselsnummer(
    val fodselsnummer: String,
    val personPseudoId: UUID,
)

@Serializable
data class ApiSaksbildePerson(
    val aktorId: String,
    val fodselsnummer: String,
    val andreFodselsnummer: List<ApiAnnetFodselsnummer>,
    val dodsdato: LocalDate?,
    val tilleggsinfoForInntektskilder: List<ApiTilleggsinfoForInntektskilde>,
    val arbeidsgivere: List<ApiArbeidsgiver>,
    val selvstendigNaering: ApiSelvstendigNaering?,
    val vilkarsgrunnlagV2: List<ApiVilkårsgrunnlagV2>,
)
