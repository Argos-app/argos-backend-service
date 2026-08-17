create trigger trg_atualiza_data_app_user
    BEFORE update
    on app_user
    for each row
execute FUNCTION fn_atualiza_data();

create trigger trg_atualiza_data_user_credential
    BEFORE update
    on user_credential
    for each row
execute FUNCTION fn_atualiza_data();

create trigger trg_atualiza_data_herd
    BEFORE update
    on herd
    for each row
execute FUNCTION fn_atualiza_data();

create trigger trg_atualiza_data_lot
    BEFORE update
    on lot
    for each row
execute FUNCTION fn_atualiza_data();

create trigger trg_atualiza_data_animal_exception
    BEFORE update
    on animal_exception
    for each row
execute FUNCTION fn_atualiza_data();

create trigger trg_atualiza_data_supplier
    BEFORE update
    on supplier
    for each row
execute FUNCTION fn_atualiza_data();

create trigger trg_atualiza_data_medicine
    BEFORE update
    on medicine
    for each row
execute FUNCTION fn_atualiza_data();

create trigger trg_atualiza_data_medicine_batch
    BEFORE update
    on medicine_batch
    for each row
execute FUNCTION fn_atualiza_data();

create trigger trg_atualiza_data_data_catalog
    BEFORE update
    on data_catalog
    for each row
execute FUNCTION fn_atualiza_data();

-- triggers: auditoria

create trigger trg_auditoria_application
    AFTER insert or update or delete
    on application
    for each row
execute FUNCTION fn_auditoria();

create trigger trg_auditoria_stock_movement
    AFTER insert or update or delete
    on stock_movement
    for each row
execute FUNCTION fn_auditoria();

-- triggers: carencia e abate

create trigger trg_calcula_carencia_lote
    BEFORE insert
    on application
    for each row
execute FUNCTION fn_calcula_carencia_lote();

create trigger trg_atualiza_apto_abate_lote
    AFTER insert or update
    on application
    for each row
execute FUNCTION fn_atualiza_apto_abate_lote();

create trigger trg_calcula_carencia_excecao
    BEFORE insert
    on application_animal
    for each row
execute FUNCTION fn_calcula_carencia_excecao();

-- triggers: estoque e alerta

create trigger trg_baixa_estoque
    AFTER insert
    on application
    for each row
execute FUNCTION fn_baixa_estoque();

create trigger trg_gera_alerta
    AFTER insert
    on application
    for each row
execute FUNCTION fn_gera_alerta();

-- triggers: contagem e acesso

create trigger trg_atualiza_contagem
    AFTER insert or update
    on animal_exception
    for each row
execute FUNCTION fn_atualiza_contagem();

create trigger trg_registra_acesso_diario
    AFTER insert
    on access_audit
    for each row
execute FUNCTION fn_registra_acesso_diario();
