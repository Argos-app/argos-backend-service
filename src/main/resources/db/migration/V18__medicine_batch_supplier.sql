alter table medicine_batch
    add column supplier_id UUID;

alter table medicine_batch
    add constraint fk_medicine_batch_supplier
        foreign key (supplier_id) references supplier (id) on delete restrict;

create index idx_medicine_batch_supplier on medicine_batch (supplier_id);
