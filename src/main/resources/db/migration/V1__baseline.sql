-- Access Control
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    avatar_url VARCHAR(512),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE privileges (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_privileges (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    privilege_id BIGINT NOT NULL REFERENCES privileges(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, privilege_id)
);

-- Courses
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    term VARCHAR(20) NOT NULL,
    join_code VARCHAR(10) NOT NULL UNIQUE,
    syllabus_source_uri VARCHAR(512),
    syllabus_parsed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PENDING', 'REMOVED')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, course_id)
);

CREATE TABLE syllabus_topics (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES syllabus_topics(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    order_index INTEGER NOT NULL
);

CREATE TABLE course_glossary (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    term VARCHAR(120) NOT NULL,
    canonical_form VARCHAR(255) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('TERM', 'SYMBOL', 'NOTATION'))
);

-- Glossary Suggestions & Notes
CREATE TABLE glossary_suggestions (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    note_id BIGINT,
    original_text VARCHAR(255) NOT NULL,
    suggested_correction VARCHAR(255) NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE notes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    syllabus_topic_id BIGINT REFERENCES syllabus_topics(id) ON DELETE SET NULL,
    class_date DATE NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PROCESSING', 'READY', 'ARCHIVED')),
    content JSONB,
    content_generated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE glossary_suggestions ADD CONSTRAINT fk_glossary_suggestions_note FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE SET NULL;

CREATE TABLE note_pages (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    page_index INTEGER NOT NULL,
    perceptual_hash CHAR(16) NOT NULL,
    storage_uri VARCHAR(512) NOT NULL,
    level_reached VARCHAR(12) NOT NULL DEFAULT 'N0' CHECK (level_reached IN ('N0', 'N1', 'N1_5', 'N2', 'N3')),
    overall_confidence REAL NOT NULL DEFAULT 0.0,
    quality_metrics JSONB,
    regions JSONB,
    processed_at TIMESTAMPTZ,
    UNIQUE(note_id, page_index)
);

CREATE TABLE topic_coverage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    syllabus_topic_id BIGINT NOT NULL REFERENCES syllabus_topics(id) ON DELETE CASCADE,
    state VARCHAR(12) NOT NULL DEFAULT 'UNSEEN' CHECK (state IN ('UNSEEN', 'SEEN', 'PARTIAL', 'COVERED')),
    score REAL NOT NULL DEFAULT 0.0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, syllabus_topic_id)
);

CREATE TABLE study_items (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    syllabus_topic_id BIGINT NOT NULL REFERENCES syllabus_topics(id) ON DELETE CASCADE,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('FLASHCARD', 'MULTIPLE_CHOICE', 'TRUE_FALSE')),
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE study_items ADD CONSTRAINT study_items_kind_matches_payload CHECK (payload ->> 'kind' = kind);

CREATE TABLE attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    study_item_id BIGINT NOT NULL REFERENCES study_items(id) ON DELETE CASCADE,
    response JSONB NOT NULL,
    is_correct BOOLEAN NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE review_schedule (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    study_item_id BIGINT NOT NULL REFERENCES study_items(id) ON DELETE CASCADE,
    due_at TIMESTAMPTZ NOT NULL,
    interval_days INTEGER NOT NULL DEFAULT 1,
    ease REAL NOT NULL DEFAULT 2.5,
    UNIQUE(user_id, study_item_id)
);

CREATE TABLE ai_calls (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT REFERENCES courses(id) ON DELETE SET NULL,
    call_type VARCHAR(16) NOT NULL CHECK (call_type IN ('OCR_M', 'IA_00', 'IA_01', 'IA_02', 'IA_03', 'IA_04')),
    level VARCHAR(12) NOT NULL CHECK (level IN ('N1', 'N1_5', 'N2', 'N3', 'BATCH', 'ON_DEMAND')),
    input_tokens INTEGER NOT NULL,
    output_tokens INTEGER NOT NULL,
    estimated_cost NUMERIC(10,6) NOT NULL,
    latency_ms INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token VARCHAR(255) NOT NULL UNIQUE,
    platform VARCHAR(12) NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sync_queue (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    entity_type VARCHAR(40) NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    payload JSONB NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('SYNC_COMPLETE', 'NOTE_READY', 'STUDY_REMINDER', 'SYSTEM')),
    payload JSONB NOT NULL,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ
);

