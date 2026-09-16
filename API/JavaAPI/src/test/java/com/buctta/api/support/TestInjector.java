package com.buctta.api.support;

import com.buctta.api.config.MediaProperties;
import com.buctta.api.serviceimp.IMPL_MediaStorageService;

import java.lang.reflect.Field;

/**
 * 测试辅助：手工完成 {@code @Resource} 字段注入，避免为了单测拉起整个 Spring 容器
 * （本项目集成测试基类依赖 Testcontainers/Docker，纯逻辑单测不应受此限制）。
 */
public final class TestInjector {

    /** 测试用的固定随机种子无关文件内容长度，避免各测试重复定义 */
    public static final int TINY = 4;

    private TestInjector() {
    }

    /** 逐个字段显式注入，顺序与目标类中 {@code @Resource} 字段声明顺序无关，但由调用方保证语义正确 */
    public static void injectOrdered(Object target, Object... dependencies) {
        for (Object dependency : dependencies) {
            Field field = findFieldFor(target.getClass(), dependency.getClass());
            if (field == null) {
                throw new IllegalStateException("找不到可注入字段: " + dependency.getClass()
                        .getSimpleName() + " @ " + target.getClass().getSimpleName());
            }
            field.setAccessible(true);
            try {
                field.set(target, dependency);
            }
            catch (IllegalAccessException e) {
                throw new IllegalStateException("注入失败: " + field.getName(), e);
            }
        }
    }

    /**
     * 匹配注入字段：优先按名字（fake 类名去掉 {@code Fake} 前缀后首字母小写），
     * 使 fake 实例能注入到 {@code @Resource} 声明的具体仓储字段上；
     * 名字匹配不到时再退化为按声明的接口类型匹配。
     */
    private static Field findFieldFor(Class<?> targetType, Class<?> dependencyType) {
        String simple = dependencyType.getSimpleName();
        String byName = simple.startsWith("Fake")
                ? Character.toLowerCase(simple.charAt("Fake".length()))
                + simple.substring("Fake".length() + 1)
                : Character.toLowerCase(simple.charAt(0)) + simple.substring(1);

        try {
            return targetType.getDeclaredField(byName);
        }
        catch (NoSuchFieldException ignored) {
            // 落到类型匹配
        }

        Class<?> current = targetType;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().isAssignableFrom(dependencyType)) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /** 构造指向临时目录的媒体配置 */
    public static MediaProperties mediaProperties(java.nio.file.Path tempDir) {
        MediaProperties properties = new MediaProperties();
        properties.setRoot(tempDir.toString());
        properties.setStorage("media");
        return properties;
    }

    /** 构造已注入配置的存储服务 */
    public static IMPL_MediaStorageService storageService(MediaProperties properties) {
        IMPL_MediaStorageService service = new IMPL_MediaStorageService();
        injectOrdered(service, properties);
        return service;
    }

    /**
     * 校验 fake 类名到目标字段名的映射，便于在测试启动前暴露命名不一致问题。
     * 约定：fake 类名去掉 {@code Fake} 前缀后首字母小写即为目标字段名。
     */
    public static String fieldNameOf(Class<?> fakeType) {
        String simple = fakeType.getSimpleName();
        if (!simple.startsWith("Fake")) {
            throw new IllegalArgumentException("fake 类名必须以 Fake 开头: " + simple);
        }
        return Character.toLowerCase(simple.charAt("Fake".length()))
                + simple.substring("Fake".length() + 1);
    }
}
