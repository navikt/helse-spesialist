package no.nav.helse.spesialist.api.bootstrap

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage

internal fun Route.debugMinneApi() {
    route("/minne") {
        get {
            val heapMemoryUsage: MemoryUsage = ManagementFactory.getMemoryMXBean().heapMemoryUsage
            val nomheapMemoryUsage: MemoryUsage = ManagementFactory.getMemoryMXBean().nonHeapMemoryUsage
            val runtime = Runtime.getRuntime()
            val svar =
                """
                runtime.max:          ${formatSize(runtime.maxMemory())}                
                runtime.commited:     ${formatSize(runtime.totalMemory())}                
                heapMemoryUsage.used: ${formatSize(heapMemoryUsage.used)}
                runtime.total - free: ${formatSize(runtime.totalMemory() - runtime.freeMemory())}
                heapMemoryUsage.init: ${formatSize(heapMemoryUsage.init)}
                
                nomheapMemoryUsage.init:      ${formatSize(nomheapMemoryUsage.init)}
                nomheapMemoryUsage.used:      ${formatSize(nomheapMemoryUsage.used)}
                nomheapMemoryUsage.committed: ${formatSize(nomheapMemoryUsage.committed)}
                nomheapMemoryUsage.max:       ${formatSize(nomheapMemoryUsage.max)}

                """.trimIndent()
            call.respondText(svar, ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}

private fun formatSize(v: Long): String {
    if (v < 1024) return "$v B"
    val z = (63 - java.lang.Long.numberOfLeadingZeros(v)) / 10
    return String.format("%.1f %sB", v.toDouble() / (1L shl (z * 10)), " KMGTPE"[z])
}
