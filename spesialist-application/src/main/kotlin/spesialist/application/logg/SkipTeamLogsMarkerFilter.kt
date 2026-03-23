package no.nav.helse.spesialist.application.logg

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker
import org.slf4j.MarkerFactory

val SKIP_TEAM_LOGS_MARKER: Marker = MarkerFactory.getMarker("SKIP_TEAM_LOGS_MARKER")

class SkipTeamLogsMarkerFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent) =
        if (event.markerList?.contains(SKIP_TEAM_LOGS_MARKER) == true) {
            FilterReply.DENY
        } else {
            FilterReply.ACCEPT
        }
}
