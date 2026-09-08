package no.nav.helse.db

import no.nav.helse.modell.vedtak.BegrunnelseForSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.domain.Identitetsnummer

interface BegrunnelseForSkjønnsfastsettelseRepository {
    fun finnBegrunnelseForSkjønnsfastsattSykepengegrunnlag(identitetsnummer: Identitetsnummer): List<BegrunnelseForSkjønnsfastsattSykepengegrunnlag>
}
