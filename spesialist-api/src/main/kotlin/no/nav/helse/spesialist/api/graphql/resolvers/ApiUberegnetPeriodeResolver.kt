package no.nav.helse.spesialist.api.graphql.resolvers

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import io.ktor.utils.io.core.toByteArray
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.spesialist.api.graphql.mapping.tilApiDag
import no.nav.helse.spesialist.api.graphql.mapping.tilApiHendelse
import no.nav.helse.spesialist.api.graphql.mapping.tilApiInntektstype
import no.nav.helse.spesialist.api.graphql.mapping.tilApiNotat
import no.nav.helse.spesialist.api.graphql.mapping.tilApiPeriodetilstand
import no.nav.helse.spesialist.api.graphql.mapping.tilApiPeriodetype
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

    override fun erForkastet(): Boolean = periode.erForkastet

    override fun fom(): LocalDate = periode.fom

    override fun tom(): LocalDate = periode.tom

    override fun id(): UUID = UUID.nameUUIDFromBytes(vedtaksperiodeId().toString().toByteArray() + index.toByte())

    override fun inntektstype(): ApiInntektstype = periode.inntektstype.tilApiInntektstype()

    override fun opprettet(): LocalDateTime = periode.opprettet

    override fun periodetype(): ApiPeriodetype = periode.tilApiPeriodetype()

    override fun tidslinje(): List<ApiDag> = periode.tidslinje.map { it.tilApiDag() }

    override fun vedtaksperiodeId(): UUID = periode.vedtaksperiodeId

    override fun periodetilstand(): ApiPeriodetilstand = periode.periodetilstand.tilApiPeriodetilstand(true)

    override fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt

    override fun hendelser(): List<ApiHendelse> = periode.hendelser.map { it.tilApiHendelse() }

    override fun varsler(): List<ApiVarselDTO> =
        if (skalViseAktiveVarsler) {
            varselRepository.finnVarslerForUberegnetPeriode(vedtaksperiodeId()).map { it.toVarselDto() }
        } else {
            varselRepository.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId()).map { it.toVarselDto() }
        }

    override fun notater(): List<ApiNotat> = notatDao.finnNotater(vedtaksperiodeId()).map { it.tilApiNotat() }
}
