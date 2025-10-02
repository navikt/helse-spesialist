package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentInfo
import no.nav.helse.spesialist.api.graphql.schema.ApiPersonnavn
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling
import java.time.Instant

data class ApiOppgaveProjeksjon(
    val id: String,
    val aktorId: String,
    val navn: ApiPersonnavn,
    val egenskaper: List<ApiEgenskap>,
    val tildeling: ApiTildeling?,
    val opprettetTidspunkt: Instant,
    val opprinneligSoeknadstidspunkt: Instant,
    val paVentInfo: ApiPaVentInfo?,
)

data class ApiOppgaveProjeksjonSide(
    val totaltAntall: Long,
    val sidetall: Int,
    val sidestoerrelse: Int,
    val elementer: List<ApiOppgaveProjeksjon>,
) {
    val totaltAntallSider: Long
        get() = (totaltAntall + (sidestoerrelse - 1)) / sidestoerrelse
}
