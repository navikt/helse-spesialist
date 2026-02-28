package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.application.spillkar.`ManuelleInngangsvilkårVurderinger`

fun interface InngangsvilkårInnsender {
    fun sendManuelleVurderinger(vurderinger: ManuelleInngangsvilkårVurderinger)
}
