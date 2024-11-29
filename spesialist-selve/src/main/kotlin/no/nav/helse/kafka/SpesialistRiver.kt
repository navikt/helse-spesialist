package no.nav.helse.kafka

import no.nav.helse.rapids_rivers.River

internal sealed interface SpesialistRiver : River.PacketListener {
    fun validations(): River.PacketValidation
}
