with req as (
  select count(*) as n from metric_event
  where type = 'RECO_REQUEST' and created_at >= now() - interval '24 hours'
),
clicks as (
  select count(*) as n from interaction
  where kind in ('VIEW','LIKE','BOOKMARK','SHARE') and created_at >= now() - interval '24 hours'
)
select (select n from clicks)::numeric / greatest((select n from req),1) as ctr_proxy_24h;

select c.category, count(i.id) as interactions
from interaction i join content c on c.id = i.content_id
where i.created_at >= now() - interval '24 hours'
group by c.category
order by interactions desc
limit 20;

select avg(case when cache_hit then 1.0 else 0.0 end) as hit_rate
from metric_event
where type = 'RECO_REQUEST' and created_at >= now() - interval '24 hours';