CREATE TABLE coverage_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    coverage_pct REAL NOT NULL,
    taken_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_courses_owner ON courses(owner_user_id);
CREATE INDEX idx_enrollments_user ON enrollments(user_id);
CREATE INDEX idx_syllabus_topics_course ON syllabus_topics(course_id, parent_id, order_index);
CREATE INDEX idx_notes_user_course ON notes(user_id, course_id);
CREATE INDEX idx_notes_status ON notes(status);
CREATE INDEX idx_note_pages_note ON note_pages(note_id, page_index);
CREATE INDEX idx_study_items_course_topic ON study_items(course_id, syllabus_topic_id);
CREATE INDEX idx_attempts_user ON attempts(user_id, study_item_id);
CREATE INDEX idx_topic_coverage_user ON topic_coverage(user_id);
CREATE INDEX idx_ai_calls_user ON ai_calls(user_id, created_at);
CREATE INDEX idx_sync_queue_status ON sync_queue(status, created_at);
CREATE INDEX idx_glossary_suggestions_course ON glossary_suggestions(course_id, status);

-- Seed Roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_STUDENT', 'Default student role'),
    ('ROLE_TEACHER', 'Course owner and syllabus publisher'),
    ('ROLE_ADMIN', 'System administrator');

-- Seed Privileges
INSERT INTO privileges (name, description) VALUES
    ('NOTE_READ_OWN', 'Read own notes'),
    ('NOTE_WRITE_OWN', 'Create and edit own notes'),
    ('NOTE_DELETE_OWN', 'Delete own notes'),
    ('COURSE_READ', 'View courses'),
    ('COURSE_WRITE', 'Create and manage courses'),
    ('SYLLABUS_PUBLISH', 'Publish syllabus for owned courses'),
    ('GLOSSARY_WRITE', 'Manage course glossary'),
    ('COVERAGE_READ_OWN', 'View own topic coverage'),
    ('COVERAGE_READ_COURSE', 'View course-wide coverage stats'),
    ('STUDY_ITEM_GENERATE', 'Generate study items from notes'),
    ('STUDY_ATTEMPT_WRITE', 'Submit study attempts'),
    ('AI_VISION_INVOKE', 'Invoke AI vision services'),
    ('USAGE_READ_OWN', 'View own AI usage stats'),
    ('USAGE_READ_COURSE', 'View course AI usage stats'),
    ('USER_MANAGE', 'Manage users'),
    ('ROLE_MANAGE', 'Manage roles and privileges');

-- Map privileges to roles
-- Student privileges
INSERT INTO role_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r, privileges p
WHERE r.name = 'ROLE_STUDENT' AND p.name IN (
    'NOTE_READ_OWN', 'NOTE_WRITE_OWN', 'NOTE_DELETE_OWN',
    'COURSE_READ', 'COVERAGE_READ_OWN',
    'STUDY_ITEM_GENERATE', 'STUDY_ATTEMPT_WRITE',
    'AI_VISION_INVOKE', 'USAGE_READ_OWN'
);

-- Teacher privileges (student + extras)
INSERT INTO role_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r, privileges p
WHERE r.name = 'ROLE_TEACHER' AND p.name IN (
    'NOTE_READ_OWN', 'NOTE_WRITE_OWN', 'NOTE_DELETE_OWN',
    'COURSE_READ', 'COURSE_WRITE',
    'SYLLABUS_PUBLISH', 'GLOSSARY_WRITE',
    'COVERAGE_READ_OWN', 'COVERAGE_READ_COURSE',
    'STUDY_ITEM_GENERATE', 'STUDY_ATTEMPT_WRITE',
    'AI_VISION_INVOKE', 'USAGE_READ_OWN', 'USAGE_READ_COURSE'
);

-- Admin privileges
INSERT INTO role_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r, privileges p
WHERE r.name = 'ROLE_ADMIN' AND p.name IN (
    'NOTE_READ_OWN', 'COURSE_READ', 'COURSE_WRITE',
    'COVERAGE_READ_COURSE', 'USAGE_READ_COURSE',
    'USER_MANAGE', 'ROLE_MANAGE'
);
