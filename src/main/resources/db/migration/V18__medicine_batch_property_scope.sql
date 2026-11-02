-- O estoque passa a pertencer a uma propriedade, e não a todos os usuários.
-- Registros antigos sem vínculo inequívoco permanecem sem propriedade e não aparecem
-- na listagem filtrada até que sejam reconciliados.
alter table medicine_batch
    add column property_id uuid references property (id) on delete restrict;

-- É possível inferir a propriedade apenas se todas as aplicações do lote
-- aconteceram numa única fazenda.
with batch_ownership as (
    select a.medicine_batch_id,
           (array_agg(distinct h.property_id))[1] as property_id
    from application a
    join lot l on l.id = a.lot_id
    join herd h on h.id = l.herd_id
    group by a.medicine_batch_id
    having count(distinct h.property_id) = 1
)
update medicine_batch mb
set property_id = ownership.property_id
from batch_ownership ownership
where mb.id = ownership.medicine_batch_id;

alter table medicine_batch drop constraint if exists uq_medicine_batch;

create unique index uq_medicine_batch_per_property
    on medicine_batch (property_id, medicine_id, batch_number)
    where property_id is not null;

-- Mantém a unicidade para registros históricos ainda sem propriedade.
create unique index uq_medicine_batch_unassigned
    on medicine_batch (medicine_id, batch_number)
    where property_id is null;

create index idx_medicine_batch_property_status
    on medicine_batch (property_id, status, expiry_date);
