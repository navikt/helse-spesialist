package no.nav.helse.db

import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import java.time.LocalDate

interface LegacyVedtaksperiodeRepository {
    fun finnVedtaksperioder(fødselsnummer: String): List<VedtaksperiodeDto>

    fun lagreVedtaksperioder(
        fødselsnummer: String,
        vedtaksperioder: List<VedtaksperiodeDto>,
    )

    fun førsteKjenteDag(fødselsnummer: String): LocalDate?
}
