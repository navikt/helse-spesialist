package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate

@GraphQLName("Kjonn")
enum class ApiKjonn {
    Kvinne,
    Mann,
    Ukjent,
}

@GraphQLName("Adressebeskyttelse")
enum class ApiAdressebeskyttelse {
    Ugradert,
    Fortrolig,
    StrengtFortrolig,
    StrengtFortroligUtland,
    Ukjent,
}

@GraphQLName("Personinfo")
data class ApiPersoninfo(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fodselsdato: LocalDate,
    val kjonn: ApiKjonn,
    val adressebeskyttelse: ApiAdressebeskyttelse,
    val fullmakt: Boolean? = null,
    val unntattFraAutomatisering: ApiUnntattFraAutomatiskGodkjenning? = null,
    val automatiskBehandlingStansetAvSaksbehandler: Boolean? = null,
)
