-- Datos iniciales. Hibernate ejecuta este archivo automáticamente justo después de
-- crear las tablas cuando spring.jpa.hibernate.ddl-auto es create o create-drop, así la
-- base arranca lista para usar cada vez que se levanta el backend.
-- Hibernate lee una sentencia por línea: no partir un INSERT en varias líneas.

-- Roles
INSERT INTO roles (name, description) VALUES ('ROLE_STUDENT', 'Default student role'), ('ROLE_TEACHER', 'Course owner and syllabus publisher'), ('ROLE_ADMIN', 'System administrator');

-- Privilegios
INSERT INTO privileges (name, description) VALUES ('NOTE_READ_OWN', 'Read own notes'), ('NOTE_WRITE_OWN', 'Create and edit own notes'), ('NOTE_DELETE_OWN', 'Delete own notes'), ('COURSE_READ', 'View courses'), ('COURSE_WRITE', 'Create and manage courses'), ('SYLLABUS_PUBLISH', 'Publish syllabus for owned courses'), ('GLOSSARY_WRITE', 'Manage course glossary'), ('COVERAGE_READ_OWN', 'View own topic coverage'), ('COVERAGE_READ_COURSE', 'View course-wide coverage stats'), ('STUDY_ITEM_GENERATE', 'Generate study items from notes'), ('STUDY_ATTEMPT_WRITE', 'Submit study attempts'), ('AI_VISION_INVOKE', 'Invoke AI vision services'), ('USAGE_READ_OWN', 'View own AI usage stats'), ('USAGE_READ_COURSE', 'View course AI usage stats'), ('USER_MANAGE', 'Manage users'), ('ROLE_MANAGE', 'Manage roles and privileges');

-- Tabla intermedia role_privileges (relación @ManyToMany con @JoinTable)
INSERT INTO role_privileges (role_id, privilege_id) SELECT r.id, p.id FROM roles r, privileges p WHERE r.name = 'ROLE_STUDENT' AND p.name IN ('NOTE_READ_OWN', 'NOTE_WRITE_OWN', 'NOTE_DELETE_OWN', 'COURSE_READ', 'COVERAGE_READ_OWN', 'STUDY_ITEM_GENERATE', 'STUDY_ATTEMPT_WRITE', 'AI_VISION_INVOKE', 'USAGE_READ_OWN');
INSERT INTO role_privileges (role_id, privilege_id) SELECT r.id, p.id FROM roles r, privileges p WHERE r.name = 'ROLE_TEACHER' AND p.name IN ('NOTE_READ_OWN', 'NOTE_WRITE_OWN', 'NOTE_DELETE_OWN', 'COURSE_READ', 'COURSE_WRITE', 'SYLLABUS_PUBLISH', 'GLOSSARY_WRITE', 'COVERAGE_READ_OWN', 'COVERAGE_READ_COURSE', 'STUDY_ITEM_GENERATE', 'STUDY_ATTEMPT_WRITE', 'AI_VISION_INVOKE', 'USAGE_READ_OWN', 'USAGE_READ_COURSE');
INSERT INTO role_privileges (role_id, privilege_id) SELECT r.id, p.id FROM roles r, privileges p WHERE r.name = 'ROLE_ADMIN' AND p.name IN ('NOTE_READ_OWN', 'COURSE_READ', 'COURSE_WRITE', 'COVERAGE_READ_COURSE', 'USAGE_READ_COURSE', 'USER_MANAGE', 'ROLE_MANAGE');

-- Cuentas demo: las mismas que la pantalla de login del frontend muestra como ayuda
-- (DummyAuthProvider). Las contraseñas van cifradas con BCrypt, igual que las que se
-- registran desde la app. Los ids no se escriben a mano para que la secuencia IDENTITY
-- siga funcionando con los usuarios que se registren después.
INSERT INTO users (email, password_hash, first_name, last_name, is_active, created_at, updated_at) VALUES ('estudiante@est.una.ac.cr', '$2a$10$H2XY/9xVBbhEPQPd7hd6ouKrnbGYYmgVvOHrs0PZ7eYJcXstuCFIS', 'Brandon', 'Brenes', true, now(), now());
INSERT INTO users (email, password_hash, first_name, last_name, is_active, created_at, updated_at) VALUES ('docente@una.ac.cr', '$2a$10$KtfnMczAMXYxyn9AmW4c/.KkvjTlxWaAUlsBWzk07239lYsVCqagi', 'Maikol', 'Guzmán', true, now(), now());
INSERT INTO users (email, password_hash, first_name, last_name, is_active, created_at, updated_at) VALUES ('admin@una.ac.cr', '$2a$10$CK/yRhLSougPtPS8aMeFbe/P8jheQpNr0DIetlTnKEgR0dGyO7VWa', 'Admin', 'Glifo', true, now(), now());

-- Tabla intermedia user_roles
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'estudiante@est.una.ac.cr' AND r.name = 'ROLE_STUDENT';
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'docente@una.ac.cr' AND r.name = 'ROLE_TEACHER';
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'admin@una.ac.cr' AND r.name = 'ROLE_ADMIN';
