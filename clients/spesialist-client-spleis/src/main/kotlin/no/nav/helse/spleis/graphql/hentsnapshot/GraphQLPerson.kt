package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import java.time.LocalDate

@Generated
public data class GraphQLPerson(
    @get:JsonProperty(value = "aktorId")
    public val aktorId: String,
    @get:JsonProperty(value = "arbeidsgivere")
    public val arbeidsgivere: List<GraphQLArbeidsgiver>,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "dodsdato")
    public val dodsdato: LocalDate? = null,
    @get:JsonProperty(value = "fodselsnummer")
    public val fodselsnummer: String,
    @get:JsonProperty(value = "versjon")
    public val versjon: Int,
    @get:JsonProperty(value = "vilkarsgrunnlag")
    public val vilkarsgrunnlag: List<GraphQLVilkarsgrunnlag>,
)
