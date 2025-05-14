-- Query to check users table
SELECT id, username, email, role FROM users;

-- Insert default admin user if none exists
INSERT INTO users (username, email, password, role, balance, created_at)
SELECT 'admin', 'admin@transit.tn', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ADMIN', 0.00, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role = 'ADMIN');

-- Insert default user if none exists
INSERT INTO users (username, email, password, role, balance, created_at)
SELECT 'user', 'user@transit.tn', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', 100.00, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'user@transit.tn');

-- Password is 'password' 