/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.core.util.reflection;

import org.apiguardian.api.API;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

/**
 * Reflection utilities.
 * <p>
 * Minecraft 26.x is distributed unobfuscated, so the intermediary/Quilt mapping resolution of the original
 * implementation is not needed anymore: names are looked up directly.
 */
// TODO add variants that return null instead of throwing (if (dothrow) throw e ?)
public class ReflectionHelper {

    @API(status = MAINTAINED, since = "1.0.0")
    public static boolean isDevEnv() {
        return Boolean.getBoolean("requiem.dev") || System.getProperty("fml.devMode") != null;
    }

    @SuppressWarnings("unchecked")
    @API(status = STABLE, since = "1.0.0")
    public static <T> Class<T> findClass(String className) throws ClassNotFoundException {
        return (Class<T>) Class.forName(className);
    }

    /**
     * Finds a method with the specified name and parameters in the given class and returns an accessible {@link Method} for it.
     *
     * @param owner          The class to find the method on.
     * @param name           The name of the method to find (Mojang name).
     * @param returnType     The return type of the method to find (unused, kept for API compatibility).
     * @param parameterTypes The parameter types of the method to find.
     * @throws UnableToFindMethodException if an issue prevents the method from being reflected
     * @apiNote for performance, store the returned value and avoid calling this repeatedly.
     */
    @API(status = MAINTAINED, since = "1.0.0")
    public static Method findMethodFromIntermediary(Class<?> owner, String name, Class<?> returnType, Class<?>... parameterTypes) throws UnableToFindMethodException {
        try {
            Method m = owner.getDeclaredMethod(name, parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new UnableToFindMethodException(e);
        }
    }

    /**
     * Finds a field with the specified name in the given class and returns an accessible {@link Field} object representing it.
     *
     * @param owner The class to find the field on.
     * @param name  The name of the field to find (Mojang name).
     * @param type  The type of the field to find (unused, kept for API compatibility).
     * @throws UnableToFindFieldException if an issue prevents the field from being reflected
     * @apiNote for performance, store the returned value and avoid calling this repeatedly.
     */
    @API(status = MAINTAINED, since = "1.0.0")
    public static Field findFieldFromIntermediary(Class<?> owner, String name, Class<?> type) throws UnableToFindFieldException {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new UnableToFindFieldException(e);
        }
    }

    public static <T> void setField(Field f, T value) throws IllegalAccessException {
        f.setAccessible(true);
        f.set(null, value);
    }

    /**
     * Creates a factory for the given class implementing the given <tt>lambdaType</tt>.
     * The constructor of the class will be looked up using the passed in <tt>lookup</tt> object.
     *
     * @param clazz         Class for which to create a factory
     * @param invokedName   Name of the method to be implemented in the functional interface
     * @param lambdaType    Class of a functional interface to be implemented by the factory
     * @param lookup        Lookup to be used to find the constructor
     * @param samMethodType Signature and return type of method to be implemented by the function object.
     * @return a factory implementing <tt>lambdaType</tt>
     */
    @API(status = MAINTAINED, since = "1.0.0")
    @SuppressWarnings("unchecked")
    public static <T> T createFactory(
            Class<?> clazz,
            String invokedName,
            Class<? super T> lambdaType,
            MethodHandles.Lookup lookup,
            MethodType samMethodType,
            Class<?>... cnstParams
    ) {
        try {
            MethodHandle handle = lookup.findConstructor(clazz, MethodType.methodType(void.class, cnstParams));
            CallSite metafactory = LambdaMetafactory.metafactory(
                    lookup,
                    invokedName,
                    MethodType.methodType(lambdaType),
                    samMethodType,
                    handle,
                    MethodType.methodType(clazz, cnstParams)
            );
            return (T) metafactory.getTarget().invoke();
        } catch (Throwable throwable) {
            throw new UncheckedReflectionException(throwable);
        }
    }
}