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

create or replace procedure sp_marcar_excecao(
    p_lot_id UUID,
    p_ear_tag VARCHAR,
    p_motivo VARCHAR,
    p_peso NUMERIC default null
) as
$$
DECLARE
    v_sublote UUID;
    v_herd    UUID;
    v_codigo  VARCHAR(20);
    v_animal  UUID;
begin
    select herd_id, lot_code into v_herd, v_codigo from lot where id = p_lot_id;

    if v_herd is null then
        raise EXCEPTION 'Lote % nao encontrado', p_lot_id;
    end if;

    select id
    into v_sublote
    from lot
    where parent_lot_id = p_lot_id
      and is_exception_lot = true
      and active = true;

    if v_sublote is null then
        insert into lot (herd_id, parent_lot_id, lot_code, category, is_exception_lot)
        values (v_herd, p_lot_id, left(v_codigo, 14) || '-EXC', 'EXCECAO', true)
        returning id into v_sublote;
    end if;

    insert into animal_exception (lot_id, origin_lot_id, ear_tag, category,
                                  current_weight, exception_reason)
    values (v_sublote, p_lot_id, p_ear_tag, 'BOI', p_peso, p_motivo)
    returning id into v_animal;

    insert into animal_lot_history (animal_exception_id, lot_id, reason)
    values (v_animal, v_sublote, 'EXCECAO');

    commit;
EXCEPTION
    when OTHERS then
        rollback;
        raise;
end;
$$ language plpgsql;

create or replace procedure sp_carga_rpa(p_load_run_id UUID)
as
$$
DECLARE
    r            RECORD;
    v_json       JSONB;
    v_tabela     VARCHAR(60);
    v_target     UUID;
    v_lidos      INTEGER := 0;
    v_inseridos  INTEGER := 0;
    v_atualizado INTEGER := 0;
    v_rejeitados INTEGER := 0;
begin
    for r in select *
             from load_staging
             where load_run_id = p_load_run_id
               and processed = false
        loop
            v_lidos := v_lidos + 1;

            begin
                v_json := r.source_columns::JSONB;
                v_tabela := v_json ->> 'tabela';

                if v_tabela = 'supplier' then
                    select id into v_target from supplier where cnpj = v_json ->> 'cnpj';

                    if v_target is null then
                        insert into supplier (company_name, cnpj, email, phone, address, source_id)
                        values (v_json ->> 'company_name', v_json ->> 'cnpj', v_json ->> 'email',
                                v_json ->> 'phone', v_json ->> 'address',
                                (v_json ->> 'source_id')::INTEGER)
                        returning id into v_target;
                        v_inseridos := v_inseridos + 1;
                    else
                        update supplier
                        set company_name = v_json ->> 'company_name',
                            email        = v_json ->> 'email',
                            phone        = v_json ->> 'phone',
                            address      = v_json ->> 'address'
                        where id = v_target;
                        v_atualizado := v_atualizado + 1;
                    end if;

                    insert into load_mapping (table_name, source_id, target_id, natural_key)
                    values ('supplier', v_json ->> 'source_id', v_target, v_json ->> 'cnpj')
                    on CONFLICT (table_name, source_id) do NOTHING;
                else
                    raise EXCEPTION 'Tabela de origem nao mapeada: %', v_tabela;
                end if;

                update load_staging set processed = true where id = r.id;

            EXCEPTION
                when OTHERS then
                    v_rejeitados := v_rejeitados + 1;
                    insert into load_error (load_run_id, source_table, natural_key,
                                            original_content, error_message)
                    values (p_load_run_id,
                            COALESCE(v_tabela, 'DESCONHECIDA'),
                            COALESCE(v_json ->> 'cnpj', r.row_hash),
                            COALESCE(v_json, jsonb_build_object('raw', r.source_columns)),
                            SQLERRM);
            end;
        end loop;

    update load_run
    set end_time      = current_timestamp,
        rows_read     = v_lidos,
        rows_inserted = v_inseridos,
        rows_updated  = v_atualizado,
        rows_rejected = v_rejeitados,
        loaded_until  = current_timestamp,
        status        = case
                            when v_rejeitados = 0 then 'SUCESSO'
                            when v_rejeitados = v_lidos then 'ERRO'
                            else 'PARCIAL' end
    where id = p_load_run_id;

    commit;
end;
$$ language plpgsql;
