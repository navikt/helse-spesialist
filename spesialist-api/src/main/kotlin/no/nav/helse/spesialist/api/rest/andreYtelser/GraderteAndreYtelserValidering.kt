package no.nav.helse.spesialist.api.rest.andreYtelser

import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelserPeriode
import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelserType
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.andreytelser.validerGraderteAndreYtelserPeriode

internal fun List<ApiGraderteAndreYtelserPeriode>.tilGraderteAndreYtelserPerioder() =
    map {
        GraderteAndreYtelserPeriode(
            periode = Periode(it.fom, it.tom),
            grad = it.grad,
        )
    }

internal fun ApiGraderteAndreYtelserType.tilDomeneType() = GraderteAndreYtelserType.valueOf(name)

internal fun KallKontekst.validerGraderteAndreYtelserEndring(
    person: Person,
    graderteAndreYtelserId: GraderteAndreYtelserId,
    perioder: List<GraderteAndreYtelserPeriode>,
    type: GraderteAndreYtelserType,
) {
    validerGraderteAndreYtelserPeriode(
        eksisterendeGraderteAndreYtelser =
            transaksjon.graderteAndreYtelserRepository
                .finnAlleForIdentitetsnummer(person.id)
                .filterNot { it.id == graderteAndreYtelserId },
        nyGraderteAndreYtelserType = type,
        nyGraderteAndreYtelserPerioder = perioder,
        vedtaksperioder = transaksjon.legacyVedtaksperiodeRepository.finnVedtaksperioder(person.id.value),
    )
}
