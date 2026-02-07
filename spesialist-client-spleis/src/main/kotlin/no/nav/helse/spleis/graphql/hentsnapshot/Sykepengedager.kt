package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import java.time.LocalDate

@Generated
public data class Sykepengedager(
    @get:JsonProperty(value = "forbrukteSykedager")
    public val forbrukteSykedager: Int? = null,
    @get:JsonProperty(value = "gjenstaendeSykedager")
    public val gjenstaendeSykedager: Int? = null,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "maksdato")
    public val maksdato: LocalDate,
    @get:JsonProperty(value = "oppfylt")
    public val oppfylt: Boolean,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "skjaeringstidspunkt")
    public val skjaeringstidspunkt: LocalDate,
)
