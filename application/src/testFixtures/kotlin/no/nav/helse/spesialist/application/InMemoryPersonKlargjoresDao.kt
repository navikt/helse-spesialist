package no.nav.helse.spesialist.application

class InMemoryPersonKlargjoresDao: PersonKlargjoresDao {
    val fødselsnummerSomKlargjøres = mutableSetOf<String>()

    override fun personKlargjøres(fødselsnummer: String) {
        fødselsnummerSomKlargjøres.add(fødselsnummer)
    }

    override fun klargjøringPågår(fødselsnummer: String): Boolean =
        fødselsnummer in fødselsnummerSomKlargjøres

    override fun personKlargjort(fødselsnummer: String) {
        fødselsnummerSomKlargjøres.remove(fødselsnummer)
    }
}
