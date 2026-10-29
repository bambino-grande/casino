CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    owner_type TEXT NOT NULL,
    owner_id UUID,
    system_code TEXT,
    balance_minor BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_accounts_user ON accounts (owner_id) WHERE owner_type = 'USER';

CREATE UNIQUE INDEX uq_accounts_system ON accounts (system_code) WHERE owner_type = 'SYSTEM';

CREATE TABLE ledger_transactions (
    id UUID PRIMARY KEY,
    reference TEXT NOT NULL,
    kind TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ledger_transactions_reference UNIQUE (reference)
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount_minor BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ledger_entries_transaction FOREIGN KEY (transaction_id) REFERENCES ledger_transactions(id),
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX ix_ledger_entries_transaction ON ledger_entries (transaction_id);

CREATE INDEX ix_ledger_entries_account ON ledger_entries (account_id, id DESC) INCLUDE (amount_minor);

CREATE TABLE inbox_messages (
    id UUID PRIMARY KEY,
    consumer TEXT NOT NULL,
    event_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_inbox_messages_consumer_event UNIQUE (consumer, event_id)
);
