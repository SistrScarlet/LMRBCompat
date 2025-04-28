package net.sistr.lmrbcompat.reflection;

import java.lang.reflect.Method;
import java.util.Optional;

public class ReflectionUtil {

    public static Optional<Object> execWithInstancing(String className, String methodName, Object... args) {
        try {
            var sampleClass = Class.forName(className);
            var instance = sampleClass.getConstructor().newInstance();
            var method = sampleClass.getMethod(methodName);
            return Optional.ofNullable(method.invoke(instance, args));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<Object> execSimple(Object instance, String methodName) {
        try {
            var method = instance.getClass().getMethod(methodName);
            return Optional.ofNullable(method.invoke(instance));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<IMethod> exec(Object instance, String methodName, Class<?>... parameterTypes) {
        try {
            var method = instance.getClass().getMethod(methodName, parameterTypes);
            return Optional.of(args1 -> {
                try {
                    return Optional.ofNullable(method.invoke(instance, args1));
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

    public static Optional<Object> execStaticSimple(String className, String methodName) {
        try {
            // クラスの取得
            Class<?> targetClass = Class.forName(className);

            // メソッドの取得
            Method method = targetClass.getMethod(methodName);

            // staticメソッドの呼び出し（第一引数にnullを指定）
            return Optional.ofNullable(method.invoke(null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<IMethod> execStatic(String className, String methodName, Class<?>... paramTypes) {
        try {
            // クラスの取得
            Class<?> targetClass = Class.forName(className);

            // メソッドの取得
            Method method = targetClass.getMethod(methodName, paramTypes);

            // staticメソッドの呼び出し（第一引数にnullを指定）
            return Optional.of(args -> {
                try {
                    return Optional.ofNullable(method.invoke(null, args));
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

    public interface IMethod {
        Optional<Object> exec(Object... args);
    }

    public static Optional<IConstructor> getConstructor(String className, Class<?>... paramTypes) {
        try {
            var clazz = Class.forName(className);
            var constructor = clazz.getConstructor(paramTypes);
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

    public static Optional<Object> getStaticField(String className, String fieldName) {
        try {
            return getStaticField(Class.forName(className), fieldName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<Object> getStaticField(Object instance, String fieldName) {
        try {
            return getStaticField(instance.getClass(), fieldName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<Object> getStaticField(Class<?> targetClass, String fieldName) {
        try {
            var field = targetClass.getField(fieldName);
            return Optional.ofNullable(field.get(null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<Object> getField(Object o, String fieldName) {
        try {
            var field = o.getClass().getField(fieldName);
            return Optional.ofNullable(field.get(o));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static boolean isClassExist(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
