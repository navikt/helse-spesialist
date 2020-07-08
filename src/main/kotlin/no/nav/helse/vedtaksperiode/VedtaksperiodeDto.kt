package no.nav.helse.vedtaksperiode

import no.nav.helse.modell.vedtak.PersoninfoDto

data class VedtaksperiodeDto(
    val fødselsnummer: String,
    val aktørId: String,
    val personinfo: PersoninfoDto,
    val arbeidsgiverRef: Int,
    val speilSnapshotRef: Int,
    val infotrygdutbetalingerRef: Int?
)
