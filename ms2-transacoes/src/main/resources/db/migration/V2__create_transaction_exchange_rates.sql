CREATE TABLE transaction_exchange_rates (
                                            transaction_id BIGINT        NOT NULL,
                                            currency       VARCHAR(3)    NOT NULL,
                                            rate           NUMERIC(19,6) NOT NULL,
                                            rate_date      DATE          NOT NULL,
                                            CONSTRAINT fk_transaction_exchange_rates_transaction
                                                FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_exchange_rates_transaction
    ON transaction_exchange_rates (transaction_id);