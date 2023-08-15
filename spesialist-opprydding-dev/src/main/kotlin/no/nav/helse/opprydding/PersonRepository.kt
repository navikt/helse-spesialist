package no.nav.helse.opprydding

import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

internal class PersonRepository(private val dataSource: DataSource) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun slett(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            session.transaction {
                val personId = it.finnPerson(fødselsnummer) ?: run {
                    sikkerlogg.info("Fant ikke person med fødselsnummer $fødselsnummer, avbryter sletting")
                    return@transaction
                }
                it.slettOverstyring(personId)
                it.slettReserverPerson(personId)
                it.slettOpptegnelse(personId)
                it.slettPeriodehistorikk(personId)
                it.slettTotrinnsvurdering(personId)
                it.slettUtbetaling(personId)
                it.slettVedtak(personId)
                it.slettArbeidsforhold(personId)
                it.slettSnapshot(personId)
                it.slettGosysoppgaver(personId)
                it.slettEgenAnsatt(personId)
                it.slettVergemål(personId)
                it.slettHendelse(fødselsnummer)
                it.slettInntekt(personId)
                it.slettPerson(personId)

                sikkerlogg.info("Person med fødselsnummer $fødselsnummer ble slettet")
            }
        }
    }

    private fun TransactionalSession.finnPerson(fødselsnummer: String): Int? {
        @Language("PostgreSQL")
        val query = "SELECT id FROM person WHERE fodselsnummer = ?"
        return run(queryOf(query, fødselsnummer.toLong()).map { it.int("id") }.asSingle)
    }

    private fun TransactionalSession.slettPerson(personRef: Int) {
        val infotrygdutbetalingRef = finnInfotrygdutbetalingerRef(personRef)
        val personinfoRef = finnPersoninfoRef(personRef)

        @Language("PostgreSQL")
        val query2 = "DELETE FROM stottetabell_for_skjonnsmessig_fastsettelse WHERE fodselsnummer = (SELECT fodselsnummer FROM person WHERE id = ?)"
        run(queryOf(query2, personRef).asExecute)

        @Language("PostgreSQL")
        val query = "DELETE FROM person WHERE id = ?"
        run(queryOf(query, personRef).asExecute)

        slettInfo(personinfoRef)
        slettInfotrygdutbetalinger(infotrygdutbetalingRef)
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
        slettOverstyringerForVedtaksperioder(personRef)
        slettOverstyrtDag(personRef)
        slettOverstyringInntekt(personRef)
        slettOverstyringArbeidsforhold(personRef)
        slettOverstyringTidslinje(personRef)
        slettSkjønnsfastsettingSykepengegrunnlagArbeidsgiver(personRef)
        slettSkjønnsfastsettingSykepengegrunnlag(personRef)
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyring WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOverstyringerForVedtaksperioder(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM overstyringer_for_vedtaksperioder WHERE overstyring_ref IN (SELECT id FROM overstyring WHERE person_ref = ?)"
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
        val query = "DELETE FROM skjonnsfastsetting_sykepengegrunnlag WHERE overstyring_ref IN (SELECT id FROM overstyring WHERE person_ref = ?) RETURNING begrunnelse_fritekst_ref, begrunnelse_mal_ref"
        val begrunnelseRef = run(queryOf(query, personRef).map { it.longOrNull("begrunnelse_fritekst_ref") to it.longOrNull("begrunnelse_mal_ref") }.asList)
        begrunnelseRef.forEach { refs ->
            refs.first?.let { slettBegrunnelse(it) }
            refs.second?.let { slettBegrunnelse(it) }
        }
    }

    private fun TransactionalSession.slettBegrunnelse(begrunnelseRef: Long) {
        @Language("PostgreSQL")
        val query =
            """DELETE FROM begrunnelse WHERE id = ?"""
        run(queryOf(query, begrunnelseRef).asExecute)
    }

    private fun TransactionalSession.slettVedtak(personRef: Int) {
        slettKommentarer(personRef)
        slettNotat(personRef)
        slettOppgave(personRef)
        slettVarsler(personRef)
        slettAutomatisering(personRef)
        slettRisikovurdering(personRef)
        slettAutomatiseringProblem(personRef)
        slettSaksbehandleroppgavetype(personRef)
        slettVedtaksperiodegenerasjoner(personRef)
        slettOpprinneligSøknadsdato(personRef)
        slettUtbetalingIdVedtaksperiodeId(personRef)
        @Language("PostgreSQL")
        val query = "DELETE FROM vedtak WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettPeriodehistorikk(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM periodehistorikk WHERE utbetaling_id IN (SELECT utbetaling_id FROM utbetaling_id WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettVedtaksperiodegenerasjoner(personRef: Int) {
        slettGenerasjonBegrunnelseKoblinger(personRef)
        @Language("PostgreSQL")
        val query = """
             DELETE FROM selve_vedtaksperiode_generasjon svg USING vedtak v WHERE svg.vedtaksperiode_id = v.vedtaksperiode_id AND v.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettGenerasjonBegrunnelseKoblinger(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
             DELETE FROM generasjon_begrunnelse_kobling gbk USING vedtak v INNER JOIN selve_vedtaksperiode_generasjon svg on v.vedtaksperiode_id = svg.vedtaksperiode_id WHERE gbk.generasjon_id = svg.unik_id AND v.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOpprinneligSøknadsdato(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
             DELETE FROM opprinnelig_soknadsdato os USING vedtak v WHERE os.vedtaksperiode_id = v.vedtaksperiode_id AND v.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettVarsler(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
             DELETE FROM selve_varsel sv USING vedtak v WHERE sv.vedtaksperiode_id = v.vedtaksperiode_id AND v.person_ref = ?
        """
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettKommentarer(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
            DELETE FROM kommentarer 
            WHERE notat_ref IN (
                SELECT n.id 
                FROM notat n 
                    INNER JOIN vedtak v ON n.vedtaksperiode_id = v.vedtaksperiode_id 
                WHERE v.person_ref = ?
            )"""
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettNotat(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM notat WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtak WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettAutomatiseringProblem(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM automatisering_problem WHERE vedtaksperiode_ref IN (SELECT id FROM vedtak WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettAutomatisering(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM automatisering WHERE vedtaksperiode_ref IN (SELECT id FROM vedtak WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettSaksbehandleroppgavetype(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM saksbehandleroppgavetype WHERE vedtak_ref IN (SELECT id FROM vedtak WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettTildeling(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM tildeling WHERE oppgave_id_ref IN (SELECT o.id FROM oppgave o INNER JOIN vedtak v ON o.vedtak_ref = v.id WHERE v.person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOppgaveBehandlingKobling(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM oppgave_behandling_kobling WHERE oppgave_id IN (SELECT o.id FROM oppgave o INNER JOIN vedtak v on v.id = o.vedtak_ref WHERE v.person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettOppgave(personRef: Int) {
        slettTildeling(personRef)
        slettOppgaveBehandlingKobling(personRef)
        @Language("PostgreSQL")
        val query = "DELETE FROM oppgave WHERE vedtak_ref IN (SELECT id FROM vedtak WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettRisikovurdering(personRef: Int) {
        slettRisikovurdering2021(personRef)
        slettRisikovurderingFaresignal(personRef)
        slettRisikovurderingArbeidsuførhetsvurdering(personRef)
        @Language("PostgreSQL")
        val query = "DELETE FROM risikovurdering WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtak WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettRisikovurdering2021(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM risikovurdering_2021 WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtak WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettRisikovurderingFaresignal(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
            DELETE FROM risikovurdering_faresignal 
            WHERE risikovurdering_ref IN (
                SELECT r.id FROM risikovurdering r 
                INNER JOIN vedtak v ON r.vedtaksperiode_id = v.vedtaksperiode_id 
                WHERE person_ref = ?
            )"""
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettRisikovurderingArbeidsuførhetsvurdering(personRef: Int) {
        @Language("PostgreSQL")
        val query = """
            DELETE FROM risikovurdering_arbeidsuforhetvurdering 
            WHERE risikovurdering_ref IN (
                SELECT r.id FROM risikovurdering r 
                INNER JOIN vedtak v ON r.vedtaksperiode_id = v.vedtaksperiode_id 
                WHERE person_ref = ?
            )"""
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettUtbetalingIdVedtaksperiodeId(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM vedtaksperiode_utbetaling_id WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtak WHERE person_ref = ?)"
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
        val query = """
            SELECT id FROM annullert_av_saksbehandler 
            WHERE id IN (
                SELECT annullert_av_saksbehandler_ref FROM utbetaling_id ui 
                INNER JOIN utbetaling u ON ui.id = u.utbetaling_id_ref 
                WHERE ui.person_ref = ?
            )"""
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
        return run(queryOf(query, personRef).map { listOf(it.int("arbeidsgiver_fagsystem_id_ref"), it.int("person_fagsystem_id_ref")) }.asList).flatten()
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
        run(queryOf(query,  *oppdragRef.toTypedArray()).asExecute)
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
        val query = "DELETE FROM hendelse WHERE fodselsnummer = ?"
        run(queryOf(query, fødselsnummer.toLong()).asExecute)
    }

    private fun TransactionalSession.slettInntekt(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM inntekt WHERE person_ref = ?"
        run(queryOf(query, personRef).asExecute)
    }

    private fun TransactionalSession.slettTotrinnsvurdering(personRef: Int) {
        @Language("PostgreSQL")
        val query = "DELETE FROM totrinnsvurdering WHERE vedtaksperiode_id IN (SELECT vedtaksperiode_id FROM vedtak WHERE person_ref = ?)"
        run(queryOf(query, personRef).asExecute)
    }
}
