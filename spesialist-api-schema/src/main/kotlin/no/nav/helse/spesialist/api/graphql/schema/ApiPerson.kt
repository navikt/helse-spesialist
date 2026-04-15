package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLName("Saksbehandler")
data class ApiSaksbehandler(
    val navn: String,
    val ident: String?,
)

@GraphQLName("Enhet")
data class ApiEnhet(
    val id: String,
)

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

@GraphQLName("Person")
data class ApiPerson(
    val versjon: Int,
    val aktorId: String,
    val fodselsnummer: String,
    val andreFodselsnummer: List<ApiAnnetFodselsnummer>,
    val dodsdato: LocalDate?,
    val personinfo: ApiPersoninfo,
    val enhet: ApiEnhet,
    val tildeling: ApiTildeling?,
    val tilleggsinfoForInntektskilder: List<ApiTilleggsinfoForInntektskilde>,
    val arbeidsgivere: List<ApiArbeidsgiver>,
    val selvstendigNaering: ApiSelvstendigNaering?,
    val vilkarsgrunnlagV2: List<ApiVilkårsgrunnlagV2>,
)
