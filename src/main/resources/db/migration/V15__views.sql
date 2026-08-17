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
                                     join linhagem f on f.parent_lot_id = pai.id)
select lote_consultado, id as lote_ancestral, lot_code, nivel, caminho
from linhagem;
