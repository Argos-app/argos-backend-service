-- procedures
create or replace procedure sp_registrar_aplicacao(
    p_lot_id UUID,
    p_medicine_batch_id UUID,
    p_user_id UUID,
    p_dose NUMERIC,
    p_unit VARCHAR,
    p_route VARCHAR,
    p_goal VARCHAR default null
) as
$$
DECLARE
    v_cabecas INTEGER;
    v_saldo   NUMERIC(10, 3);
begin
    select head_count into v_cabecas from lot where id = p_lot_id;

    if v_cabecas is null or v_cabecas = 0 then
        raise EXCEPTION 'Lote % nao tem animal', p_lot_id;
    end if;

    v_saldo := fn_saldo_medicine_batch(p_medicine_batch_id);

    if v_saldo < p_dose * v_cabecas then
        raise EXCEPTION 'Saldo insuficiente no frasco %: tem %, precisa de %',
            p_medicine_batch_id, v_saldo, p_dose * v_cabecas;
    end if;

    insert into application (lot_id, medicine_batch_id, user_id, application_date,
                             application_time, dose, unit, route, head_count_applied, goal)
    values (p_lot_id, p_medicine_batch_id, p_user_id, current_date,
            current_time, p_dose, p_unit, p_route, v_cabecas, p_goal);

    commit;
EXCEPTION
    when OTHERS then
        rollback;
        raise;
end;
$$ language plpgsql;

create or replace procedure sp_registrar_aplicacao_animal(
    p_animal_exception_id UUID,
    p_medicine_batch_id UUID,
    p_user_id UUID,
    p_dose NUMERIC,
    p_unit VARCHAR,
    p_route VARCHAR,
    p_goal VARCHAR default null
)
as
$$
DECLARE
    v_lot_id       UUID;
    v_peso         NUMERIC(7,2);
    v_saldo        NUMERIC(10,3);
    v_application  UUID;
    v_carencia     DATE;
begin

    -- localizar o animal e o lote onde ele ta
    select lot_id, current_weight
    into v_lot_id, v_peso
    from animal_exception
    where id = p_animal_exception_id
      and status = 'ATIVO';

    if v_lot_id is null then
        raise EXCEPTION
            'Animal de excecao % nao encontrado ou nao esta ativo',
            p_animal_exception_id;
    end if;



    v_saldo := fn_saldo_medicine_batch(p_medicine_batch_id);

    if v_saldo < p_dose then
        raise EXCEPTION
            'Saldo insuficiente no frasco %: tem %, precisa de %',
            p_medicine_batch_id,
            v_saldo,
            p_dose;
    end if;


    -- calcular fim da carencia
    select fn_data_fim_carencia(medicine_id, current_date)
    into v_carencia
    from medicine_batch
    where id = p_medicine_batch_id;

    if v_carencia is null then
        raise EXCEPTION
            'Lote de medicamento % nao encontrado',
            p_medicine_batch_id;
    end if;


    -- registra a aplicação principal
    insert into application (
        lot_id,
        medicine_batch_id,
        user_id,
        application_date,
        application_time,
        dose,
        unit,
        route,
        head_count_applied,
        meat_withdrawal_end,
        goal
    )
    values (
               v_lot_id,
               p_medicine_batch_id,
               p_user_id,
               current_date,
               current_time,
               p_dose,
               p_unit,
               p_route,
               1,
               v_carencia,
               p_goal
           )
    returning id into v_application;


    -- associar especificamente ao animal
    insert into application_animal (
        application_id,
        animal_exception_id,
        dose_given,
        weight_at_time,
        meat_withdrawal_end,
        released_for_slaughter
    )
    values (
               v_application,
               p_animal_exception_id,
               p_dose,
               v_peso,
               v_carencia,
               v_carencia <= current_date
           );


    commit;

EXCEPTION
    when OTHERS then
        rollback;
        raise;
