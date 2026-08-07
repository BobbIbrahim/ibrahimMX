-- Existing run_time values are UTC clock values by application convention.
-- Attach the UTC offset explicitly without shifting the clock value or relying
-- on the database session time zone.
alter table automations
    alter column run_time type time with time zone
    using (case when run_time is null then null else (run_time::text || '+00')::time with time zone end);
