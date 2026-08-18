create index idx_application_carencia
    on application (meat_withdrawal_end)
    where meat_withdrawal_end is not null;

create unique index idx_animal_exception_lot_ear_tag
    on animal_exception (lot_id, ear_tag);

create index idx_change_audit_tabela_data
    on change_audit (table_name, happened_at);

create index idx_stock_movement_lote_data
    on stock_movement (medicine_batch_id, movement_date);

create index idx_application_lot_data 
    on application (lot_id, application_date);
