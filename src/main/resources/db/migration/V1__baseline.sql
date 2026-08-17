-- Access Control
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
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

CREATE INDEX idx_courses_owner ON courses(owner_user_id);
CREATE INDEX idx_enrollments_user ON enrollments(user_id);
CREATE INDEX idx_syllabus_topics_course ON syllabus_topics(course_id, parent_id, order_index);

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
