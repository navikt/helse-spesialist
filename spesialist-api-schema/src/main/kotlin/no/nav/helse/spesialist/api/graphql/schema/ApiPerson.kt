@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLName("Infotrygdutbetaling")
data class ApiInfotrygdutbetaling(
    val fom: String,
    val tom: String,
    val grad: String,
    val dagsats: Double,
    val typetekst: String,
    val organisasjonsnummer: String,
)

@GraphQLName("Saksbehandler")
data class ApiSaksbehandler(
    val navn: String,
    val ident: String?,
)

@Serializable
@GraphQLName("AktivSaksbehandler")
data class ApiAktivSaksbehandler(
    val navn: String,
    val ident: String,
    val oid: UUID,
)

@GraphQLName("UnntattFraAutomatiskGodkjenning")
data class ApiUnntattFraAutomatiskGodkjenning(
    val erUnntatt: Boolean,
    val arsaker: List<String>,
    val tidspunkt: LocalDateTime?,
)

@GraphQLName("Enhet")
data class ApiEnhet(
    val id: String,
    val navn: String,
)

@Serializable
@GraphQLName("Tildeling")
data class ApiTildeling(
    val navn: String,
    val epost: String,
    val oid: UUID,
)

@GraphQLName("PaVent")
data class ApiPaVent(
    val frist: LocalDate?,
    val oid: UUID,
)

@GraphQLName("Avslag")
data class ApiAvslag(
    val type: ApiAvslagstype,
    val begrunnelse: String,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
    val invalidert: Boolean,
)

@GraphQLName("VedtakBegrunnelse")
data class ApiVedtakBegrunnelse(
    val utfall: ApiVedtakUtfall,
    val begrunnelse: String?,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
)

@GraphQLName("Annullering")
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
data class ApiAnnetFodselsnummer(
    val fodselsnummer: String,
    val personPseudoId: UUID,
)

@GraphQLIgnore
interface PersonSchema {
    fun versjon(): Int

    fun aktorId(): String

    fun fodselsnummer(): String

    fun andreFodselsnummer(): List<ApiAnnetFodselsnummer>

    fun dodsdato(): LocalDate?

    fun personinfo(): ApiPersoninfo

    fun enhet(): ApiEnhet

    fun tildeling(): ApiTildeling?

    fun tilleggsinfoForInntektskilder(): List<ApiTilleggsinfoForInntektskilde>

    fun arbeidsgivere(): List<ApiArbeidsgiver>

    fun selvstendigNaering(): ApiSelvstendigNaering?

    fun infotrygdutbetalinger(): List<ApiInfotrygdutbetaling>?

    fun vilkarsgrunnlagV2(): List<ApiVilkårsgrunnlagV2>
}

@GraphQLName("Person")
class ApiPerson(
    private val resolver: PersonSchema,
) : PersonSchema by resolver
