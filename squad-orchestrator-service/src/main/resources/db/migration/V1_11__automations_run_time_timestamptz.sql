-- A recurring schedule has no meaningful execution date; run_time now
-- persists as an explicit UTC timestamptz anchored to 1970-01-01. Only the
-- UTC hour/minute (plus weekly_day for WEEKLY automations) is ever used for
-- scheduling; the anchor date itself carries no meaning.
--
-- Existing values are `time with time zone`. The conversion first resolves
-- each value to its plain UTC time-of-day via `AT TIME ZONE 'UTC'` (which,
-- for a timetz operand, is independent of the database session time zone),
-- combines it with the fixed anchor date, and finally reinterprets that
-- civil timestamp as UTC to produce the stored timestamptz instant. None of
-- this depends on the PostgreSQL session, JVM, developer, or browser time
-- zone.
alter table automations
    alter column run_time type timestamptz
    using (
        case when run_time is null then null
        else (date '1970-01-01' + (run_time at time zone 'UTC')) at time zone 'UTC'
        end
    );
