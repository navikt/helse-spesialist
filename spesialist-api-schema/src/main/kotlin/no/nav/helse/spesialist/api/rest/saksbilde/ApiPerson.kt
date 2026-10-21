@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
    YearMonth::class,
)

package no.nav.helse.spesialist.api.rest.saksbilde

import com.expediagroup.graphql.generator.annotations.GraphQLName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@GraphQLName("Saksbehandler")
@Serializable
data class ApiSaksbehandler(
    val navn: String,
    val ident: String?,
)

@GraphQLName("Tildeling")
@Serializable
data class ApiSaksbildeTildeling(
    val navn: String,
    val epost: String,
    val oid: UUID,
)

@GraphQLName("PaVent")
@Serializable
data class ApiPaVent(
    val frist: LocalDate?,
    val oid: UUID,
)

@GraphQLName("Avslag")
@Serializable
data class ApiAvslag(
    val type: ApiAvslagstype,
    val begrunnelse: String,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
    val invalidert: Boolean,
)

@GraphQLName("VedtakBegrunnelse")
@Serializable
data class ApiVedtakBegrunnelse(
    val utfall: ApiVedtakUtfall,
    val begrunnelse: String?,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
)

@GraphQLName("Annullering")
@Serializable
data class ApiAnnullering(
    val saksbehandlerIdent: String,
    val arbeidsgiverFagsystemId: String?,
    val personFagsystemId: String?,
    val tidspunkt: LocalDateTime,
    val arsaker: List<String>,
    val begrunnelse: String?,
    val vedtaksperiodeId: UUID,
)

@GraphQLName("AnnetFodselsnummer")
@Serializable
data class ApiAnnetFodselsnummer(
    val fodselsnummer: String,
    val personPseudoId: UUID,
)

@GraphQLName("Person")
@Serializable
data class ApiSaksbildePerson(
    val aktorId: String,
    val fodselsnummer: String,
    val andreFodselsnummer: List<ApiAnnetFodselsnummer>,
    val dodsdato: LocalDate?,
    val tildeling: ApiSaksbildeTildeling?,
    val tilleggsinfoForInntektskilder: List<ApiTilleggsinfoForInntektskilde>,
    val arbeidsgivere: List<ApiArbeidsgiver>,
    val selvstendigNaering: ApiSelvstendigNaering?,
    val vilkarsgrunnlagV2: List<ApiVilkårsgrunnlagV2>,
)
