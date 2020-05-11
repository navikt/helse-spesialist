package no.nav.helse.modell.person

import no.nav.helse.modell.vedtak.NavnDto

data class PersonDto(val fødselsnummer: String, val navn: NavnDto)
