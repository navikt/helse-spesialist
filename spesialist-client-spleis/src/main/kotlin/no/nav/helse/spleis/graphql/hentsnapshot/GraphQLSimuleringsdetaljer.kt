package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import java.time.LocalDate

@Generated
public data class GraphQLSimuleringsdetaljer(
    @get:JsonProperty(value = "belop")
    public val belop: Int,
    @get:JsonProperty(value = "antallSats")
    public val antallSats: Int,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "faktiskFom")
    public val faktiskFom: LocalDate,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "faktiskTom")
    public val faktiskTom: LocalDate,
    @get:JsonProperty(value = "klassekode")
    public val klassekode: String,
    @get:JsonProperty(value = "klassekodeBeskrivelse")
    public val klassekodeBeskrivelse: String,
    @get:JsonProperty(value = "konto")
    public val konto: String,
    @get:JsonProperty(value = "refunderesOrgNr")
    public val refunderesOrgNr: String,
    @get:JsonProperty(value = "sats")
    public val sats: Double,
    @get:JsonProperty(value = "tilbakeforing")
    public val tilbakeforing: Boolean,
    @get:JsonProperty(value = "typeSats")
    public val typeSats: String,
    @get:JsonProperty(value = "uforegrad")
    public val uforegrad: Int,
    @get:JsonProperty(value = "utbetalingstype")
    public val utbetalingstype: String,
)
