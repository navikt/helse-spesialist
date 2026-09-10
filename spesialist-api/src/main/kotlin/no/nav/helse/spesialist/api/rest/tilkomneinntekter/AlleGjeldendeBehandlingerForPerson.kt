package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer

internal fun KallKontekst.alleGjeldendeBehandlingerForPerson(
    identitetsnummer: Identitetsnummer,
): List<Behandling> =
    transaksjon.vedtaksperiodeRepository
        .finnAlleForPerson(identitetsnummer)
        .filterNot { it.forkastet }
        .map {
            transaksjon.behandlingRepository.finnNyesteForVedtaksperiode(it.id)
                ?: error("Fant ikke vedtaksperiode")
        }
