package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.VedtaksperiodeRepository
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring

class TilkommenInntektMutationHandler(
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    // tidligste fom må være skjæringstidspunkt +1 (fiksa se under)
    // perioden må være innenfor vedtaksperiodene (fiksa se under)
    // kan ikke legge inn perioder som overlapper med eksisterende perioder, selvom de er fjernet
) : TilkommenInntektMutationSchema {
    override fun leggTilTilkommenInntekt(
        tilkommenInntektOverstyring: ApiTilkommenInntektOverstyring,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val gyldigePeriodeGrenser =
            vedtaksperiodeRepository.finnVedtaksperioder(tilkommenInntektOverstyring.fodselsnummer).map {
                it.behandlinger.last()
            }.groupBy({ it.skjæringstidspunkt }, { it.tom }).map { (skjæringstidspunkt, listeAvTom) ->
                skjæringstidspunkt.plusDays(1) to listeAvTom.max()
            }
        if (gyldigePeriodeGrenser.none { (fom, tom) ->
                tilkommenInntektOverstyring.fom >= fom && tilkommenInntektOverstyring.tom <= tom
            }
        ) {
            error("Kan ikke legge til tilkommen inntekt som går utenfor et sykefraværstilfelle")
        }
    }
}
