# 类：CallBackContainer\<T>

> 把消息封装好返回给前端的容器
* 声明与实现：***com.buctta.api.utils::\utils\CallBackContainer.java***
* 数据库原型表：***admin_users***
  
## 成员变量

### *private T **data***
* 消息数据

### *private String **code***
* 消息代码

### *private String **msg***
* 消息内容

## 成员函数

### *public **CallBackContainer()***
* 构造函数。创建一个空的消息容器

### *public **CallBackContainer(T data)***
* 构造函数。创建一个 ***data***为data的消息容器

### *public **CallBackContainer(String code, String msg, T data)***
* 构造函数。创建一个 ***data***为data，***code***为code、***msg***为msg的消息容器

### *public T **getData()***
* 返回 ***data***

### *public String **getCode()***
* 返回 ***code***


### *public String **getMsg()***
* 返回 ***msg***

### *public void **setData(T data)***
* 修改 ***data***

### *public void **setCode(String code)***
* 修改 ***code***

### *public void **setMsg(String msg)***
* 作用：修改 ***msg***

### *public static **CallBackContainer Succeed()***
* 返回一个空消息容器

### *public static \<T> **CallBackContainer\<T> Succeed(T data)***
* 返回一个 ***data*** 为data，其余值为空的消息容器

### *public static \<T> **CallBackContainer\<T>Succeed(T data, String msg)***
* 返回一个 ***data*** 为data，***msg*** 为msg，其余值为空的消息容器

### *public static **CallBackContainer Failed(String code,String msg)***
* 返回一个 ***code***为code、***msg***为msg的消息容器