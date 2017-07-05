/*
 * Copyright 1999-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.jmx.mbeanserver;

import com.sun.jmx.remote.util.EnvHelp;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.management.AttributeNotFoundException;
import javax.management.Description;

import javax.management.Descriptor;
import javax.management.DescriptorFields;
import javax.management.DescriptorKey;
import javax.management.DynamicMBean;
import javax.management.ImmutableDescriptor;
import javax.management.MBean;
import javax.management.MBeanInfo;
import javax.management.MXBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.MXBeanMappingFactory;

import static com.sun.jmx.defaults.JmxProperties.MBEANSERVER_LOGGER;
import com.sun.jmx.mbeanserver.Util;
import com.sun.jmx.remote.util.EnvHelp;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.AttributeNotFoundException;
import javax.management.JMX;
import javax.management.ObjectName;
import javax.management.ObjectNameTemplate;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.MXBeanMappingFactory;

/**
 * This class contains the methods for performing all the tests needed to verify
 * that a class represents a JMX compliant MBean.
 *
 * @since 1.5
 */
public class Introspector {

    /**
     * Pattern used to extract Attribute Names from ObjectNameTemplate Annotation
     * For example, in the following example, the Name attribute value is
     * retrieved : ":type=MyType, name={Name}"
     */
    private static Pattern OBJECT_NAME_PATTERN_TEMPLATE =
            Pattern.compile("(\\{[^\\}]+\\})|(=\"\\{[^\\}]+\\}\")");
     /*
     * ------------------------------------------
     *  PRIVATE CONSTRUCTORS
     * ------------------------------------------
     */

