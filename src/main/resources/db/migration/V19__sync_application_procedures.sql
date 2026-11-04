CREATE OR REPLACE PROCEDURE sp_registrar_aplicacao(
    p_lot_id UUID,
    p_medicine_batch_id UUID,
    p_user_id UUID,
    p_dose NUMERIC,
    p_unit VARCHAR,
    p_route VARCHAR,
    p_goal VARCHAR DEFAULT NULL
)
AS $$
DECLARE
    v_cabecas INTEGER;
    v_saldo NUMERIC(10, 3);
BEGIN
    SELECT head_count
    INTO v_cabecas
    FROM lot
    WHERE id = p_lot_id;

    IF v_cabecas IS NULL OR v_cabecas = 0 THEN
        RAISE EXCEPTION 'Lote % nao tem animal', p_lot_id;
    END IF;

    v_saldo := fn_saldo_medicine_batch(p_medicine_batch_id);

    IF v_saldo < p_dose * v_cabecas THEN
        RAISE EXCEPTION 'Saldo insuficiente no frasco %: tem %, precisa de %',
            p_medicine_batch_id, v_saldo, p_dose * v_cabecas;
    END IF;

    INSERT INTO application (
        lot_id,
        medicine_batch_id,
        user_id,
        application_date,
        application_time,
        dose,
        unit,
        route,
        head_count_applied,
        goal
    )
    VALUES (
        p_lot_id,
        p_medicine_batch_id,
        p_user_id,
        CURRENT_DATE,
        CURRENT_TIME,
        p_dose,
        p_unit,
        p_route,
        v_cabecas,
        p_goal
    );
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE sp_registrar_aplicacao_animal(
    p_animal_exception_id UUID,
    p_medicine_batch_id UUID,
    p_user_id UUID,
    p_dose NUMERIC,
    p_unit VARCHAR,
    p_route VARCHAR,
    p_goal VARCHAR DEFAULT NULL
)
AS $$
DECLARE
    v_lot_id UUID;
    v_peso NUMERIC(7, 2);
    v_saldo NUMERIC(10, 3);
    v_application UUID;
    v_carencia DATE;
BEGIN
    SELECT lot_id, current_weight
    INTO v_lot_id, v_peso
    FROM animal_exception
    WHERE id = p_animal_exception_id
      AND status = 'ATIVO';

    IF v_lot_id IS NULL THEN
        RAISE EXCEPTION
            'Animal de excecao % nao encontrado ou nao esta ativo',
            p_animal_exception_id;
    END IF;

    v_saldo := fn_saldo_medicine_batch(p_medicine_batch_id);

    IF v_saldo < p_dose THEN
        RAISE EXCEPTION
            'Saldo insuficiente no frasco %: tem %, precisa de %',
            p_medicine_batch_id,
            v_saldo,
            p_dose;
    END IF;

    SELECT fn_data_fim_carencia(medicine_id, CURRENT_DATE)
    INTO v_carencia
    FROM medicine_batch
    WHERE id = p_medicine_batch_id;

    IF v_carencia IS NULL THEN
        RAISE EXCEPTION
            'Lote de medicamento % nao encontrado',
            p_medicine_batch_id;
    END IF;

    INSERT INTO application (
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
    VALUES (
        v_lot_id,
        p_medicine_batch_id,
        p_user_id,
        CURRENT_DATE,
        CURRENT_TIME,
        p_dose,
        p_unit,
        p_route,
        1,
        v_carencia,
        p_goal
    )
    RETURNING id INTO v_application;

    INSERT INTO application_animal (
        application_id,
        animal_exception_id,
        dose_given,
        weight_at_time,
        meat_withdrawal_end,
        released_for_slaughter
    )
    VALUES (
        v_application,
        p_animal_exception_id,
        p_dose,
        v_peso,
        v_carencia,
        v_carencia <= CURRENT_DATE
    );
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE sp_retornar_excecao(
    p_animal_id UUID
)
AS $$
DECLARE
    v_origem UUID;
    v_sublote UUID;
    v_carencia DATE;
    v_restante INTEGER;
BEGIN
    SELECT
        ae.origin_lot_id,
        ae.lot_id,
        GREATEST(
            COALESCE(ae.slaughter_release_date, CURRENT_DATE - 1),
            COALESCE(
                (
                    SELECT MAX(ap.meat_withdrawal_end)
                    FROM application ap
                    WHERE ap.lot_id = ae.lot_id
                ),
                CURRENT_DATE - 1
            )
        )
    INTO v_origem, v_sublote, v_carencia
    FROM animal_exception ae
    WHERE ae.id = p_animal_id
      AND ae.status = 'ATIVO';

    IF v_origem IS NULL THEN
        RAISE EXCEPTION
            'Animal % nao encontrado ou nao esta em excecao ativa',
            p_animal_id;
    END IF;

    IF v_carencia > CURRENT_DATE THEN
        RAISE EXCEPTION
            'Animal % ainda em carencia ate %',
            p_animal_id,
            v_carencia;
    END IF;

    UPDATE animal_lot_history
    SET exit_date = CURRENT_DATE
    WHERE animal_exception_id = p_animal_id
      AND lot_id = v_sublote
      AND exit_date IS NULL;

    UPDATE animal_exception
    SET lot_id = v_origem,
        status = 'RECUPERADO',
        exception_end_date = CURRENT_DATE,
        fit_for_slaughter = true
    WHERE id = p_animal_id;

    INSERT INTO animal_lot_history (
        animal_exception_id,
        lot_id,
        reason
    )
    VALUES (
        p_animal_id,
        v_origem,
        'RETORNO_EXCECAO'
    );

    SELECT head_count
    INTO v_restante
    FROM lot
    WHERE id = v_sublote;

    IF COALESCE(v_restante, 0) <= 0 THEN
        UPDATE lot
        SET close_date = CURRENT_DATE,
            active = false
        WHERE id = v_sublote;
    END IF;
END;
$$ LANGUAGE plpgsql;