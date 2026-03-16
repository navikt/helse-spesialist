package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.application.logg.teamLogs
import kotlin.reflect.KProperty0

internal class FraVerdiValidering<T, R>(
    felt: KProperty0<ApiPatchEndring<T>?>,
    private val faktiskVerdi: R,
    private val mapping: (T) -> R,
) {
    private val feltverdi = felt.get()
    private val feltnavn = felt.name

    fun valider(): Boolean {
        if (feltverdi != null) {
            val forventetVerdi = mapping(feltverdi.fra)
            if (forventetVerdi != faktiskVerdi) {
                teamLogs.warn(
                    "Feil / utdatert fra-verdi i request for $feltnavn." +
                        " Requesten forventet $feltnavn=$forventetVerdi," +
                        " men ressursen hadde $feltnavn=$faktiskVerdi",
                )
                return false
            }
        }
        return true
    }
}

internal fun <T, R> fraVerdiValidering(
    endringFelt: KProperty0<ApiPatchEndring<T>?>,
    faktiskVerdi: R,
    mapping: (T) -> R,
): FraVerdiValidering<T, R> = FraVerdiValidering(endringFelt, faktiskVerdi, mapping)

internal fun <T> fraVerdiValidering(
    endringFelt: KProperty0<ApiPatchEndring<T>?>,
    faktiskVerdi: T,
): FraVerdiValidering<T, T> = FraVerdiValidering(endringFelt, faktiskVerdi) { it }
