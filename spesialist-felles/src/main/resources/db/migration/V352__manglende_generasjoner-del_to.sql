update selve_vedtaksperiode_generasjon
set skjæringstidspunkt = '2022-11-22',
    fom                = '2022-11-22',
    tom                = '2022-12-20'
where vedtaksperiode_id in ('08a20197-8906-44d6-b149-a1dbbd5aa1c4', '7cbbad2a-c42a-4633-a7c2-2172daac1cfe')
