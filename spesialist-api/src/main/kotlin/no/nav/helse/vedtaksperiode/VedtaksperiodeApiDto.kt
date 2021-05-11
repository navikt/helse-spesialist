package no.nav.helse.vedtaksperiode

import no.nav.helse.person.PersoninfoApiDto

data class VedtaksperiodeApiDto(
    val fødselsnummer: String,
    val aktørId: String,
    val personinfo: PersoninfoApiDto,
    val arbeidsgiverRef: Long,
    val speilSnapshotRef: Int,
    val infotrygdutbetalingerRef: Int?
)