    // private constructor defined to "hide" the default public constructor
    private Introspector() {

        // ------------------------------
        // ------------------------------

    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    /**
     * Tell whether a MBean of the given class is a Dynamic MBean.
     * This method does nothing more than returning
     * <pre>
     * javax.management.DynamicMBean.class.isAssignableFrom(c)
     * </pre>
     * This method does not check for any JMX MBean compliance:
     * <ul><li>If <code>true</code> is returned, then instances of
     *     <code>c</code> are DynamicMBean.</li>
     *     <li>If <code>false</code> is returned, then no further
     *     assumption can be made on instances of <code>c</code>.
     *     In particular, instances of <code>c</code> may, or may not
     *     be JMX standard MBeans.</li>
     * </ul>
     * @param c The class of the MBean under examination.
     * @return <code>true</code> if instances of <code>c</code> are
     *         Dynamic MBeans, <code>false</code> otherwise.
     *
     **/
    public static final boolean isDynamic(final Class<?> c) {
        // Check if the MBean implements the DynamicMBean interface
        return javax.management.DynamicMBean.class.isAssignableFrom(c);
    }

    /**
     * Basic method for testing that a MBean of a given class can be
     * instantiated by the MBean server.<p>
     * This method checks that:
     * <ul><li>The given class is a concrete class.</li>
     *     <li>The given class exposes at least one public constructor.</li>
     * </ul>
     * If these conditions are not met, throws a NotCompliantMBeanException.
     * @param c The class of the MBean we want to create.
     * @exception NotCompliantMBeanException if the MBean class makes it
     *            impossible to instantiate the MBean from within the
     *            MBeanServer.
     *
     **/
    public static void testCreation(Class<?> c)
        throws NotCompliantMBeanException {
        // Check if the class is a concrete class
        final int mods = c.getModifiers();
        if (Modifier.isAbstract(mods) || Modifier.isInterface(mods)) {
            throw new NotCompliantMBeanException("MBean class must be concrete");
        }

        // Check if the MBean has a public constructor
        final Constructor<?>[] consList = c.getConstructors();
        if (consList.length == 0) {
            throw new NotCompliantMBeanException("MBean class must have public constructor");
        }
    }

    public static void checkCompliance(Class<?> mbeanClass)
    throws NotCompliantMBeanException {

        // Check that @Resource is used correctly (if it used).
        MBeanInjector.validate(mbeanClass);

        // Is DynamicMBean?
        //
        if (DynamicMBean.class.isAssignableFrom(mbeanClass))
            return;
        // Is Standard MBean?
        //
        final Exception mbeanException;
        try {
            getStandardMBeanInterface(mbeanClass);
            return;
        } catch (NotCompliantMBeanException e) {
            mbeanException = e;
        }
        // Is MXBean?
        //
        final Exception mxbeanException;
        try {
            getMXBeanInterface(mbeanClass);
            return;
        } catch (NotCompliantMBeanException e) {
            mxbeanException = e;
        }
        // Is @MBean or @MXBean class?
        // In fact we find @MBean or @MXBean as a hacky variant of
        // getStandardMBeanInterface or getMXBeanInterface.  If we get here
        // then nothing worked.
        final String msg =
            "MBean class " + mbeanClass.getName() + " does not implement " +
            "DynamicMBean; does not follow the Standard MBean conventions (" +
            mbeanException.toString() + "); does not follow the MXBean conventions (" +
            mxbeanException.toString() + "); and does not have or inherit the @" +
            MBean.class.getSimpleName() + " or @" + MXBean.class.getSimpleName() +
            " annotation";
        throw new NotCompliantMBeanException(msg);
    }

    /**
     * <p>Make a DynamicMBean out of the existing MBean object.  The object
     * may already be a DynamicMBean, or it may be a Standard MBean or
     * MXBean, possibly defined using {@code @MBean} or {@code @MXBean}.</p>
     * @param mbean the object to convert to a DynamicMBean.
     * @param <T> a type parameter defined for implementation convenience
     * (which would have to be removed if this method were part of the public
     * API).
     * @return the converted DynamicMBean.
     * @throws NotCompliantMBeanException if {@code mbean} is not a compliant
     * MBean object, including the case where it is null.
     */
    public static <T> DynamicMBean makeDynamicMBean(T mbean)
    throws NotCompliantMBeanException {
        if (mbean == null)
            throw new NotCompliantMBeanException("Null MBean object");
        if (mbean instanceof DynamicMBean)
            return (DynamicMBean) mbean;
        final Class<?> mbeanClass = mbean.getClass();
        Class<? super T> c = null;
        try {
            c = Util.cast(getStandardMBeanInterface(mbeanClass));
        } catch (NotCompliantMBeanException e) {
            // Ignore exception - we need to check whether
            // mbean is an MXBean first.
        }
        if (c != null)
            return new StandardMBeanSupport(mbean, c);

        try {
            c = Util.cast(getMXBeanInterface(mbeanClass));
        } catch (NotCompliantMBeanException e) {
            // Ignore exception - we cannot decide whether mbean was supposed
            // to be an MBean or an MXBean. We will call checkCompliance()
            // to generate the appropriate exception.
        }
        if (c != null) {
            MXBeanMappingFactory factory;
            try {
                factory = MXBeanMappingFactory.forInterface(c);
            } catch (IllegalArgumentException e) {
                NotCompliantMBeanException ncmbe =
                        new NotCompliantMBeanException(e.getMessage());
                ncmbe.initCause(e);
                throw ncmbe;
            }
            return new MXBeanSupport(mbean, c, factory);
        }
        checkCompliance(mbeanClass);
        throw new NotCompliantMBeanException("Not compliant"); // not reached
    }

    /**
     * Basic method for testing if a given class is a JMX compliant MBean.
     *
     * @param baseClass The class to be tested
     *
     * @return <code>null</code> if the MBean is a DynamicMBean,
     *         the computed {@link javax.management.MBeanInfo} otherwise.
     * @exception NotCompliantMBeanException The specified class is not a
     *            JMX compliant MBean
     */
    public static MBeanInfo testCompliance(Class<?> baseClass)
        throws NotCompliantMBeanException {

        // ------------------------------
        // ------------------------------

        // Check if the MBean implements the MBean or the Dynamic
        // MBean interface
        if (isDynamic(baseClass))
            return null;

        return testCompliance(baseClass, null);
    }

    public static void testComplianceMXBeanInterface(Class<?> interfaceClass,
                                                     MXBeanMappingFactory factory)
            throws NotCompliantMBeanException {
        MXBeanIntrospector.getInstance(factory).getAnalyzer(interfaceClass);
    }

    /**
     * Basic method for testing if a given class is a JMX compliant
     * Standard MBean.  This method is only called by the legacy code
     * in com.sun.management.jmx.
     *
     * @param baseClass The class to be tested.
     *
     * @param mbeanInterface the MBean interface that the class implements,
     * or null if the interface must be determined by introspection.
     *
     * @return the computed {@link javax.management.MBeanInfo}.
     * @exception NotCompliantMBeanException The specified class is not a
     *            JMX compliant Standard MBean
     */
    public static synchronized MBeanInfo
            testCompliance(final Class<?> baseClass,
                           Class<?> mbeanInterface)
            throws NotCompliantMBeanException {
        if (mbeanInterface == null)
            mbeanInterface = getStandardMBeanInterface(baseClass);
        MBeanIntrospector<?> introspector = StandardMBeanIntrospector.getInstance();
        return getClassMBeanInfo(introspector, baseClass, mbeanInterface);
    }

    private static <M> MBeanInfo
            getClassMBeanInfo(MBeanIntrospector<M> introspector,
                              Class<?> baseClass, Class<?> mbeanInterface)
    throws NotCompliantMBeanException {
        PerInterface<M> perInterface = introspector.getPerInterface(mbeanInterface);
        return introspector.getClassMBeanInfo(baseClass, perInterface);
    }

    /**
     * Get the MBean interface implemented by a JMX Standard
     * MBean class. This method is only called by the legacy
     * code in "com.sun.management.jmx".
     *
     * @param baseClass The class to be tested.
     *
     * @return The MBean interface implemented by the MBean.
     *         Return <code>null</code> if the MBean is a DynamicMBean,
     *         or if no MBean interface is found.
     */
    public static Class<?> getMBeanInterface(Class<?> baseClass) {
        // Check if the given class implements the MBean interface
        // or the Dynamic MBean interface
        if (isDynamic(baseClass)) return null;
        try {
            return getStandardMBeanInterface(baseClass);
        } catch (NotCompliantMBeanException e) {
            return null;
        }
    }

    /**
     * Get the MBean interface implemented by a JMX Standard MBean class.
     *
     * @param baseClass The class to be tested.
     *
     * @return The MBean interface implemented by the Standard MBean.
     *
     * @throws NotCompliantMBeanException The specified class is
     * not a JMX compliant Standard MBean.
     */
    public static <T> Class<? super T> getStandardMBeanInterface(Class<T> baseClass)
    throws NotCompliantMBeanException {
        if (baseClass.isAnnotationPresent(MBean.class))
            return baseClass;
        Class<? super T> current = baseClass;
        Class<? super T> mbeanInterface = null;
        while (current != null) {
            mbeanInterface =
                findMBeanInterface(current, current.getName());
            if (mbeanInterface != null) break;
            current = current.getSuperclass();
        }
        if (mbeanInterface != null) {
            return mbeanInterface;
        } else {
            final String msg =
                "Class " + baseClass.getName() +
                " is not a JMX compliant Standard MBean";
            throw new NotCompliantMBeanException(msg);
        }
    }

    /**
     * Get the MXBean interface implemented by a JMX MXBean class.
     *
     * @param baseClass The class to be tested.
     *
     * @return The MXBean interface implemented by the MXBean.
     *
     * @throws NotCompliantMBeanException The specified class is
     * not a JMX compliant MXBean.
     */
    public static <T> Class<? super T> getMXBeanInterface(Class<T> baseClass)
        throws NotCompliantMBeanException {
        if (hasMXBeanAnnotation(baseClass))
            return baseClass;
        try {
            return MXBeanSupport.findMXBeanInterface(baseClass);
        } catch (Exception e) {
            throw throwException(baseClass,e);
        }
    }

    public static <T> Class<? super T> getStandardOrMXBeanInterface(
            Class<T> baseClass, boolean mxbean)
    throws NotCompliantMBeanException {
        if (mxbean)
            return getMXBeanInterface(baseClass);
        else
            return getStandardMBeanInterface(baseClass);
    }

    public static ObjectName templateToObjectName(Descriptor descriptor,
            DynamicMBean mbean)
            throws NotCompliantMBeanException {
        String template = (String)
            descriptor.getFieldValue(JMX.OBJECT_NAME_TEMPLATE);
        if(template == null) return null;
        try {
            Matcher m = OBJECT_NAME_PATTERN_TEMPLATE.matcher(template);
            while (m.find()){
                String grp = m.group();
                System.out.println("GROUP " + grp);
                String attributeName = null;
                boolean quote = false;
                if(grp.startsWith("=\"{")) {
                    attributeName = grp.substring(3, grp.length() - 2);
                    quote = true;
                } else
                    attributeName = grp.substring(1, grp.length() - 1);

                Object attributeValue = mbean.getAttribute(attributeName);
                String validValue = quote ?
                    "=" + ObjectName.quote(attributeValue.toString()) :
                    attributeValue.toString();
                template = template.replace(grp, validValue);
            }
            return new ObjectName(template);
        }catch(Exception ex) {
            NotCompliantMBeanException ncex = new
                    NotCompliantMBeanException(ObjectNameTemplate.class.
                    getSimpleName() + " annotation value [" + template + "] " +
                    "is invalid. " + ex);
            ncex.initCause(ex);
            throw ncex;
        }
    }

    /*
     * ------------------------------------------
     *  PRIVATE METHODS
     * ------------------------------------------
     */

    static boolean hasMXBeanAnnotation(Class<?> c) {
        MXBean m = c.getAnnotation(MXBean.class);
        return (m != null && m.value());
    }

    /**
     * Try to find the MBean interface corresponding to the class aName
     * - i.e. <i>aName</i>MBean, from within aClass and its superclasses.
     **/
    private static <T> Class<? super T> findMBeanInterface(
            Class<T> aClass, String aName) {
        Class<? super T> current = aClass;
        while (current != null) {
            final Class<?>[] interfaces = current.getInterfaces();
            final int len = interfaces.length;
            for (int i=0;i<len;i++)  {
                Class<? super T> inter = Util.cast(interfaces[i]);
                inter = implementsMBean(inter, aName);
                if (inter != null) return inter;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public static String descriptionForElement(AnnotatedElement elmt) {
        if (elmt == null)
            return null;
        Description d = elmt.getAnnotation(Description.class);
        if (d == null)
            return null;
        return d.value();
    }

    public static String descriptionForParameter(
            Annotation[] parameterAnnotations) {
        for (Annotation a : parameterAnnotations) {
            if (a instanceof Description)
                return ((Description) a).value();
        }
        return null;
    }

    public static String nameForParameter(
            Annotation[] parameterAnnotations) {
        for (Annotation a : parameterAnnotations) {
            Class<? extends Annotation> ac = a.annotationType();
            // You'd really have to go out of your way to have more than
            // one @Name annotation, so we don't check for that.
            if (ac.getSimpleName().equals("Name")) {
                try {
                    Method value = ac.getMethod("value");
                    if (value.getReturnType() == String.class &&
                            value.getParameterTypes().length == 0) {
                        return (String) value.invoke(a);
                    }
                } catch (Exception e) {
                    MBEANSERVER_LOGGER.log(
                            Level.WARNING,
                            "Unexpected exception getting @" + ac.getName(),
                            e);
                }
            }
        }
        return null;
    }

    public static Descriptor descriptorForElement(final AnnotatedElement elmt,
            boolean isSetter) {
        if (elmt == null)
            return ImmutableDescriptor.EMPTY_DESCRIPTOR;
        final Annotation[] annots = elmt.getAnnotations();
        Descriptor descr = descriptorForAnnotations(annots);
        String[] exceptions = {};
        if(elmt instanceof Method)
            exceptions = getAllExceptions(((Method) elmt).getExceptionTypes());
        else
            if(elmt instanceof Constructor<?>)
                exceptions = getAllExceptions(((Constructor<?>) elmt).
                        getExceptionTypes());

        if(exceptions.length > 0 ) {
            String fieldName = isSetter ? JMX.SET_EXCEPTIONS_FIELD :
                JMX.EXCEPTIONS_FIELD;

            String[] fieldNames = {fieldName};
            Object[] fieldValues = {exceptions};
            descr = ImmutableDescriptor.union(descr,
                    new ImmutableDescriptor(fieldNames, fieldValues));
        }

        return descr;
    }

    public static Descriptor descriptorForAnnotation(Annotation annot) {
        return descriptorForAnnotations(new Annotation[] {annot});
    }

    public static Descriptor descriptorForAnnotations(Annotation[] annots) {
        if (annots.length == 0)
            return ImmutableDescriptor.EMPTY_DESCRIPTOR;
        Map<String, Object> descriptorMap = new HashMap<String, Object>();
        for (Annotation a : annots) {
            if (a instanceof DescriptorFields)
                addDescriptorFieldsToMap(descriptorMap, (DescriptorFields) a);
            addAnnotationFieldsToMap(descriptorMap, a);
        }

        if (descriptorMap.isEmpty())
            return ImmutableDescriptor.EMPTY_DESCRIPTOR;
        else
            return new ImmutableDescriptor(descriptorMap);
    }

    /**
     * Array of thrown excepions.
     * @param exceptions can be null;
     * @return An Array of Exception class names. Size is 0 if method is null.
     */
    private static String[] getAllExceptions(Class<?>[] exceptions) {
        Set<String> set = new LinkedHashSet<String>();
        for(Class<?>ex : exceptions)
            set.add(ex.getName());

        String[] arr = new String[set.size()];
        return set.toArray(arr);
    }

    private static void addDescriptorFieldsToMap(
            Map<String, Object> descriptorMap, DescriptorFields df) {
        for (String field : df.value()) {
            int eq = field.indexOf('=');
            if (eq < 0) {
                throw new IllegalArgumentException(
                        "@DescriptorFields string must contain '=': " +
                        field);
            }
            String name = field.substring(0, eq);
            String value = field.substring(eq + 1);
            addToMap(descriptorMap, name, value);
        }
    }

    private static void addAnnotationFieldsToMap(
            Map<String, Object> descriptorMap, Annotation a) {
        Class<? extends Annotation> c = a.annotationType();
        Method[] elements = c.getMethods();
        for (Method element : elements) {
            DescriptorKey key = element.getAnnotation(DescriptorKey.class);
            if (key != null) {
                String name = key.value();
                Object value;
                try {
                    value = element.invoke(a);
                } catch (RuntimeException e) {
                    // we don't expect this - except for possibly
                    // security exceptions?
                    // RuntimeExceptions shouldn't be "UndeclaredThrowable".
                    // anyway...
                    throw e;
                } catch (Exception e) {
                    // we don't expect this
                    throw new UndeclaredThrowableException(e);
                }
                if (!key.omitIfDefault() ||
                        !equals(value, element.getDefaultValue())) {
                    value = annotationToField(value);
                    addToMap(descriptorMap, name, value);
                }
            }
        }
    }

    private static void addToMap(
            Map<String, Object> descriptorMap, String name, Object value) {
        Object oldValue = descriptorMap.put(name, value);
        if (oldValue != null && !equals(oldValue, value)) {
            final String msg =
                "Inconsistent values for descriptor field " + name +
                " from annotations: " + value + " :: " + oldValue;
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Throws a NotCompliantMBeanException or a SecurityException.
     * @param notCompliant the class which was under examination
     * @param cause the raeson why NotCompliantMBeanException should
     *        be thrown.
     * @return nothing - this method always throw an exception.
     *         The return type makes it possible to write
     *         <pre> throw throwException(clazz,cause); </pre>
     * @throws SecurityException - if cause is a SecurityException
     * @throws NotCompliantMBeanException otherwise.
     **/
    static NotCompliantMBeanException throwException(Class<?> notCompliant,
            Throwable cause)
            throws NotCompliantMBeanException, SecurityException {
        if (cause instanceof SecurityException)
            throw (SecurityException) cause;
        if (cause instanceof NotCompliantMBeanException)
            throw (NotCompliantMBeanException)cause;
        final String classname =
                (notCompliant==null)?"null class":notCompliant.getName();
        final String reason =
                (cause==null)?"Not compliant":cause.getMessage();
        final NotCompliantMBeanException res =
                new NotCompliantMBeanException(classname+": "+reason);
        res.initCause(cause);
        throw res;
    }

    // Convert a value from an annotation element to a descriptor field value
    // E.g. with @interface Foo {class value()} an annotation @Foo(String.class)
    // will produce a Descriptor field value "java.lang.String"
    private static Object annotationToField(Object x) {
        // An annotation element cannot have a null value but never mind
        if (x == null)
            return null;
        if (x instanceof Number || x instanceof String ||
                x instanceof Character || x instanceof Boolean ||
                x instanceof String[])
            return x;
        // Remaining possibilities: array of primitive (e.g. int[]),
        // enum, class, array of enum or class.
        Class<?> c = x.getClass();
        if (c.isArray()) {
            if (c.getComponentType().isPrimitive())
                return x;
            Object[] xx = (Object[]) x;
            String[] ss = new String[xx.length];
            for (int i = 0; i < xx.length; i++)
                ss[i] = (String) annotationToField(xx[i]);
            return ss;
        }
        if (x instanceof Class<?>)
            return ((Class<?>) x).getName();
        if (x instanceof Enum<?>)
            return ((Enum<?>) x).name();
        // The only other possibility is that the value is another
        // annotation, or that the language has evolved since this code
        // was written.  We don't allow for either of those currently.
        // If it is indeed another annotation, then x will be a proxy
        // with an unhelpful name like $Proxy2.  So we extract the
        // proxy's interface to use that in the exception message.
        if (Proxy.isProxyClass(c))
            c = c.getInterfaces()[0];  // array "can't be empty"
        throw new IllegalArgumentException("Illegal type for annotation " +
                "element using @DescriptorKey: " + c.getName());
    }

    // This must be consistent with the check for duplicate field values in
    // ImmutableDescriptor.union.  But we don't expect to be called very
    // often so this inefficient check should be enough.
    private static boolean equals(Object x, Object y) {
        return Arrays.deepEquals(new Object[] {x}, new Object[] {y});
    }

    /**
     * Returns the XXMBean interface or null if no such interface exists
     *
     * @param c The interface to be tested
     * @param clName The name of the class implementing this interface
     */
    private static <T> Class<? super T> implementsMBean(Class<T> c, String clName) {
        String clMBeanName = clName + "MBean";
        if (c.getName().equals(clMBeanName)) {
            return c;
        }
        Class<?>[] interfaces = c.getInterfaces();
        for (int i = 0;i < interfaces.length; i++) {
            if (interfaces[i].getName().equals(clMBeanName))
                return Util.cast(interfaces[i]);
        }

        return null;
    }

    public static Object elementFromComplex(Object complex, String element)
    throws AttributeNotFoundException {
        try {
            if (complex.getClass().isArray() && element.equals("length")) {
                return Array.getLength(complex);
            } else if (complex instanceof CompositeData) {
                return ((CompositeData) complex).get(element);
            } else {
                // Java Beans introspection
                //
                BeanInfo bi = java.beans.Introspector.getBeanInfo(complex.getClass());
                PropertyDescriptor[] pds = bi.getPropertyDescriptors();
                for (PropertyDescriptor pd : pds)
                    if (pd.getName().equals(element))
                        return pd.getReadMethod().invoke(complex);
                throw new AttributeNotFoundException(
                    "Could not find the getter method for the property " +
                    element + " using the Java Beans introspector");
            }
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } catch (AttributeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw EnvHelp.initCause(
                new AttributeNotFoundException(e.getMessage()), e);
        }
    }
}
