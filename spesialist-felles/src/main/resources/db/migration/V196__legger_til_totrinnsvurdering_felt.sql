/*
    Legger til kolonnen totrinngsvurdering, for å markere at
    denne oppgaven trenger ein totrinnsvurdring
*/

ALTER TABLE oppgave
    ADD COLUMN totrinngsvurdering BOOLEAN DEFAULT FALSE;
