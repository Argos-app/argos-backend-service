

create or replace view vw_saldo_estoque as
select mb.id                                                      as medicine_batch_id,
       m.brand_name,
       mb.batch_number,
       sm.movement_date,
       sm.type,
       sm.quantity,
       SUM(case when sm.type = 'E' then sm.quantity else -sm.quantity end)
       over (partition by mb.id order by sm.movement_date, sm.id) as saldo_acumulado
from stock_movement sm
         join medicine_batch mb on mb.id = sm.medicine_batch_id
         join medicine m on m.id = mb.medicine_id;

create or replace view vw_animal_application as
select
    m.brand_name as "medicineName",
    ae.ear_tag as "earCode",
    a.application_date as "applicationDate",
    l.lot_code as "lotCode",
    aa.meat_withdrawal_end as "meatWithdrawalEnd",

    case
        when aa.meat_withdrawal_end is null
            or aa.meat_withdrawal_end <= current_date
            then 'Concluida'
        else 'Pendente'
        end as "status"

from application_animal aa

         join application a
              on a.id = aa.application_id

         join animal_exception ae
              on ae.id = aa.animal_exception_id

         join lot l
              on l.id = a.lot_id

         join medicine_batch mb
              on mb.id = a.medicine_batch_id

         join medicine m
              on m.id = mb.medicine_id;

create or replace view vw_lot_application as
select
    m.brand_name as "medicineName",
    l.lot_code as "lotCode",
    a.application_date as "applicationDate",
    a.meat_withdrawal_end as "meatWithdrawalEnd",

    case
        when a.meat_withdrawal_end is null
            or a.meat_withdrawal_end <= current_date
            then 'Concluida'
        else 'Pendente'
        end as "status",

    a.head_count_applied as "headCount"

from application a

         join lot l
              on l.id = a.lot_id

         join medicine_batch mb
              on mb.id = a.medicine_batch_id

         join medicine m
              on m.id = mb.medicine_id

where not exists (
    select 1
    from application_animal aa
    where aa.application_id = a.id
);

create or replace view vw_evolucao_peso as
select ae.id                           as animal_exception_id,
       ae.ear_tag,
       ap.application_date,
       aa.weight_at_time,
       LAG(aa.weight_at_time) over w   as peso_anterior,
       LAG(ap.application_date) over w as data_anterior,
       ROUND(
               (aa.weight_at_time - LAG(aa.weight_at_time) over w)
                   / NULLIF(ap.application_date - LAG(ap.application_date) over w, 0)::NUMERIC
           , 3)                        as ganho_medio_diario
from application_animal aa
         join application ap on ap.id = aa.application_id
         join animal_exception ae on ae.id = aa.animal_exception_id
where aa.weight_at_time is not null
WINDOW w as (partition by ae.id order by ap.application_date);


create or replace view vw_ranking_conformidade as
select l.id                                                        as lot_id,
       l.lot_code,
       p.name                                                      as property_name,
       l.head_count,
       l.fit_for_slaughter,
       l.slaughter_release_date,
       RANK() over (order by l.slaughter_release_date nulls FIRST) as posicao
from lot l
         join herd h on h.id = l.herd_id
         join property p on p.id = h.property_id
where l.active = true;


create or replace view vw_linhagem_lote as
with recursive linhagem as (select l.id,
                                   l.lot_code,
                                   l.parent_lot_id,
                                   1                as nivel,
                                   l.lot_code::TEXT as caminho,
                                   l.id             as lote_consultado
                            from lot l
                            union all
                            select pai.id,
                                   pai.lot_code,
                                   pai.parent_lot_id,
                                   f.nivel + 1,
                                   pai.lot_code || ' > ' || f.caminho,
                                   f.lote_consultado
                            from lot pai
                                     join linhagem f on f.parent_lot_id = pai.id
                            where f.nivel < 20)
select lote_consultado, id as lote_ancestral, lot_code, nivel, caminho
from linhagem;


create or replace view vw_categoria_pendente as
select m.id,
       m.brand_name,
       m.source_id,
       m.created_at
from medicine m
         join therapeutic_category tc on tc.id = m.therapeutic_category_id
where tc.name = 'Não informado';

-- tabelas de fato e dimensao star schema

create table dim_date
(
    date_key   INTEGER primary key,
    date       DATE        not null unique,
    day        INTEGER     not null,
    month      INTEGER     not null,
    month_name VARCHAR(12) not null,
    quarter    INTEGER     not null,
    year       INTEGER     not null,
    week       INTEGER     not null,
    is_weekend BOOLEAN     not null
);

