# 类：AdminUser

* 声明与实现：***com.buctta.api.entities::\entities\AdminUser.java***
* 数据库原型表：***admin_users***
  
## 成员变量

### *private long **id***
* 数据库原型： ***admin_users:id***
  * 属性：***AUTO_INCREAMENT***，***PRIMARY***

### *private String **name***
* 数据库原型： ***admin_users:id***
  * 属性：***UNIQUE***

### *private String **password***
* 数据库原型： ***admin_users:id***

## 成员函数

### *public String **getId()***
* 返回 ***id***
  
### *public String **getUsername()***
* 返回 ***name***

### *public String **getPassword()***
* 返回 ***password***


### *public String **setId()***
* 修改 ***id***
  
### *public String **setUsername()***
* 修改 ***name***

### *public String **setPassword()***
* 修改 ***password***