CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- === Roles ===
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- === Users ===
CREATE TABLE users (
    id UUID PRIMARY KEY,
    login VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    patronymic VARCHAR(100),
    phone_number VARCHAR(20)
);

-- === User Roles ===
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- === Departments ===
CREATE TABLE department (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    chief_id UUID NOT NULL,
    FOREIGN KEY (chief_id) REFERENCES users(id)
);

-- === Department Users ===
CREATE TABLE department_users (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    department_id UUID NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE CASCADE
);

-- === Department Folders ===
CREATE TABLE department_folders (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_folder_id UUID,
    created_by VARCHAR(100) NOT NULL,
    department_id UUID NOT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NULL,
    FOREIGN KEY (parent_folder_id) REFERENCES department_folders(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE CASCADE
);

-- === Department Files ===
CREATE TABLE department_files (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    folder_id UUID,
    created_by VARCHAR(100) NOT NULL,
    department_id UUID NOT NULL,
    created_at TIMESTAMP,
    file_data BYTEA,
    file_type VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (folder_id) REFERENCES department_folders(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE CASCADE
);

-- === Department Folder Access ===
CREATE TABLE department_folder_access (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    folder_id UUID NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES department_folders(id) ON DELETE CASCADE
);

-- === User Folders ===
CREATE TABLE user_folders (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_folder_id UUID,
    user_id UUID NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    FOREIGN KEY (parent_folder_id) REFERENCES user_folders(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- === User Files ===
CREATE TABLE user_files (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    folder_id UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMP,
    file_data BYTEA,
    file_type VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (folder_id) REFERENCES user_folders(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- === Chat ===
CREATE TABLE chat (
    id UUID PRIMARY KEY,
    is_group BOOLEAN DEFAULT FALSE,
    name VARCHAR(255),
    created_at TIMESTAMP
);

-- === Chat Members ===
CREATE TABLE chat_members (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    user_id UUID NOT NULL,
    FOREIGN KEY (chat_id) REFERENCES chat(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- === Chat Messages ===
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    message TEXT,
    sent_at TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES chat(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id)
);

-- === Chat Message Attachments ===
CREATE TABLE chat_message_attachments (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(100),
    file_data BYTEA,
    FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE
);

-- Индексы для чатов
CREATE INDEX idx_chat_members_user_id ON chat_members(user_id);
CREATE INDEX idx_chat_messages_chat_id ON chat_messages(chat_id);
CREATE INDEX idx_chat_messages_sender_id ON chat_messages(sender_id);

-- Индексы для департаментов
CREATE INDEX idx_department_users_user_id ON department_users(user_id);
CREATE INDEX idx_department_users_department_id ON department_users(department_id);
CREATE INDEX idx_department_folder_access_user_id ON department_folder_access(user_id);
CREATE INDEX idx_department_folder_access_folder_id ON department_folder_access(folder_id);

-- Индексы для пользовательских файлов
CREATE INDEX idx_user_files_folder_id ON user_files(folder_id);
CREATE INDEX idx_user_folders_user_id ON user_folders(user_id);

INSERT INTO roles (id, name) VALUES
  (gen_random_uuid(), 'ADMIN'),
  (gen_random_uuid(), 'DEPARTMENT_HEAD'),
  (gen_random_uuid(), 'TEACHER');