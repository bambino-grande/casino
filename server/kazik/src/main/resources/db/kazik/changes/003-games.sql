CREATE TABLE jackpot_pools (
    id UUID PRIMARY KEY,
    code TEXT NOT NULL,
    amount_minor BIGINT NOT NULL,
    seed_minor BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_jackpot_pools_code UNIQUE (code)
);

CREATE TABLE rounds (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    idempotency_key TEXT NOT NULL,
    machine_code TEXT NOT NULL,
    paytable_version INTEGER NOT NULL,
    bet_minor BIGINT NOT NULL,
    reels TEXT NOT NULL,
    win_minor BIGINT NOT NULL,
    jackpot_minor BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_rounds_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX ix_rounds_user ON rounds (user_id, id DESC);

CREATE INDEX ix_rounds_jackpots ON rounds (id DESC) WHERE jackpot_minor > 0;
