ALTER TABLE shifts
    ADD CONSTRAINT fk_shift_owner FOREIGN KEY (owner) REFERENCES users (login);
ALTER TABLE shifts
    ADD CONSTRAINT uq_shift_slot UNIQUE (shift_date, building, slot);
ALTER TABLE shifts
    ADD CONSTRAINT ck_shift_time CHECK (ends_at > starts_at);
ALTER TABLE shifts
    ADD CONSTRAINT ck_shift_slot CHECK (slot BETWEEN 0 AND 7);
ALTER TABLE shifts
    ADD CONSTRAINT ck_shift_building CHECK (building IN (1, 4));
ALTER TABLE availability
    ADD CONSTRAINT fk_availability_user FOREIGN KEY (user_login) REFERENCES users (login);
ALTER TABLE availability
    ADD CONSTRAINT uq_availability_cell UNIQUE (user_login, parity, weekday, slot);
ALTER TABLE availability
    ADD CONSTRAINT ck_availability_parity CHECK (parity IN (0, 1));
ALTER TABLE availability
    ADD CONSTRAINT ck_availability_weekday CHECK (weekday BETWEEN 1 AND 6);
ALTER TABLE exchange_offers
    ADD CONSTRAINT fk_offer_owner FOREIGN KEY (owner) REFERENCES users (login);
ALTER TABLE exchange_offers
    ADD CONSTRAINT fk_offer_shift FOREIGN KEY (shift_id) REFERENCES shifts (id);
ALTER TABLE exchange_offers_declined_by
    ADD CONSTRAINT fk_declined_user FOREIGN KEY (item_value) REFERENCES users (login);
ALTER TABLE exchange_requests
    ADD CONSTRAINT fk_request_initiator FOREIGN KEY (initiator) REFERENCES users (login);
ALTER TABLE exchange_requests
    ADD CONSTRAINT fk_request_recipient FOREIGN KEY (recipient) REFERENCES users (login);
ALTER TABLE exchange_requests
    ADD CONSTRAINT fk_request_offer FOREIGN KEY (offer_id) REFERENCES exchange_offers (id);
ALTER TABLE exchange_requests_shift_ids
    ADD CONSTRAINT fk_request_shift FOREIGN KEY (item_value) REFERENCES shifts (id);
ALTER TABLE time_entries
    ADD CONSTRAINT fk_time_user FOREIGN KEY (user_login) REFERENCES users (login);
ALTER TABLE time_entries
    ADD CONSTRAINT fk_time_shift FOREIGN KEY (shift_id) REFERENCES shifts (id);
ALTER TABLE time_entries
    ADD CONSTRAINT ck_time_order CHECK (ended_at IS NULL OR ended_at >= started_at);
ALTER TABLE notifications
    ADD CONSTRAINT fk_notification_user FOREIGN KEY (user_login) REFERENCES users (login);
ALTER TABLE user_sessions
    ADD CONSTRAINT fk_session_user FOREIGN KEY (login) REFERENCES users (login);
ALTER TABLE users
    ADD CONSTRAINT ck_rating CHECK (score BETWEEN 0 AND 5);
ALTER TABLE users
    ADD CONSTRAINT ck_role CHECK (role IN ('EMPLOYEE', 'SCHEDULER', 'MANAGER'));
CREATE INDEX ix_shifts_owner_date ON shifts (owner, shift_date);
CREATE INDEX ix_requests_status ON exchange_requests (status);
CREATE INDEX ix_notifications_user_read ON notifications (user_login, is_read);
CREATE INDEX ix_time_user ON time_entries (user_login, started_at);
CREATE INDEX ix_session_expiry ON user_sessions (expires_at);
CREATE INDEX ix_session_user ON user_sessions (login);
