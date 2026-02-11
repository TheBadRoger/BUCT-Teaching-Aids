CREATE TABLE IF NOT EXISTS admin_users
(
    id       int(10) primary key NOT NULL AUTO_INCREMENT,
    username varchar(30)         NOT NULL,
    password varchar(255)        NOT NULL,
    UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS judge_users
(
    id       int AUTO_INCREMENT PRIMARY KEY,
    username varchar(64)  NOT NULL,
    password varchar(256) NULL,
    constraint username
        UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS teacher_list
(
    name         VARCHAR(50),
    organization VARCHAR(50),
    gender       VARCHAR(5),
    education    VARCHAR(50),
    jointime     VARCHAR(30),
    id           INT AUTO_INCREMENT,
    PRIMARY KEY (id),
    UNIQUE KEY name (name)
);

CREATE TABLE IF NOT EXISTS student_list
(
    name           VARCHAR(100),
    class_name     VARCHAR(100),
    gender         VARCHAR(10),
    student_number VARCHAR(50),
    admission_date DATE,
    id             INT AUTO_INCREMENT,
    PRIMARY KEY (id),
    UNIQUE KEY student_number (student_number)
);

CREATE TABLE IF NOT EXISTS course_list
(
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    course_number       VARCHAR(50) UNIQUE NOT NULL,
    course_name         VARCHAR(200)       NOT NULL,
    course_introduction TEXT,
    start_date          varchar(50),
    duration            varchar(50),
    teaching_teachers   text,
    teaching_classes    text,
    target_audience     text,
    course_status       varchar(50),
    class_address       VARCHAR(255),
    course_price        DECIMAL(10, 2)              DEFAULT 0.00,
    course_tags         text,
    teaching_objectives text,
    course_outline      longtext,
    course_image        VARCHAR(500),
    view_count          INT                NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_list
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    telephone  VARCHAR(20),
    email      VARCHAR(100),
    password   VARCHAR(255) NOT NULL,
    user_type  VARCHAR(20)  NOT NULL,
    teacher_id INT UNIQUE,
    student_id INT UNIQUE,
    CONSTRAINT fk_user_teacher FOREIGN KEY (teacher_id) REFERENCES teacher_list (id),
    CONSTRAINT fk_user_student FOREIGN KEY (student_id) REFERENCES student_list (id)
);

CREATE TABLE IF NOT EXISTS student_course
(
    student_id INT     NOT NULL,
    course_id  INT     NOT NULL,
    is_viewed  BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (student_id, course_id),
    CONSTRAINT fk_student_course_student
        FOREIGN KEY (student_id) REFERENCES student_list (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    CONSTRAINT fk_student_course_course
        FOREIGN KEY (course_id) REFERENCES course_list (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE
);

CREATE INDEX idx_student_course_student_id ON student_course (student_id);
CREATE INDEX idx_student_course_course_id ON student_course (course_id);

