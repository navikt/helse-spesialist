UPDATE oppgave SET status = 'Ferdigstilt' WHERE status = 'AvventerSystem' AND oppdatert::date >= date '2021-05-01' AND oppdatert <= timestamp '2021-05-10 10:00:00';
