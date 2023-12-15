package no.nav.helse.spesialist.api

import java.util.UUID
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering

interface Avviksvurderinghenter {
    fun hentAvviksvurdering(vilkårsgrunnlagId: UUID): Avviksvurdering?
}
