CREATE TABLE transactions (
                              id                  BIGSERIAL      PRIMARY KEY,
                              user_id             BIGINT         NOT NULL,
                              type                VARCHAR(20)    NOT NULL,
                              amount              NUMERIC(19, 2) NOT NULL,
                              description         VARCHAR(255)   NOT NULL,
                              category            VARCHAR(20)    NOT NULL,
                              destination_user_id BIGINT,
                              status              VARCHAR(20)    NOT NULL,
                              rejection_reason    VARCHAR(255),
                              occurred_at         TIMESTAMP      NOT NULL
);

CREATE INDEX idx_transactions_user_occurred ON transactions (user_id, occurred_at);