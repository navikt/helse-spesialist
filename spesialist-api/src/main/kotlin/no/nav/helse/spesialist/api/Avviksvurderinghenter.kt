package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering
import java.util.UUID

interface Avviksvurderinghenter {
    fun hentAvviksvurdering(vilkårsgrunnlagId: UUID): Avviksvurdering?
}
