INSERT INTO accounts (id, owner_type, owner_id, system_code, balance_minor, created_at) VALUES
    ('00000000-0000-7000-8000-000000000001', 'SYSTEM', NULL, 'HOUSE', 0, now()),
    ('00000000-0000-7000-8000-000000000003', 'SYSTEM', NULL, 'GATEWAY', 0, now());

INSERT INTO jackpot_pools (id, code, amount_minor, seed_minor, created_at)
VALUES ('00000000-0000-7000-8000-0000000000a1', 'MAIN', 100000, 100000, now());
