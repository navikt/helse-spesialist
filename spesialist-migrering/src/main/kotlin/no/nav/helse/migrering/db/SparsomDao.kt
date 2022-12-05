package no.nav.helse.migrering.db

import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.migrering.domene.Varsel
import org.intellij.lang.annotations.Language

internal class SparsomDao(private val dataSource: DataSource) {

    internal fun finnVarslerFor(fødselsnummer: String): List<Varsel> {
        @Language("PostgreSQL")
        val query = """
            with aktiviteter as materialized (
    select a.id
    from aktivitet_kontekst ak
             inner join kontekst_verdi kv on ak.kontekst_verdi_id = kv.id
             inner join kontekst_navn k on ak.kontekst_navn_id = k.id
             inner join aktivitet a on a.id = ak.aktivitet_id
    where kv.verdi = ? and k.navn = 'fødselsnummer' and level='VARSEL'::level
)
select a.id, a.aktivitet_uuid, p.ident, a.tidsstempel, a.level, m.tekst, v.verdi from aktivitet a
           inner join personident p on p.id = a.personident_id
           inner join melding m on m.id = a.melding_id
           inner join aktivitet_kontekst ak2 on a.id = ak2.aktivitet_id
           inner join kontekst_navn kn on ak2.kontekst_navn_id = kn.id
          inner join kontekst_verdi v on ak2.kontekst_verdi_id = v.id
where a.id in (select id from aktiviteter) and kn.navn = 'vedtaksperiodeId';
        """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer).map {
                Varsel(
                    UUID.fromString(it.string("vedtaksperiodeId")),
                    it.string("tekst"),
                    it.localDateTime("tidsstempel"),
                    it.uuid("aktivitet_uuid"),
                )
            }.asList)
        }
    }
}