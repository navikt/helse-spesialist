package no.nav.helse.kafka.message_builders

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.modell.melding.SubsumsjonEvent

fun SubsumsjonEvent.somJsonMessage(
    fødselsnummer: String,
    versjonAvKode: String,
): JsonMessage =
    JsonMessage.newMessage(
        "subsumsjon",
        mapOf(
            "subsumsjon" to
                listOfNotNull(
                    "id" to id,
                    "versjon" to "1.0.0",
                    "kilde" to kilde,
                    "versjonAvKode" to versjonAvKode,
                    "fodselsnummer" to fødselsnummer,
                    "sporing" to sporing,
                    "tidsstempel" to tidsstempel,
                    "lovverk" to lovverk,
                    "lovverksversjon" to lovverksversjon,
                    "paragraf" to paragraf,
                    "input" to input,
                    "output" to output,
                    "utfall" to utfall,
                    ledd?.let { "ledd" to it },
                    bokstav?.let { "bokstav" to it },
                ).toMap(),
        ),
    )
