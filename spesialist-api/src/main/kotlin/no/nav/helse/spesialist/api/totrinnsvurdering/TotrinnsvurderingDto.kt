package no.nav.helse.spesialist.api.totrinnsvurdering

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties
class TotrinnsvurderingDto(
    val oppgavereferanse: Long,
)