CREATE TABLE prizes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    round_id UUID NOT NULL,
    image_file TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_prizes_round UNIQUE (round_id)
);

CREATE INDEX ix_prizes_user ON prizes (user_id, id DESC);
