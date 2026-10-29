package no.nav.helse.spesialist.api.rest.vedtaksperioder

import no.nav.helse.spesialist.api.rest.saksbilde.ApiAnnullering
import no.nav.helse.spesialist.domain.Annullering
import no.nav.helse.spesialist.domain.Saksbehandler

internal fun Annullering.tilApiAnnullering(saksbehandler: Saksbehandler): ApiAnnullering =
    ApiAnnullering(
        saksbehandlerIdent = saksbehandler.ident.value,
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        personFagsystemId = personFagsystemId,
        tidspunkt = tidspunkt,
        arsaker = årsaker,
        begrunnelse = kommentar,
        vedtaksperiodeId = vedtaksperiodeId,
    )
