-- Constant training type dictionary; the application never modifies it.
CREATE TABLE IF NOT EXISTS training_type (
    id                 BIGSERIAL PRIMARY KEY,
    training_type_name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO training_type (training_type_name)
VALUES ('STRENGTH'), ('CARDIO'), ('FLEXIBILITY'), ('BALANCE'), ('YOGA'), ('ZUMBA')
ON CONFLICT (training_type_name) DO NOTHING;
