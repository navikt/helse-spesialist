package no.nav.helse.spesialist.domain.andreytelser

object GraderteAndreYtelserValidator {
    fun validerDager(dager: List<GraderteAndreYtelserDag>): Resultat {
        if (dager.isEmpty()) {
            return Resultat.ManglerEndringer
        }
        if (dager.any { dag -> dag.sykdomsgrad < 0 || dag.arbeidsgrad < 0 || dag.andreYtelserGrader.values.any { it < 0 } }) {
            return Resultat.NegativGrad
        }
        if (dager.any { dag -> dag.andreYtelserGrader.values.sum() + dag.sykdomsgrad + dag.arbeidsgrad > 100 }) {
            return Resultat.TotalGradOver100
        }
        return Resultat.OK
    }

    sealed interface Resultat {
        object OK : Resultat

        object ManglerEndringer : Resultat

        object NegativGrad : Resultat

        object TotalGradOver100 : Resultat
    }
}
