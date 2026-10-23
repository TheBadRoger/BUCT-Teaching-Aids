CREATE TABLE note_likes
(
    note_id    BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT pk_note_likes PRIMARY KEY (note_id, student_id)
);

CREATE TABLE notes
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    content       TEXT                  NOT NULL,
    html_content  TEXT                  NULL,
    title         VARCHAR(255)          NOT NULL,
    is_public     BIT(1)                NULL,
    like_count    INT                   NULL,
    comment_count INT                   NULL,
    student_id    BIGINT                NOT NULL,
    course_id     BIGINT                NOT NULL,
    created_at    datetime              NULL,
    updated_at    datetime              NULL,
    CONSTRAINT pk_notes PRIMARY KEY (id)
);

ALTER TABLE notes
    ADD CONSTRAINT FK_NOTES_ON_COURSE FOREIGN KEY (course_id) REFERENCES course_list (id);

ALTER TABLE notes
    ADD CONSTRAINT FK_NOTES_ON_STUDENT FOREIGN KEY (student_id) REFERENCES student_list (id);

ALTER TABLE note_likes
    ADD CONSTRAINT fk_notlik_on_note FOREIGN KEY (note_id) REFERENCES notes (id);

ALTER TABLE note_likes
    ADD CONSTRAINT fk_notlik_on_student FOREIGN KEY (student_id) REFERENCES student_list (id);