package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.modell.vedtak.PersoninfoDto

data class VedtaksperiodeDto(
    val fødselsnummer: String,
    val aktørId: String,
    val personinfo: PersoninfoDto,
    val arbeidsgiverRef: Long,
    val speilSnapshotRef: Int,
    val infotrygdutbetalingerRef: Int?
)
