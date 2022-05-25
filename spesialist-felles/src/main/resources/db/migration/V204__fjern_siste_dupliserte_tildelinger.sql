delete
from tildeling
where (saksbehandler_ref, oppgave_id_ref) in (
                                              ('5be7f81d-1446-40bf-9df7-8e8db0f9e61d', 2710485),
                                              ('3475a65a-3511-4235-b15e-57efbbf95a4f', 2711556)
    );

-- den siste er komplett duplikat, begge tildelinger er til samme saksbehandler
delete
from tildeling
where ctid = '(3560,46)'
  and oppgave_id_ref = 2709336
