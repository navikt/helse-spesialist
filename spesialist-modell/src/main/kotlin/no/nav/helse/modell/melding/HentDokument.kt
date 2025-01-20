package no.nav.helse.modell.melding

import java.util.UUID

data class HentDokument(val dokumentId: UUID, val dokumentType: String) : UtgåendeHendelse