insert into dim_date (date_key, date, day, month, month_name, quarter, year, week, is_weekend)
select TO_CHAR(d, 'YYYYMMDD')::INTEGER,
       d::DATE,
       EXTRACT(DAY from d)::INTEGER,
       EXTRACT(MONTH from d)::INTEGER,
       TRIM(TO_CHAR(d, 'Month')),
       EXTRACT(QUARTER from d)::INTEGER,
       EXTRACT(YEAR from d)::INTEGER,
       EXTRACT(WEEK from d)::INTEGER,
       EXTRACT(ISODOW from d) in (6, 7)
from generate_series('2024-01-01'::DATE, '2030-12-31'::DATE, '1 day') as d;

create or replace view dim_property as
select id as property_key, name, city, state, area_hectares
from property;

create or replace view dim_lot as
select l.id as lot_key,
       l.lot_code,
       l.category,
       h.herd_name,
       h.breed,
       l.is_exception_lot,
       l.head_count,
       l.open_date,
       l.close_date,
       l.active
from lot l
         join herd h on h.id = l.herd_id;

create or replace view dim_animal_exception as
select id      as animal_exception_key,
       ear_tag,
       category,
       case
           when birth_date is null then null
           else (EXTRACT(YEAR from AGE(current_date, birth_date)) * 12
               + EXTRACT(MONTH from AGE(current_date, birth_date)))::INTEGER
           end as age_months,
       exception_reason,
       status,
       fit_for_slaughter
from animal_exception;


create or replace view dim_medicine as
select m.id           as medicine_key,
       m.brand_name,
       m.active_ingredient,
       tc.name        as therapeutic_category,
       m.needs_prescription,
       m.meat_withdrawal_days,
       m.route,
       s.company_name as supplier_name
from medicine m
         join therapeutic_category tc on tc.id = m.therapeutic_category_id
         join supplier s on s.id = m.supplier_id;

create or replace view dim_user as
select u.id as user_key, u.name, p.code as permission_code, u.active
from app_user u
         join permission p on p.id = u.permission_id;


create or replace view fact_application as
select ap.id                                             as application_id,
       TO_CHAR(ap.application_date, 'YYYYMMDD')::INTEGER as date_key,
       ap.lot_id                                         as lot_key,
       mb.medicine_id                                    as medicine_key,
       pr.id                                             as property_key,
       ap.user_id                                        as user_key,
       ap.dose,
       ap.head_count_applied,
       m.meat_withdrawal_days                            as withdrawal_days,
       (ap.meat_withdrawal_end - current_date)           as days_to_release,
       (ap.meat_withdrawal_end <= current_date)          as released_for_slaughter,
       1                                                 as application_count
from application ap
         join medicine_batch mb on mb.id = ap.medicine_batch_id
         join medicine m on m.id = mb.medicine_id
         join lot l on l.id = ap.lot_id
         join herd h on h.id = l.herd_id
         join property pr on pr.id = h.property_id;


create or replace view fact_stock_movement as
select sm.id                                                                     as stock_movement_id,
       TO_CHAR(sm.movement_date, 'YYYYMMDD')::INTEGER                            as date_key,
       mb.medicine_id                                                            as medicine_key,
       pr.id                                                                     as property_key,
       sm.user_id                                                                as user_key,
       case when sm.type = 'E' then sm.quantity else 0 end                       as quantity_in,
       case when sm.type = 'S' then sm.quantity else 0 end                       as quantity_out,
       SUM(case when sm.type = 'E' then sm.quantity else -sm.quantity end)
       over (partition by sm.medicine_batch_id order by sm.movement_date, sm.id) as running_balance
from stock_movement sm
         join medicine_batch mb on mb.id = sm.medicine_batch_id
         left join application ap on ap.id = sm.application_id
         left join lot l on l.id = ap.lot_id
         left join herd h on h.id = l.herd_id
         left join property pr on pr.id = h.property_id;


create or replace view fact_occurrence as
select oc.id                                            as occurrence_id,
       TO_CHAR(oc.occurrence_date, 'YYYYMMDD')::INTEGER as date_key,
       oc.animal_exception_id                           as animal_exception_key,
       ae.lot_id                                        as lot_key,
       pr.id                                            as property_key,
       oc.type,
       oc.resolved,
       (oc.resolved_at - oc.occurrence_date)            as days_to_resolve,
       1                                                as occurrence_count
from occurrence oc
         join animal_exception ae on ae.id = oc.animal_exception_id
         join lot l on l.id = ae.lot_id
         join herd h on h.id = l.herd_id
         join property pr on pr.id = h.property_id;



