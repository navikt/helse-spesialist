package no.nav.helse.spesialist.api.graphql.resolvers

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import io.ktor.utils.io.core.toByteArray
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.spesialist.api.graphql.mapping.tilApiHendelse
import no.nav.helse.spesialist.api.graphql.mapping.toVarselDto
import no.nav.helse.spesialist.api.graphql.schema.ApiDag
import no.nav.helse.spesialist.api.graphql.schema.ApiHendelse
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektstype
import no.nav.helse.spesialist.api.graphql.schema.ApiNotat
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetilstand
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselDTO
import no.nav.helse.spesialist.api.graphql.schema.UberegnetPeriodeSchema
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLTidslinjeperiode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLIgnore
data class ApiUberegnetPeriodeResolver(
    private val varselRepository: VarselApiRepository,
    private val periode: GraphQLTidslinjeperiode,
    private val skalViseAktiveVarsler: Boolean,
    private val notatDao: NotatApiDao,
    private val index: Int,
) : UberegnetPeriodeSchema {
    override fun behandlingId(): UUID = periode.behandlingId

    override fun erForkastet(): Boolean = erForkastet(periode)

    override fun fom(): LocalDate = fom(periode)

    override fun tom(): LocalDate = tom(periode)

    override fun id(): UUID = UUID.nameUUIDFromBytes(vedtaksperiodeId().toString().toByteArray() + index.toByte())

    override fun inntektstype(): ApiInntektstype = inntektstype(periode)

    override fun opprettet(): LocalDateTime = opprettet(periode)

    override fun periodetype(): ApiPeriodetype = periodetype(periode)

    override fun tidslinje(): List<ApiDag> = tidslinje(periode)

    override fun vedtaksperiodeId(): UUID = periode.vedtaksperiodeId

    override fun periodetilstand(): ApiPeriodetilstand = periodetilstand(periode.periodetilstand, true)

    override fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt

    override fun hendelser(): List<ApiHendelse> = periode.hendelser.map { it.tilApiHendelse() }

    override fun varsler(): List<ApiVarselDTO> =
        if (skalViseAktiveVarsler) {
            varselRepository.finnVarslerForUberegnetPeriode(vedtaksperiodeId()).map { it.toVarselDto() }
        } else {
            varselRepository.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId()).map { it.toVarselDto() }
        }

    override fun notater(): List<ApiNotat> = notater(notatDao, vedtaksperiodeId())
}
