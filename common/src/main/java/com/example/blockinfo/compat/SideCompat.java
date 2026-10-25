package com.example.blockinfo.compat;

/**
 * 判断当前是不是客户端。
 *
 * <p>不能用 {@code FMLEnvironment.dist} 或 {@code FMLEnvironment.getDist()} 直接写：
 * <ul>
 *   <li>1.21.1 ~ 1.21.8 只有字段 {@code dist}；</li>
 *   <li>1.21.9 起改成了方法 {@code getDist()}（26.x 同样是方法）。</li>
 * </ul>
 * 两种写法在另一条线上都编译不过，所以这里用反射各试一次，只取一次结果。
 * 反射只在类初始化时发生一次，之后都是读静态字段。</p>
 */
public final class SideCompat {
    private static final boolean CLIENT = detectClient();

    private SideCompat() {
    }

    public static boolean isClient() {
        return CLIENT;
    }

    private static boolean detectClient() {
        // 旧版：public static final Dist FMLEnvironment.dist
        try {
            Object dist = Class.forName("net.neoforged.fml.loading.FMLEnvironment")
                    .getField("dist")
                    .get(null);
            return "CLIENT".equals(String.valueOf(dist));
        } catch (Throwable ignored) {
            // 没有该字段，继续尝试方法
        }
        // 新版：public static Dist getDist()
        try {
            Object dist = Class.forName("net.neoforged.fml.loading.FMLEnvironment")
                    .getMethod("getDist")
                    .invoke(null);
            return "CLIENT".equals(String.valueOf(dist));
        } catch (Throwable ignored) {
            // 都拿不到时按服务端处理：不注册客户端相关的东西
        }
        return false;
    }
}
