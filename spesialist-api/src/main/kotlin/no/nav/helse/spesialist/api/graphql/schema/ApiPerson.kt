package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spesialist.api.graphql.mutation.Avslagstype
import no.nav.helse.spesialist.api.graphql.mutation.VedtakUtfall
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Infotrygdutbetaling(
    val fom: String,
    val tom: String,
    val grad: String,
    val dagsats: Double,
    val typetekst: String,
    val organisasjonsnummer: String,
)

data class Saksbehandler(
    val navn: String,
    val ident: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Reservasjon(
    val kanVarsles: Boolean,
    val reservert: Boolean,
)

data class UnntattFraAutomatiskGodkjenning(
    val erUnntatt: Boolean,
    val arsaker: List<String>,
    val tidspunkt: LocalDateTime?,
)

data class Enhet(
    val id: String,
    val navn: String,
)

data class Tildeling(
    val navn: String,
    val epost: String,
    val oid: UUID,
)

data class PaVent(
    val frist: LocalDate?,
    val oid: UUID,
)

data class Avslag(
    val type: Avslagstype,
    val begrunnelse: String,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
    val invalidert: Boolean,
)

data class VedtakBegrunnelse(
    val utfall: VedtakUtfall,
    val begrunnelse: String?,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
)

data class Annullering(
    val saksbehandlerIdent: String,
    val arbeidsgiverFagsystemId: String?,
    val personFagsystemId: String?,
    val tidspunkt: LocalDateTime,
    val arsaker: List<String>,
    val begrunnelse: String?,
)

@GraphQLIgnore
interface PersonSchema {
    fun versjon(): Int

    fun aktorId(): String

    fun fodselsnummer(): String

    fun dodsdato(): LocalDate?

    fun personinfo(): ApiPersoninfo

    fun enhet(): Enhet

    fun tildeling(): Tildeling?

    fun tilleggsinfoForInntektskilder(): List<ApiTilleggsinfoForInntektskilde>

    fun arbeidsgivere(): List<ApiArbeidsgiver>

    fun infotrygdutbetalinger(): List<Infotrygdutbetaling>?

    fun vilkarsgrunnlag(): List<ApiVilkårsgrunnlag>
}

@GraphQLName("Person")
class ApiPerson(private val resolver: PersonSchema) : PersonSchema by resolver
