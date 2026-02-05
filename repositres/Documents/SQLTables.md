# SQL数据表

### *admin_users*
```sql
CREATE TABLE admin_users
(
    id int(10) primary key NOT NULL AUTO_INCREMENT,
    username varchar(30) NOT NULL,
    password varchar(255) NOT NULL,
    UNIQUE (username)
);
```
### *judge_users*
```sql
CREATE TABLE judge_users
(
    id int(10) primary key NOT NULL AUTO_INCREMENT,
    username varchar(64) NOT NULL,
    password varchar(255) NULL,
    UNIQUE (username)
);
```

### *teacher_list*
```sql
CREATE TABLE teacher_list (
    name VARCHAR(50),
    organization VARCHAR(50),
    gender VARCHAR(5),
    education VARCHAR(50),
    telephone VARCHAR(50),
    email VARCHAR(50),
    jointime VARCHAR(30),
    id INT AUTO_INCREMENT,
    PRIMARY KEY (id),
    UNIQUE KEY name (name)
);
```

### *student_list*
```sql
CREATE TABLE student_list (
name VARCHAR(100),
class_name VARCHAR(100),
gender VARCHAR(10),
student_number VARCHAR(50),
telephone VARCHAR(20),
email VARCHAR(100),
admission_date DATE,
id INT AUTO_INCREMENT,
PRIMARY KEY (id),
UNIQUE KEY student_number (student_number)
);
```