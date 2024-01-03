package net.sistr.lmrbcompat.reflection;

public class ReflectionUtil {

    public static void invoke(String className, String methodName) {
        try {
            var sampleClass = Class.forName(className);
            var instance = sampleClass.getConstructor().newInstance();
            var method = sampleClass.getMethod(methodName);
            method.invoke(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
