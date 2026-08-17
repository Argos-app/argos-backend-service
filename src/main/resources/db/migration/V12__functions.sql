-- functions

create or replace FUNCTION fn_data_fim_carencia(p_medicine_id UUID, p_data DATE)
    returns DATE as
$$
DECLARE
    v_dias INTEGER;
begin
    select meat_withdrawal_days into v_dias from medicine where id = p_medicine_id;

    if v_dias is null then
        raise EXCEPTION 'Medicamento % nao encontrado', p_medicine_id;
    end if;

    return p_data + v_dias;
end;
$$ language plpgsql;

create or replace FUNCTION fn_lote_apto_abate(p_lot_id UUID, p_data DATE default current_date)
    returns BOOLEAN as
$$
begin
    return not exists (select 1
                       from application
                       where lot_id = p_lot_id
                         and meat_withdrawal_end > p_data);
end;
$$ language plpgsql;

create or replace FUNCTION fn_saldo_medicine_batch(p_batch_id UUID)
    returns NUMERIC as
$$
DECLARE
    v_saldo NUMERIC(10, 3);
begin
    select COALESCE(SUM(case when type = 'E' then quantity else -quantity end), 0)
    into v_saldo
    from stock_movement
    where medicine_batch_id = p_batch_id;

    return v_saldo;
end;
$$ language plpgsql;

-- trigger function: updated_at

create or replace FUNCTION fn_atualiza_data()
    returns trigger as
$$
begin
    NEW.updated_at := current_timestamp;
    return NEW;
end;
$$ language plpgsql;

-- trigger function: auditoria

create or replace FUNCTION fn_auditoria()
    returns trigger as
$$
DECLARE
    v_app_user UUID;
    v_ip       INET;
begin
    begin
        v_app_user := NULLIF(current_setting('app.usuario', true), '')::UUID;
        v_ip := NULLIF(current_setting('app.ip', true), '')::INET;
    EXCEPTION
        when OTHERS then
            v_app_user := null;
            v_ip := null;
    end;

    insert into change_audit (table_name, operation, record_id, old_data, new_data,
                              db_user, app_user_id, ip_address)
    values (TG_TABLE_NAME,
            TG_OP,
            COALESCE(NEW.id, OLD.id)::VARCHAR,
            case when TG_OP in ('UPDATE', 'DELETE') then row_to_json(OLD)::JSONB end,
            case when TG_OP in ('INSERT', 'UPDATE') then row_to_json(NEW)::JSONB end,
            CURRENT_USER,
            v_app_user,
            v_ip);

    return COALESCE(NEW, OLD);
end;
$$ language plpgsql;

-- trigger function: calcula carencia lote

create or replace FUNCTION fn_calcula_carencia_lote()
    returns trigger as
$$
DECLARE
    v_medicine_id UUID;
begin
    select medicine_id
    into v_medicine_id
    from medicine_batch
    where id = NEW.medicine_batch_id;

    NEW.meat_withdrawal_end := fn_data_fim_carencia(v_medicine_id, NEW.application_date);

    if NEW.head_count_applied is null then
        select head_count into NEW.head_count_applied from lot where id = NEW.lot_id;
    end if;

    return NEW;
end;
$$ language plpgsql;

-- trigger function: atualiza apto abate lote

create or replace FUNCTION fn_atualiza_apto_abate_lote()
    returns trigger as
$$
DECLARE
    v_maior DATE;
begin
    select MAX(meat_withdrawal_end)
    into v_maior
    from application
    where lot_id = NEW.lot_id;

    update lot
    set slaughter_release_date = v_maior,
        fit_for_slaughter      = (v_maior is null or v_maior <= current_date)
    where id = NEW.lot_id;

    return NEW;
end;
$$ language plpgsql;

-- trigger function: calcula carencia excecao

create or replace FUNCTION fn_calcula_carencia_excecao()
    returns trigger as
$$
DECLARE
    v_medicine_id UUID;
    v_data        DATE;
begin
    select mb.medicine_id, ap.application_date
    into v_medicine_id, v_data
    from application ap
             join medicine_batch mb on mb.id = ap.medicine_batch_id
    where ap.id = NEW.application_id;

    NEW.meat_withdrawal_end := fn_data_fim_carencia(v_medicine_id, v_data);
    NEW.released_for_slaughter := (NEW.meat_withdrawal_end <= current_date);

    update animal_exception
    set slaughter_release_date = GREATEST(COALESCE(slaughter_release_date, NEW.meat_withdrawal_end),
                                          NEW.meat_withdrawal_end),
        fit_for_slaughter      = false
    where id = NEW.animal_exception_id;

    return NEW;
end;
$$ language plpgsql;

-- trigger function: baixa estoque

create or replace FUNCTION fn_baixa_estoque()
    returns trigger as
$$
begin
    insert into stock_movement (medicine_batch_id, application_id, user_id,
                                type, quantity, reason)
    values (NEW.medicine_batch_id, NEW.id, NEW.user_id,
            'S', NEW.dose * NEW.head_count_applied, 'APLICACAO');

    return NEW;
end;
$$ language plpgsql;

-- trigger function: gera alerta

create or replace FUNCTION fn_gera_alerta()
    returns trigger as
$$
DECLARE
    v_codigo VARCHAR(20);
begin
    select lot_code into v_codigo from lot where id = NEW.lot_id;

    insert into alert (user_id, lot_id, type, severity, title, message)
    values (NEW.user_id,
            NEW.lot_id,
            'CARENCIA',
            'MEDIA',
            'Lote em carencia: ' || v_codigo,
            'O lote ' || v_codigo || ' fica em carencia ate ' ||
            TO_CHAR(NEW.meat_withdrawal_end, 'DD/MM/YYYY') || '.');

    return NEW;
end;
$$ language plpgsql;

-- trigger function: atualiza contagem

create or replace FUNCTION fn_atualiza_contagem()
    returns trigger as
$$
begin
    if TG_OP = 'INSERT' then
        update lot set head_count = head_count - 1 where id = NEW.origin_lot_id;
        update lot set head_count = head_count + 1 where id = NEW.lot_id;
    ELSIF TG_OP = 'UPDATE' and NEW.lot_id <> OLD.lot_id then
        update lot set head_count = head_count - 1 where id = OLD.lot_id;
        update lot set head_count = head_count + 1 where id = NEW.lot_id;
    end if;

    return NEW;
end;
$$ language plpgsql;

-- trigger function: registra acesso diario

create or replace FUNCTION fn_registra_acesso_diario()
    returns trigger as
$$
begin
    if NEW.success and NEW.user_id is not null then
        insert into daily_access (user_id, date, source, session_count, first_login_time)
        values (NEW.user_id, NEW.happened_at::DATE, 'MOBILE', 1, NEW.happened_at)
        on CONFLICT (user_id, date, source)
            do update set session_count = daily_access.session_count + 1;
    end if;

    return NEW;
end;
$$ language plpgsql;
