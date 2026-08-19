-- ============================================================
-- V2: Seed Default Administrator User and ensure role mappings
-- ============================================================

-- Password is 'Admin123!' (BCrypt hash)
INSERT INTO users (email, password_hash, first_name, last_name, is_active, created_at, updated_at)
SELECT 'admin@glifo.ac.cr',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'Admin',
       'Glifo',
       true,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@glifo.ac.cr');

-- Link default admin user to ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@glifo.ac.cr'
  AND r.name = 'ROLE_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
