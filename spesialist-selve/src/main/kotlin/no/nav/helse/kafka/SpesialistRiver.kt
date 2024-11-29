package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.River

internal sealed interface SpesialistRiver : River.PacketListener {
    fun validations(): River.PacketValidation
}
