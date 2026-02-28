package no.nav.helse.spesialist.valkey

import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.domain.Personinfo
import java.time.Duration

class CacheEnabledPersoninfoHenter(
    private val personinfoHenter: PersoninfoHenter,
    private val cachingProxy: CachingProxy,
) : PersoninfoHenter {
    override fun hentPersoninfo(ident: String): Personinfo? =
        cachingProxy.get<Personinfo>(key = "personinfo:$ident", timeToLive = Duration.ofHours(24)) {
            personinfoHenter.hentPersoninfo(ident)
        }
}
