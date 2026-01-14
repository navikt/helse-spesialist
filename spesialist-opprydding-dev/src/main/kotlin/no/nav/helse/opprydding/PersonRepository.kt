package no.nav.helse.opprydding

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal class PersonRepository(
    private val dataSource: DataSource,
) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun slett(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            session.transaction {
                val personId =
                    it.finnPerson(fødselsnummer) ?: run {
                        sikkerlogg.info("Fant ikke person med fødselsnummer $fødselsnummer, avbryter sletting")
                        return@transaction
                    }
                it.slettPersonKlargjøres(fødselsnummer)
                it.slettPersonpseudoid(fødselsnummer)
                it.slettOverstyring(personId)
                it.slettAvslag(personId)
                it.slettReserverPerson(personId)
                it.slettOpptegnelse(personId)
                it.slettPåVent(personId)
                it.slettPeriodehistorikk(personId)
                it.slettTotrinnsvurdering(personId)
                it.slettUtbetaling(personId)
                it.slettKommentarer(personId)
                it.slettNotat(personId)
                it.slettVedtak(personId, fødselsnummer)
                it.slettArbeidsforhold(personId)
                it.slettSnapshot(personId)
                it.slettGosysoppgaver(personId)
                it.slettEgenAnsatt(personId)
                it.slettVergemål(personId)
                it.slettHendelse(fødselsnummer)
                it.slettInntekt(personId)
                it.slettDokument(personId)
                it.slettAvviksvurdering(fødselsnummer)
                it.slettTilkommenInntekt(fødselsnummer)
                it.slettPerson(personId)

                sikkerlogg.info("Person med fødselsnummer $fødselsnummer ble slettet")
            }
        }
    }

    private fun TransactionalSession.finnPerson(fødselsnummer: String): Int? {
        @Language("PostgreSQL")
        val query = "SELECT id FROM person WHERE fødselsnummer = ?"
        return run(queryOf(query, fødselsnummer).map { it.int("id") }.asSingle)
    }

    private fun TransactionalSession.slettPerson(personRef: Int) {
        val infotrygdutbetalingRef = finnInfotrygdutbetalingerRef(personRef)
        val personinfoRef = finnPersoninfoRef(personRef)

        @Language("PostgreSQL")
        val query = "DELETE FROM person WHERE id = ?"
        run(queryOf(query, personRef).asExecute)

        slettInfo(personinfoRef)
        slettInfotrygdutbetalinger(infotrygdutbetalingRef)
    }

    private fun TransactionalSession.slettAvviksvurdering(fødselsnummer: String) {
        val sammenligningsgrunnlagRefs = finnSammenligningsgrunnlag(fødselsnummer)

        @Language("PostgreSQL")
        val query1 =
            "DELETE FROM avviksvurdering WHERE fødselsnummer = :fodselsnummer"
        run(queryOf(query1, mapOf("fodselsnummer" to fødselsnummer)).asUpdate)

        slettSammenligningsgrunnlag(sammenligningsgrunnlagRefs)
    }

    private fun TransactionalSession.slettPersonKlargjøres(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM person_klargjores WHERE fødselsnummer = :fodselsnummer"
        run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer)).asUpdate)
    }

    private fun TransactionalSession.slettPersonpseudoid(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM personpseudoid WHERE identitetsnummer = :fodselsnummer"
        run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer)).asUpdate)
    }

    private fun TransactionalSession.slettSammenligningsgrunnlag(sammenligningsgrunnlagRefs: List<Int>) {
        if (sammenligningsgrunnlagRefs.isEmpty()) return
        @Language("PostgreSQL")
        val query2 =
            "DELETE FROM sammenligningsgrunnlag WHERE id IN (${sammenligningsgrunnlagRefs.joinToString { "?" }}) "
        run(queryOf(query2, *sammenligningsgrunnlagRefs.toTypedArray()).asExecute)
    }

    private fun TransactionalSession.finnSammenligningsgrunnlag(fødselsnummer: String): List<Int> {
        @Language("PostgreSQL")
        val query =
            "SELECT sammenligningsgrunnlag_ref FROM avviksvurdering WHERE fødselsnummer = :fodselsnummer"
        return run(
            queryOf(query, mapOf("fodselsnummer" to fødselsnummer))
                .map {
                    it.int("sammenligningsgrunnlag_ref")
                }.asList,
        )
    }

    private fun TransactionalSession.finnPersoninfoRef(personRef: Int): List<Int> {
        @Language("PostgreSQL")
        val query = "SELECT pi.id FROM person_info pi INNER JOIN person p on pi.id = p.info_ref WHERE p.id = ?"
        return run(queryOf(query, personRef).map { it.int("id") }.asList)
    }

    private fun TransactionalSession.slettInfo(personinfoRef: List<Int>) {
        if (personinfoRef.isEmpty()) return
        @Language("PostgreSQL")
        val query = "DELETE FROM person_info WHERE id IN (${personinfoRef.joinToString { "?" }})"
        run(queryOf(query, *personinfoRef.toTypedArray()).asExecute)
    }

    private fun TransactionalSession.finnInfotrygdutbetalingerRef(personRef: Int): List<Int> {
        @Language("PostgreSQL")
        val query = "SELECT i.id FROM infotrygdutbetalinger i INNER JOIN person p on i.id = p.infotrygdutbetalinger_ref WHERE p.id = ?"
        return run(queryOf(query, personRef).map { it.int("id") }.asList)
    }

    private fun TransactionalSession.slettInfotrygdutbetalinger(infotrygdutbetalingerRef: List<Int>) {
        if (infotrygdutbetalingerRef.isEmpty()) return
        @Language("PostgreSQL")
        val query = "DELETE FROM infotrygdutbetalinger WHERE id IN (${infotrygdutbetalingerRef.joinToString { "?" }})"
        run(queryOf(query, *infotrygdutbetalingerRef.toTypedArray()).asExecute)
    }

    private fun TransactionalSession.slettOpptegnelse(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM opptegnelse WHERE person_id = ?"
        run(queryOf(query, personRef).asExecute)
        slettAbonnementForOpptegnelse(personRef)
    }

    private fun TransactionalSession.slettAbonnementForOpptegnelse(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM abonnement_for_opptegnelse WHERE person_id = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettArbeidsforhold(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM arbeidsforhold WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettReserverPerson(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM reserver_person WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOverstyring(personRef: Int) {
        slettOverstyrtDag(personRef)
        slettOverstyringInntekt(personRef)
        slettOverstyringArbeidsforhold(personRef)
        slettOverstyringTidslinje(personRef)
        slettTilkommenInntekt(personRef)
        slettSkjønnsfastsettingSykepengegrunnlagArbeidsgiver(personRef)
        slettSkjønnsfastsettingSykepengegrunnlag(personRef)
        slettOverstyringMinimumSykdomsgradArbeidsgiver(personRef)
        slettOverstyringMinimumSykdomsgradPeriode(personRef)
        slettOverstyringMinimumSykdomsgrad(personRef)
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOverstyrtDag(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring_dag WHERE overstyring_tidslinje_ref IN (SELECT ot.id FROM overstyring_tidslinje ot JOIN overstyring o ON ot.overstyring_ref = o.id WHERE o.person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOverstyringInntekt(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring_inntekt WHERE overstyring_ref IN (SELECT id FROM overstyring WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOverstyringTidslinje(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring_tidslinje WHERE overstyring_ref IN (SELECT id FROM overstyring WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettTilkommenInntekt(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring_tilkommen_inntekt WHERE overstyring_ref IN (SELECT id FROM overstyring WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOverstyringArbeidsforhold(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring_arbeidsforhold WHERE overstyring_ref IN (SELECT id FROM overstyring WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettSkjønnsfastsettingSykepengegrunnlagArbeidsgiver(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver WHERE skjonnsfastsetting_sykepengegrunnlag_ref IN (SELECT ss.id FROM skjonnsfastsetting_sykepengegrunnlag ss JOIN overstyring o ON ss.overstyring_ref = o.id WHERE o.person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettSkjønnsfastsettingSykepengegrunnlag(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM skjonnsfastsetting_sykepengegrunnlag WHERE overstyring_ref IN (SELECT id FROM overstyring WHERE person_ref = ?) RETURNING begrunnelse_fritekst_ref, begrunnelse_mal_ref, begrunnelse_konklusjon_ref"
        val begrunnelseRef =
            run(
                queryOf(query, personRef)
                    .map {
                        listOf(
                            it.longOrNull("begrunnelse_fritekst_ref"),
                            it.longOrNull("begrunnelse_mal_ref"),
                            it.longOrNull("begrunnelse_konklusjon_ref"),
                        )
                    }.asSingle,
            )
        begrunnelseRef?.forEach {
            it?.let { slettBegrunnelse(it) }
        }
    }

    private fun TransactionalSession.slettOverstyringMinimumSykdomsgradArbeidsgiver(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring_minimum_sykdomsgrad_arbeidsgiver WHERE overstyring_minimum_sykdomsgrad_ref IN (SELECT oms.id FROM overstyring_minimum_sykdomsgrad oms JOIN overstyring o ON oms.overstyring_ref = o.id WHERE o.person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOverstyringMinimumSykdomsgradPeriode(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring_minimum_sykdomsgrad_periode WHERE overstyring_minimum_sykdomsgrad_ref IN (SELECT oms.id FROM overstyring_minimum_sykdomsgrad oms JOIN overstyring o ON oms.overstyring_ref = o.id WHERE o.person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOverstyringMinimumSykdomsgrad(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring_minimum_sykdomsgrad WHERE overstyring_ref IN (SELECT id FROM overstyring WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettAvslag(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM vedtak_begrunnelse WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtaksperiode WHERE person_ref = ?) RETURNING begrunnelse_ref"
        val begrunnelseRef =
            run(
                queryOf(query, personRef)
                    .map {
                        it.longOrNull("begrunnelse_ref")
                    }.asSingle,
            )
        begrunnelseRef?.let { slettBegrunnelse(it) }
    }

    private fun TransactionalSession.slettBegrunnelse(begrunnelseRef: Long) {
        @Language("PostgreSQL")
        val query =
            """DELETE FROM begrunnelse WHERE id = ?"""
        run(queryOf(query, begrunnelseRef).asExecute)
    }

    private fun TransactionalSession.slettVedtak(
        personRef: Int,
        fødselsnummer: String,
    ) {
        slettOppgave(personRef)
        slettVarsler(personRef)
        slettAutomatisering(personRef)
        slettRisikovurdering(personRef)
        slettStansAutomatisering(fødselsnummer)
        slettStansAutomatiskBehandlingSaksbehandler(fødselsnummer)
        slettAutomatiseringProblem(personRef)
        slettSaksbehandleroppgavetype(personRef)
        slettVedtak(personRef)
        slettBehandlinger(personRef)
        slettVedtaksperiodebehandlingV2(personRef)
        slettOpprinneligSøknadsdato(personRef)
        slettUtbetalingIdVedtaksperiodeId(personRef)
        @Language("PostgreSQL")
        val query = "DELETE FROM vedtaksperiode WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettPeriodehistorikk(personRef: Int) {
        @Language("PostgreSQL")
        val query =
            """
            WITH slettet_rad AS (
                DELETE FROM periodehistorikk ph
                USING behandling b
                JOIN vedtaksperiode v ON v.vedtaksperiode_id = b.vedtaksperiode_id
                WHERE ph.behandling_id = b.unik_id
                AND v.person_ref = :personRef returning dialog_ref
            )
            DELETE FROM dialog USING slettet_rad sr WHERE id = sr.dialog_ref
            """.trimIndent()
        run(queryOf(query, mapOf("personRef" to personRef)).asExecute)
    }

    private fun TransactionalSession.slettPåVent(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM pa_vent pv USING vedtaksperiode v WHERE pv.vedtaksperiode_id = v.vedtaksperiode_id AND v.person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettBehandlinger(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
             DELETE FROM behandling b USING vedtaksperiode v WHERE b.vedtaksperiode_id = v.vedtaksperiode_id AND v.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettVedtak(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
             DELETE FROM vedtak v USING behandling b, vedtaksperiode vp WHERE b.spleis_behandling_id = v.behandling_id AND vp.vedtaksperiode_id = b.vedtaksperiode_id AND vp.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettVedtaksperiodebehandlingV2(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
             DELETE FROM behandling_v2 b USING vedtaksperiode v WHERE b.vedtaksperiode_id = v.vedtaksperiode_id AND v.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOpprinneligSøknadsdato(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
             DELETE FROM opprinnelig_soknadsdato os USING vedtaksperiode v WHERE os.vedtaksperiode_id = v.vedtaksperiode_id AND v.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettVarsler(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
             DELETE FROM selve_varsel sv USING vedtaksperiode v WHERE sv.vedtaksperiode_id = v.vedtaksperiode_id AND v.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettKommentarer(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
            DELETE FROM kommentarer 
            WHERE dialog_ref IN (
                SELECT n.dialog_ref 
                FROM notat n 
                    INNER JOIN vedtaksperiode v ON n.vedtaksperiode_id = v.vedtaksperiode_id 
                WHERE v.person_ref = ?
            )"""
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettNotat(personRef: Int) {
        @Language("PostgreSQL")
        val query =
            """
            WITH slettet_rad AS (
                DELETE FROM notat WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtaksperiode WHERE person_ref = :personRef) returning dialog_ref
            )
            DELETE FROM dialog USING slettet_rad sr WHERE id = sr.dialog_ref
            """.trimIndent()
        run(queryOf(query, mapOf("personRef" to personRef)).asExecute)
    }

    private fun TransactionalSession.slettAutomatiseringProblem(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM automatisering_problem WHERE vedtaksperiode_ref IN (SELECT id FROM vedtaksperiode WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettAutomatisering(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM automatisering WHERE vedtaksperiode_ref IN (SELECT id FROM vedtaksperiode WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettSaksbehandleroppgavetype(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM saksbehandleroppgavetype WHERE vedtak_ref IN (SELECT id FROM vedtaksperiode WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettTildeling(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM tildeling WHERE oppgave_id_ref IN (SELECT o.id FROM oppgave o INNER JOIN vedtaksperiode v ON o.vedtak_ref = v.id WHERE v.person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOppgaveBehandlingKobling(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM oppgave_behandling_kobling WHERE oppgave_id IN (SELECT o.id FROM oppgave o INNER JOIN vedtaksperiode v on v.id = o.vedtak_ref WHERE v.person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOppgave(personRef: Int) {
        slettTildeling(personRef)
        slettOppgaveBehandlingKobling(personRef)
        @Language("PostgreSQL")
        val query = "DELETE FROM oppgave WHERE vedtak_ref IN (SELECT id FROM vedtaksperiode WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettRisikovurdering(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM risikovurdering_2021 WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtaksperiode WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettStansAutomatisering(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM stans_automatisering WHERE fødselsnummer = ?"
        run(queryOf(query, fødselsnummer).asExecute)
    }

    private fun TransactionalSession.slettStansAutomatiskBehandlingSaksbehandler(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM stans_automatisk_behandling_saksbehandler WHERE fødselsnummer = ?"
        run(queryOf(query, fødselsnummer).asExecute)
    }

    private fun TransactionalSession.slettUtbetalingIdVedtaksperiodeId(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM vedtaksperiode_utbetaling_id WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtaksperiode WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettSnapshot(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM snapshot WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettGosysoppgaver(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM gosysoppgaver WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettEgenAnsatt(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM egen_ansatt WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettUtbetaling(personRef: Int) {
        val annullerAvSaksbehandlerRef = finnAnnullertAvSaksbehandler(personRef)

        @Language("PostgreSQL")
        val query = "DELETE FROM utbetaling WHERE utbetaling_id_ref IN (SELECT id FROM utbetaling_id WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)

        slettUtbetalingId(personRef)
        slettAnnullertAvSaksbehandler(annullerAvSaksbehandlerRef)
    }

    private fun TransactionalSession.finnAnnullertAvSaksbehandler(personRef: Int): List<Int> {
        @Language("PostgreSQL")
        val query =
            """
            SELECT aas.id FROM annullert_av_saksbehandler aas 
            INNER JOIN vedtaksperiode v USING (vedtaksperiode_id)
            WHERE v.person_ref = ?
            """
        return run(queryOf(query, personRef).map { it.int("id") }.asList)
    }

    private fun TransactionalSession.slettAnnullertAvSaksbehandler(annullertAvSaksbehandlerRef: List<Int>) {
        if (annullertAvSaksbehandlerRef.isEmpty()) return
        @Language("PostgreSQL")
        val query = """
            DELETE FROM annullert_av_saksbehandler 
            WHERE id IN (${annullertAvSaksbehandlerRef.joinToString { "?" }})"""
        run(queryOf(query, *annullertAvSaksbehandlerRef.toTypedArray()).asExecute)
    }

    private fun TransactionalSession.finnFagsystemIdRef(personRef: Int): List<Int> {
        @Language("PostgreSQL")
        val query = "SELECT arbeidsgiver_fagsystem_id_ref, person_fagsystem_id_ref FROM utbetaling_id WHERE person_ref = ?"
        return run(
            queryOf(query, personRef)
                .map {
                    listOf(it.int("arbeidsgiver_fagsystem_id_ref"), it.int("person_fagsystem_id_ref"))
                }.asList,
        ).flatten()
    }

    private fun TransactionalSession.slettUtbetalingId(personRef: Int) {
        val oppdragRef = finnFagsystemIdRef(personRef)

        @Language("PostgreSQL")
        val query = "DELETE FROM utbetaling_id WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)

        slettOppdrag(oppdragRef)
    }

    private fun TransactionalSession.slettOppdrag(oppdragRef: List<Int>) {
        if (oppdragRef.isEmpty()) return
        slettUtbetalingslinje(oppdragRef)

        @Language("PostgreSQL")
        val query = "DELETE FROM oppdrag WHERE id IN (${oppdragRef.joinToString { "?" }})"
        run(queryOf(query, *oppdragRef.toTypedArray()).asExecute)
    }

    private fun TransactionalSession.slettUtbetalingslinje(oppdragRef: List<Int>) {
        if (oppdragRef.isEmpty()) return
        @Language("PostgreSQL")
        val query = "DELETE FROM utbetalingslinje WHERE oppdrag_id IN (${oppdragRef.joinToString { "?" }})"
        run(queryOf(query, *oppdragRef.toTypedArray()).asExecute)
    }

    private fun TransactionalSession.slettVergemål(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM vergemal WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettHendelse(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM hendelse WHERE data->>'fødselsnummer' = ?"
        run(queryOf(query, fødselsnummer).asExecute)
    }

    private fun TransactionalSession.slettInntekt(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM inntekt WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettDokument(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM dokumenter WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettTotrinnsvurdering(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM totrinnsvurdering WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettTilkommenInntekt(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query =
            "DELETE FROM tilkommen_inntekt_events WHERE fødselsnummer = :fodselsnummer"
        run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer)).asUpdate)
    }
}
