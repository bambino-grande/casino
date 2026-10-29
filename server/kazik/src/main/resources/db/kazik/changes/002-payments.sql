CREATE TABLE payments (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    idempotency_key TEXT NOT NULL,
    method TEXT NOT NULL,
    purpose TEXT NOT NULL,
    amount_kopecks BIGINT NOT NULL,
    status TEXT NOT NULL,
    provider_payment_id TEXT,
    transfer_phone TEXT,
    transfer_bank TEXT,
    transfer_recipient TEXT,
    transfer_comment TEXT,
    failure_reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    succeeded_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_payments_operation UNIQUE (operation_id),
    CONSTRAINT uq_payments_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX ix_payments_user ON payments (user_id, id DESC);

CREATE INDEX ix_payments_expiry ON payments (expires_at) WHERE status = 'CREATED';

CREATE INDEX ix_payments_donation_wall ON payments (id DESC) WHERE purpose = 'AUTHOR_DONATION' AND status = 'SUCCEEDED';

CREATE TABLE donations (
    id UUID PRIMARY KEY,
    donor_name TEXT,
    message TEXT,
    show_on_wall BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_donations_payment FOREIGN KEY (id) REFERENCES payments(id)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    trace_id TEXT,
    published_at TIMESTAMP WITH TIME ZONE,
    locked_until TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_outbox_events_unpublished ON outbox_events (id) WHERE published_at IS NULL;