end;
$$ language plpgsql;

create or replace procedure sp_marcar_excecao(
    p_lot_id UUID,
    p_ear_tag VARCHAR,
    p_motivo VARCHAR,
    p_peso NUMERIC default null
)
as
$$
DECLARE
    v_sublote   UUID;
    v_herd      UUID;
    v_codigo    VARCHAR(20);
    v_categoria VARCHAR(30);
    v_seq       INTEGER;
    v_animal    UUID;
begin

    select herd_id,
           lot_code,
           category
    into v_herd,
        v_codigo,
        v_categoria
    from lot
    where id = p_lot_id;

    if v_herd is null then
        raise EXCEPTION 'Lote % nao encontrado', p_lot_id;
    end if;


    select id
    into v_sublote
    from lot
    where parent_lot_id = p_lot_id
      and is_exception_lot = true
      and active = true
      and fn_norm_texto(exception_reason) = fn_norm_texto(p_motivo);


    if v_sublote is null then

        select COUNT(*) + 1
        into v_seq
        from lot
        where parent_lot_id = p_lot_id
          and is_exception_lot = true;


        insert into lot (
            herd_id,
            parent_lot_id,
            lot_code,
            category,
            is_exception_lot,
            exception_reason
        )
        values (
                   v_herd,
                   p_lot_id,
                   left(v_codigo, 13) || '-EXC' || v_seq,
                   v_categoria,
                   true,
                   p_motivo
               )
        returning id into v_sublote;

    end if;


    insert into animal_exception (
        lot_id,
        origin_lot_id,
        ear_tag,
        category,
        current_weight,
        exception_reason
    )
    values (
               v_sublote,
               p_lot_id,
               p_ear_tag,
               'BOI',
               p_peso,
               p_motivo
           )
    returning id into v_animal;


    insert into animal_lot_history (
        animal_exception_id,
        lot_id,
        reason
    )
    values (
               v_animal,
               v_sublote,
               'EXCECAO'
           );


    commit;

EXCEPTION
    when OTHERS then
        rollback;
        raise;
end;
$$ language plpgsql;


create or replace procedure sp_retornar_excecao(p_animal_id UUID)
as
$$
DECLARE
    v_origem   UUID;
    v_sublote  UUID;
    v_carencia DATE;
    v_restante INTEGER;
begin
    select ae.origin_lot_id,
           ae.lot_id,
           GREATEST(COALESCE(ae.slaughter_release_date, current_date - 1),
                    COALESCE((select MAX(ap.meat_withdrawal_end)
                              from application ap
                              where ap.lot_id = ae.lot_id), current_date - 1))
    into v_origem, v_sublote, v_carencia
    from animal_exception ae
    where ae.id = p_animal_id
      and ae.status = 'ATIVO';

    if v_origem is null then
        raise EXCEPTION 'Animal % nao encontrado ou nao esta em excecao ativa', p_animal_id;
    end if;

    if v_carencia > current_date then
        raise EXCEPTION 'Animal % ainda em carencia ate %', p_animal_id, v_carencia;
    end if;

    update animal_lot_history
    set exit_date = current_date
    where animal_exception_id = p_animal_id
      and lot_id = v_sublote
      and exit_date is null;

    update animal_exception
    set lot_id             = v_origem,
        status             = 'RECUPERADO',
        exception_end_date = current_date,
        fit_for_slaughter  = true
    where id = p_animal_id;

    insert into animal_lot_history (animal_exception_id, lot_id, reason)
    values (p_animal_id, v_origem, 'RETORNO_EXCECAO');

    select head_count into v_restante from lot where id = v_sublote;

    if COALESCE(v_restante, 0) <= 0 then
        update lot
        set close_date = current_date,
            active     = false
        where id = v_sublote;
    end if;

    commit;
EXCEPTION
    when OTHERS then
        rollback;
        raise;
end;
$$ language plpgsql;

