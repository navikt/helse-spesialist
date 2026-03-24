package no.nav.helse.spesialist.api.rest.oppgaver

import no.nav.helse.mediator.oppgave.tilUtgåendeHendelse
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.oppgave.Oppgavehendelse

internal fun List<Oppgavehendelse>.leggTilIOutbox(
    identitetsnummer: Identitetsnummer,
    kallKontekst: KallKontekst,
    årsak: String,
) = this
    .forEach { kallKontekst.outbox.leggTil(identitetsnummer, it.tilUtgåendeHendelse(), årsak) }
