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

-- ============================================================================
-- Curso demo (issue #23): datos para mostrar el modelo relacional + JSONB en la
-- defensa. Mismo curso que el mock de Mockoon (MAT-101 / GLF-CALC1). Los ids se
-- resuelven con subconsultas para no depender del orden de las secuencias.
-- ============================================================================

-- Curso del docente y matrícula del estudiante
INSERT INTO courses (owner_user_id, code, name, term, join_code, syllabus_source_uri, syllabus_parsed_at, created_at) SELECT u.id, 'MAT-101', 'Cálculo I', 'II-2026', 'GLF-CALC1', 'syllabus/mat-101.pdf', now(), now() FROM users u WHERE u.email = 'docente@una.ac.cr';
INSERT INTO enrollments (user_id, course_id, status, joined_at) SELECT u.id, c.id, 'ACTIVE', now() FROM users u, courses c WHERE u.email = 'estudiante@est.una.ac.cr' AND c.join_code = 'GLF-CALC1';

-- Temario jerárquico (syllabus_topics se referencia a sí misma con parent_id)
INSERT INTO syllabus_topics (course_id, parent_id, code, title, order_index) SELECT c.id, NULL, 'U1', 'Límites y continuidad', 1 FROM courses c WHERE c.join_code = 'GLF-CALC1';
INSERT INTO syllabus_topics (course_id, parent_id, code, title, order_index) SELECT c.id, t.id, 'U1.1', 'Límite de una función', 1 FROM courses c JOIN syllabus_topics t ON t.course_id = c.id AND t.code = 'U1' WHERE c.join_code = 'GLF-CALC1';
INSERT INTO syllabus_topics (course_id, parent_id, code, title, order_index) SELECT c.id, t.id, 'U1.2', 'Teorema del valor extremo', 2 FROM courses c JOIN syllabus_topics t ON t.course_id = c.id AND t.code = 'U1' WHERE c.join_code = 'GLF-CALC1';
INSERT INTO syllabus_topics (course_id, parent_id, code, title, order_index) SELECT c.id, NULL, 'U2', 'Derivadas', 2 FROM courses c WHERE c.join_code = 'GLF-CALC1';

-- Glosario canónico del docente (D-15: el estudiante solo sugiere)
INSERT INTO course_glossary (course_id, term, canonical_form, kind) SELECT c.id, 'lim', '\lim_{x \to a}', 'NOTATION' FROM courses c WHERE c.join_code = 'GLF-CALC1';
INSERT INTO course_glossary (course_id, term, canonical_form, kind) SELECT c.id, 'ε', '\varepsilon', 'SYMBOL' FROM courses c WHERE c.join_code = 'GLF-CALC1';
INSERT INTO course_glossary (course_id, term, canonical_form, kind) SELECT c.id, 'compacto', 'conjunto cerrado y acotado', 'TERM' FROM courses c WHERE c.join_code = 'GLF-CALC1';

-- Apunte reconstruido: notes.content es el documento IA-01 con confianza por fragmento
INSERT INTO notes (user_id, course_id, syllabus_topic_id, class_date, title, status, content, content_generated_at, created_at) SELECT u.id, c.id, t.id, DATE '2026-09-16', 'Límites: definición épsilon-delta', 'READY', '{"schemaVersion": 1, "blocks": [{"id": "b1", "type": "HEADING", "level": 2, "text": "Límite de una función"}, {"id": "b2", "type": "PARAGRAPH", "spans": [{"text": "Decimos que f tiende a L cuando x tiende a a si ", "confidence": 0.96, "resolvedAt": "N1", "regionId": "r1"}, {"text": "para todo ε existe δ", "confidence": 0.74, "resolvedAt": "N2", "regionId": "r2"}]}, {"id": "b3", "type": "FORMULA", "latex": "\\lim_{x \\to a} f(x) = L", "confidence": 0.88, "resolvedAt": "N1_5", "regionId": "r3", "latexCompiles": true}, {"id": "b4", "type": "PARAGRAPH", "spans": [{"text": "[ilegible]", "confidence": 0.21, "resolvedAt": "UNRESOLVED", "regionId": "r4", "isUncertain": true}]}], "summary": {"verified": 1, "repaired": 1, "escalated": 1, "uncertain": 1}}', now(), now() FROM users u, courses c JOIN syllabus_topics t ON t.course_id = c.id AND t.code = 'U1.1' WHERE u.email = 'estudiante@est.una.ac.cr' AND c.join_code = 'GLF-CALC1';
INSERT INTO notes (user_id, course_id, syllabus_topic_id, class_date, title, status, content, content_generated_at, created_at) SELECT u.id, c.id, NULL, DATE '2026-09-23', 'Derivadas (borrador offline)', 'DRAFT', NULL, NULL, now() FROM users u, courses c WHERE u.email = 'estudiante@est.una.ac.cr' AND c.join_code = 'GLF-CALC1';

-- Página del apunte: regiones y métricas de calidad en JSONB (bbox normalizado 0..1)
INSERT INTO note_pages (note_id, page_index, perceptual_hash, storage_uri, level_reached, overall_confidence, quality_metrics, regions, processed_at) SELECT n.id, 0, 'c3a1f0e98b7d6245', 'pages/demo/0.jpg', 'N2', 0.70, '{"blurVariance": 142.7, "brightness": 0.61, "glareRatio": 0.03, "skewAngleDeg": 1.8, "isUsable": true, "rejectionReason": null}', '[{"id": "r1", "bbox": [0.08, 0.12, 0.92, 0.19], "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.96, "isUncertain": false}, {"id": "r2", "bbox": [0.08, 0.20, 0.92, 0.26], "kind": "TEXT", "resolvedAt": "N2", "confidence": 0.74, "isUncertain": false}, {"id": "r3", "bbox": [0.14, 0.34, 0.63, 0.43], "kind": "MATH", "resolvedAt": "N1_5", "confidence": 0.88, "isUncertain": false, "engine": "SimpleTex"}, {"id": "r4", "bbox": [0.12, 0.82, 0.88, 0.90], "kind": "TEXT", "resolvedAt": "UNRESOLVED", "confidence": 0.21, "isUncertain": true, "reason": "GLARE"}]', now() FROM notes n WHERE n.title = 'Límites: definición épsilon-delta';

-- Ítems de estudio: tres formas distintas en la misma columna payload
INSERT INTO study_items (course_id, syllabus_topic_id, kind, payload, created_at) SELECT c.id, t.id, 'FLASHCARD', '{"schemaVersion": 1, "kind": "FLASHCARD", "front": "Definición épsilon-delta del límite", "back": "Para todo ε > 0 existe δ > 0 tal que 0 < |x - a| < δ implica |f(x) - L| < ε", "latex": "\\forall \\varepsilon > 0\\ \\exists \\delta > 0", "difficulty": "MEDIUM"}', now() FROM courses c JOIN syllabus_topics t ON t.course_id = c.id AND t.code = 'U1.1' WHERE c.join_code = 'GLF-CALC1';
INSERT INTO study_items (course_id, syllabus_topic_id, kind, payload, created_at) SELECT c.id, t.id, 'MULTIPLE_CHOICE', '{"schemaVersion": 1, "kind": "MULTIPLE_CHOICE", "stem": "¿Qué condición garantiza que una función continua alcance un máximo en S?", "options": [{"id": "a", "text": "S abierto y acotado", "isCorrect": false}, {"id": "b", "text": "S cerrado y acotado", "isCorrect": true}, {"id": "c", "text": "S conexo", "isCorrect": false}], "explanation": "Teorema del valor extremo: continuidad sobre un compacto.", "shuffleOptions": true, "difficulty": "MEDIUM"}', now() FROM courses c JOIN syllabus_topics t ON t.course_id = c.id AND t.code = 'U1.2' WHERE c.join_code = 'GLF-CALC1';
INSERT INTO study_items (course_id, syllabus_topic_id, kind, payload, created_at) SELECT c.id, t.id, 'TRUE_FALSE', '{"schemaVersion": 1, "kind": "TRUE_FALSE", "statement": "Toda sucesión convergente en R es acotada.", "answer": true, "explanation": "Casi todos los términos quedan cerca del límite y el resto es finito.", "difficulty": "EASY"}', now() FROM courses c JOIN syllabus_topics t ON t.course_id = c.id AND t.code = 'U1.1' WHERE c.join_code = 'GLF-CALC1';

-- Intentos del estudiante (response en JSONB) y su calendario de repaso SM-2
INSERT INTO attempts (user_id, study_item_id, response, is_correct, answered_at) SELECT u.id, s.id, '{"schemaVersion": 1, "kind": "MULTIPLE_CHOICE", "selectedOptionId": "b", "elapsedMs": 8420}', true, now() FROM users u, study_items s WHERE u.email = 'estudiante@est.una.ac.cr' AND s.kind = 'MULTIPLE_CHOICE';
INSERT INTO attempts (user_id, study_item_id, response, is_correct, answered_at) SELECT u.id, s.id, '{"schemaVersion": 1, "kind": "FLASHCARD", "selfRating": "HARD", "elapsedMs": 12050}', false, now() FROM users u, study_items s WHERE u.email = 'estudiante@est.una.ac.cr' AND s.kind = 'FLASHCARD';
INSERT INTO review_schedule (user_id, study_item_id, due_at, interval_days, ease) SELECT u.id, s.id, now() + INTERVAL '1 day', 1, 2.36 FROM users u, study_items s WHERE u.email = 'estudiante@est.una.ac.cr' AND s.kind = 'FLASHCARD';

-- Cobertura por tema: relacional, porque se filtra y agrega en cada pantalla
INSERT INTO topic_coverage (user_id, syllabus_topic_id, state, score, updated_at) SELECT u.id, t.id, 'SOLID', 0.86, now() FROM users u, syllabus_topics t WHERE u.email = 'estudiante@est.una.ac.cr' AND t.code = 'U1.1';
INSERT INTO topic_coverage (user_id, syllabus_topic_id, state, score, updated_at) SELECT u.id, t.id, 'PARTIAL', 0.45, now() FROM users u, syllabus_topics t WHERE u.email = 'estudiante@est.una.ac.cr' AND t.code = 'U1.2';
INSERT INTO topic_coverage (user_id, syllabus_topic_id, state, score, updated_at) SELECT u.id, t.id, 'ABSENT', 0.0, now() FROM users u, syllabus_topics t WHERE u.email = 'estudiante@est.una.ac.cr' AND t.code = 'U2';

-- Telemetría de IA: la reparación N2 de la página demo
INSERT INTO ai_calls (user_id, course_id, call_type, level, input_tokens, output_tokens, estimated_cost, latency_ms, created_at) SELECT u.id, c.id, 'IA_00', 'N2', 1850, 120, 0.000350, 2140, now() FROM users u, courses c WHERE u.email = 'estudiante@est.una.ac.cr' AND c.join_code = 'GLF-CALC1';
