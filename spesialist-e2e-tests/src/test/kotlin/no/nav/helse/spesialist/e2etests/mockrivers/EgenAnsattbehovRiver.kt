package no.nav.helse.spesialist.e2etests.mockrivers

class EgenAnsattbehovRiver : AbstractBehovRiver("EgenAnsatt") {
    override fun løsning() =
        mapOf("EgenAnsatt" to false)
}
