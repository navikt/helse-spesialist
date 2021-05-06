package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.modell.oppgave.PersoninfoDto

data class VedtaksperiodeDto(
    val fødselsnummer: String,
    val aktørId: String,
    val personinfo: PersoninfoDto,
    val arbeidsgiverRef: Long,
    val speilSnapshotRef: Int,
    val infotrygdutbetalingerRef: Int?
)

enum class Periodetype {
    FØRSTEGANGSBEHANDLING,
    FORLENGELSE,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT
}

enum class Inntektskilde {
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE
}
