package net.sistr.lmrbcompat.reflection;

import java.lang.reflect.Method;
import java.util.Optional;

public class ReflectionUtil {

    public static void invoke(String className, String methodName, Object... args) {
        try {
            var sampleClass = Class.forName(className);
            var instance = sampleClass.getConstructor().newInstance();
            var method = sampleClass.getMethod(methodName);
            method.invoke(instance, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void invokeStatic(String className, String methodName, Object... args) {
        try {
            // クラスの取得
            Class<?> targetClass = Class.forName(className);

            // メソッドのパラメータ型を取得
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            // メソッドの取得
            Method method = targetClass.getMethod(methodName, paramTypes);

            // staticメソッドの呼び出し（第一引数にnullを指定）
            method.invoke(null, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Optional<IConstructor> getConstructor(String className, Class<?>... paramTypes) {
        try {
            var sampleClass = Class.forName(className);
            var constructor = sampleClass.getConstructor(paramTypes);
            return Optional.of((args) -> {
                try {
                    return Optional.of(constructor.newInstance(args));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return Optional.empty();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public interface IConstructor {
        Optional<Object> newInstance(Object... args);
    }

}
