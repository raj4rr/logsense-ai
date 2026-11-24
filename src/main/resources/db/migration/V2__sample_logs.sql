-- Sample seeded log events for testing POSTGRESQL connector
INSERT INTO logsense_log_events (occurred_at, severity, message, service_name, trace_id, request_id, host)
VALUES 
('2026-11-24 09:00:01+00', 'INFO', 'User checkout process initiated cartId=cart-1092', 'checkout-service', 'tr-7001', 'req-7001', 'node-1.prod.internal'),
('2026-11-24 09:00:03+00', 'WARN', 'Stripe payment intent confirmation delayed durationMs=3500', 'payment-service', 'tr-7001', 'req-7001', 'node-2.prod.internal'),
('2026-11-24 09:00:05+00', 'ERROR', 'Payment authorization rejected: Insufficient funds or card decline code=card_declined', 'payment-service', 'tr-7001', 'req-7001', 'node-2.prod.internal'),
('2026-11-24 09:00:06+00', 'ERROR', 'Order status marked as FAILED for orderId=ord-9921', 'order-service', 'tr-7001', 'req-7001', 'node-3.prod.internal'),
('2026-11-24 09:01:00+00', 'INFO', 'Database backup completed successfully size=1.8GB', 'db-admin', 'tr-7002', 'req-7002', 'db-primary.prod.internal');
