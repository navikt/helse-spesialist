package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import no.nav.helse.spesialist.api.rest.saksbilde.ApiOverstyring

/**
 * Innpakket fordi Jackson mister type-discriminatoren på elementene når en liste av sealed-typer er rot i svaret.
 */
@Serializable
data class ApiOverstyringerForPerson(
    val overstyringer: List<ApiOverstyring>,
)
