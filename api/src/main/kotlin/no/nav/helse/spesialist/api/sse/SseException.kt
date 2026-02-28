package no.nav.helse.spesialist.api.sse

sealed class SseException(
    message: String,
) : Exception(message) {
    class PersonIkkeFunnet(
        message: String,
    ) : SseException(message)
}
