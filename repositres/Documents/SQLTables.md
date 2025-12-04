# SQL数据表

### *admin_users*

```
CREATE TABLE admin_users
(
    id int(10) primary key NOT NULL AUTO_INCREMENT,
    username varchar(30) NOT NULL,
    password varchar(255) NOT NULL,
    UNIQUE (username)
);
```
### *teacher_list*
```
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