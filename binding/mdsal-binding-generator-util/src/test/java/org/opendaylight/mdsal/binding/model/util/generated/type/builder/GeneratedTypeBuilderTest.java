/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.binding.model.util.generated.type.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import org.junit.Test;
import org.opendaylight.mdsal.binding.model.api.Constant;
import org.opendaylight.mdsal.binding.model.api.Enumeration;
import org.opendaylight.mdsal.binding.model.api.GeneratedProperty;
import org.opendaylight.mdsal.binding.model.api.GeneratedType;
import org.opendaylight.mdsal.binding.model.api.MethodSignature;
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.mdsal.binding.model.api.type.builder.EnumBuilder;
import org.opendaylight.mdsal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.mdsal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.mdsal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.mdsal.binding.model.api.type.builder.MethodSignatureBuilder;
import org.opendaylight.mdsal.binding.model.util.BindingGeneratorUtil;
import org.opendaylight.mdsal.binding.model.util.TypeComments;
import org.opendaylight.mdsal.binding.model.util.Types;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class GeneratedTypeBuilderTest {

    @Test
    public void addConstantTest() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        // assertNotNull(generatedTypeBuilder.addComment("My comment ..."));

        Constant constant = generatedTypeBuilder.addConstant(Types.typeForClass(String.class), "myConstant",
                "myConstantValue");
        // Constant constantx =
        // generatedTypeBuilder.addConstant(Types.typeForClass(String.class),
        // "myConstant", "myConstantValue");
        Constant constant2 = generatedTypeBuilder.addConstant(
                Types.primitiveType("int", BindingGeneratorUtil.getRestrictions(null)), "myIntConstant", 1);

        Constant constant3 = new ConstantImpl(generatedTypeBuilder, Types.typeForClass(String.class), "myConstant",
                "myConstantValue");
        Constant constant4 = new ConstantImpl(generatedTypeBuilder, Types.typeForClass(String.class), "myConstant2",
                "myConstantValue");
        Constant constant5 = new ConstantImpl(generatedTypeBuilder, Types.typeForClass(String.class), "myConstant",
                "myConstantValue2");

        assertNotNull(constant);
        assertNotNull(constant2);
        assertNotNull(constant3);
        assertNotNull(constant4);
        assertNotNull(constant5);
        // assertNotNull(constantx);
        // assertTrue(constant!=constantx);

        assertFalse(constant.equals(null));
        assertFalse(constant.equals(new Object()));
        assertTrue(constant.equals(constant));
        assertTrue(constant.equals(constant3));
        assertFalse(constant.equals(constant2));
        assertFalse(constant.equals(constant4));
        assertFalse(constant.equals(constant5));

        assertTrue(constant.hashCode() == constant.hashCode());
        assertTrue(constant.hashCode() == constant3.hashCode());
        assertFalse(constant.hashCode() == constant2.hashCode());
        assertFalse(constant.hashCode() == constant4.hashCode());
        assertTrue(constant.hashCode() == constant5.hashCode());

        assertEquals(
                "Constant [type=Type (java.lang.String), name=myConstant, value=myConstantValue, definingType=my.package.MyName]",
                constant.toString());

        assertEquals("Type (java.lang.String) myConstant myConstantValue", constant.toFormattedString());

        GeneratedType instance = generatedTypeBuilder.build();
        List<Constant> constantDefinitions = instance.getConstantDefinitions();
        assertNotNull(constantDefinitions);
        assertEquals(2, constantDefinitions.size());
        assertTrue(constantDefinitions.contains(constant));
        assertTrue(constantDefinitions.contains(constant2));
        assertTrue(constantDefinitions.contains(constant3));
        assertFalse(constantDefinitions.contains(constant4));
        assertFalse(constantDefinitions.contains(constant5));

        assertEquals("myConstant", constant.getName());
        assertEquals("myConstantValue", constant.getValue());
        assertEquals(Types.typeForClass(String.class), constant.getType());
        assertEquals(generatedTypeBuilder, constant.getDefiningType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addConstantIllegalArgumentTest() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");
        generatedTypeBuilder.addConstant(Types.typeForClass(String.class), null, "myConstantValue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addConstantIllegalArgumentTest2() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");
        generatedTypeBuilder.addConstant(null, "myConstantName", "myConstantValue");
    }

    @Test
    public void generatedTypeBuilderEqualsAndHashCodeTest() {
        CodegenGeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");
        CodegenGeneratedTypeBuilder generatedTypeBuilder2 = new CodegenGeneratedTypeBuilder("my.package", "MyName");
        CodegenGeneratedTypeBuilder generatedTypeBuilder3 = new CodegenGeneratedTypeBuilder("my.package", "MyName2");
        CodegenGeneratedTypeBuilder generatedTypeBuilder4 = new CodegenGeneratedTypeBuilder("my.package2", "MyName");

        assertFalse(generatedTypeBuilder.equals(null));
        assertFalse(generatedTypeBuilder.equals(new Object()));
        assertTrue(generatedTypeBuilder.equals(generatedTypeBuilder));
        assertTrue(generatedTypeBuilder.equals(generatedTypeBuilder2));

        assertTrue(generatedTypeBuilder.hashCode() == generatedTypeBuilder.hashCode());
        assertTrue(generatedTypeBuilder.hashCode() == generatedTypeBuilder2.hashCode());
        assertFalse(generatedTypeBuilder.hashCode() == generatedTypeBuilder3.hashCode());
        assertFalse(generatedTypeBuilder.hashCode() == generatedTypeBuilder4.hashCode());

    }

    @Test
    public void addPropertyTest() {
        CodegenGeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        GeneratedPropertyBuilder propertyBuilder = generatedTypeBuilder.addProperty("myProperty");
        GeneratedPropertyBuilder propertyBuilder2 = generatedTypeBuilder.addProperty("myProperty2");
        // GeneratedPropertyBuilder propertyBuilderNull =
        // generatedTypeBuilder.addProperty(null);

        assertNotNull(propertyBuilder);
        assertNotNull(propertyBuilder2);
        // assertNotNull(propertyBuilderNull);

        assertTrue(generatedTypeBuilder.containsProperty("myProperty"));
        assertTrue(generatedTypeBuilder.containsProperty("myProperty2"));
        assertFalse(generatedTypeBuilder.containsProperty("myProperty3"));

        GeneratedType instance = generatedTypeBuilder.build();
        List<GeneratedProperty> properties = instance.getProperties();

        assertEquals(2, properties.size());

        assertTrue(properties.contains(propertyBuilder.toInstance(instance)));
        assertTrue(properties.contains(propertyBuilder2.toInstance(instance)));
        // assertTrue(properties.contains(propertyBuilderNull.toInstance(instance)));
        assertFalse(properties.contains(new GeneratedPropertyBuilderImpl("myProperty3").toInstance(instance)));

    }

    @Test(expected = IllegalArgumentException.class)
    public void addMethodIllegalArgumentTest() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");
        generatedTypeBuilder.addMethod(null);
    }

    @Test
    public void addMethodTest() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        MethodSignatureBuilder methodBuilder = generatedTypeBuilder.addMethod("myMethodName");
        MethodSignatureBuilder methodBuilder2 = generatedTypeBuilder.addMethod("myMethodName2");

        assertNotNull(methodBuilder);
        assertNotNull(methodBuilder2);

        assertTrue(generatedTypeBuilder.containsMethod("myMethodName"));
        assertTrue(generatedTypeBuilder.containsMethod("myMethodName2"));
        assertFalse(generatedTypeBuilder.containsMethod("myMethodName3"));

        GeneratedType instance = generatedTypeBuilder.build();
        List<MethodSignature> methodDefinitions = instance.getMethodDefinitions();

        assertEquals(2, methodDefinitions.size());

        assertTrue(methodDefinitions.contains(methodBuilder.toInstance(instance)));
        assertTrue(methodDefinitions.contains(methodBuilder2.toInstance(instance)));
        assertFalse(methodDefinitions.contains(new MethodSignatureBuilderImpl("myMethodName3").toInstance(instance)));

    }

    @Test(expected = IllegalArgumentException.class)
    public void addEnumerationIllegalArgumentTest() {
        CodegenGeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        generatedTypeBuilder.addEnumeration(null);
    }

    @Test
    public void addEnumerationTest() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        EnumBuilder enumBuilder = generatedTypeBuilder.addEnumeration("myEnumName");
        EnumBuilder enumBuilder2 = generatedTypeBuilder.addEnumeration("myEnumName2");

        assertNotNull(enumBuilder);
        assertNotNull(enumBuilder2);

        GeneratedType instance = generatedTypeBuilder.build();
        List<Enumeration> enumerations = instance.getEnumerations();

        assertEquals(2, enumerations.size());

        assertTrue(enumerations.contains(enumBuilder.toInstance(instance)));
        assertTrue(enumerations.contains(enumBuilder2.toInstance(instance)));
        assertFalse(enumerations.contains(new CodegenEnumerationBuilder("my.package", "myEnumName3").toInstance(instance)));

    }

    @Test(expected = IllegalArgumentException.class)
    public void addImplementsTypeIllegalArgumentTest() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        generatedTypeBuilder.addImplementsType(null);
    }

    @Test
    public void addImplementsTypeTest() {
        CodegenGeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        assertEquals(generatedTypeBuilder,
                generatedTypeBuilder.addImplementsType(Types.typeForClass(Serializable.class)));
        assertEquals(generatedTypeBuilder, generatedTypeBuilder.addImplementsType(Types.typeForClass(Runnable.class)));

        GeneratedType instance = generatedTypeBuilder.build();
        List<Type> implementTypes = instance.getImplements();

        assertEquals(2, implementTypes.size());

        assertTrue(implementTypes.contains(Types.typeForClass(Serializable.class)));
        assertTrue(implementTypes.contains(Types.typeForClass(Runnable.class)));
        assertFalse(implementTypes.contains(Types.typeForClass(Throwable.class)));

    }

    @Test(expected = IllegalArgumentException.class)
    public void addEnclosingTransferObjectIllegalArgumentTest() {
        CodegenGeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        generatedTypeBuilder.addEnclosingTransferObject((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addEnclosingTransferObjectIllegalArgumentTest2() {
        CodegenGeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        generatedTypeBuilder.addEnclosingTransferObject((GeneratedTOBuilder) null);
    }

    @Test
    public void addEnclosingTransferObjectTest() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        GeneratedTOBuilder enclosingTransferObject = generatedTypeBuilder.addEnclosingTransferObject("myTOName");
        GeneratedTOBuilder enclosingTransferObject2 = generatedTypeBuilder.addEnclosingTransferObject("myTOName2");
        assertEquals(generatedTypeBuilder, generatedTypeBuilder.addEnclosingTransferObject(new CodegenGeneratedTOBuilder(
                generatedTypeBuilder.getFullyQualifiedName(), "myTOName3")));

        assertNotNull(enclosingTransferObject);
        assertNotNull(enclosingTransferObject2);

        GeneratedType instance = generatedTypeBuilder.build();
        List<GeneratedType> enclosedTypes = instance.getEnclosedTypes();

        assertEquals(3, enclosedTypes.size());

        assertTrue(enclosedTypes.contains(enclosingTransferObject.build()));
        assertTrue(enclosedTypes.contains(enclosingTransferObject2.build()));
        assertTrue(enclosedTypes.contains(new CodegenGeneratedTOBuilder(generatedTypeBuilder.getFullyQualifiedName(),
                "myTOName3").build()));
        assertFalse(enclosedTypes.contains(new CodegenGeneratedTOBuilder(generatedTypeBuilder.getFullyQualifiedName(),
                "myTOName4").build()));

    }

    @Test
    public void generatedTypeTest() {
        GeneratedTypeBuilder generatedTypeBuilder = new CodegenGeneratedTypeBuilder("my.package", "MyName");

        generatedTypeBuilder.setDescription("My description ...");
        generatedTypeBuilder.setModuleName("myModuleName");
        generatedTypeBuilder.setReference("myReference");
        generatedTypeBuilder.setSchemaPath(SchemaPath.create(true, QName.create("test", "/path")));
        assertNotNull(generatedTypeBuilder.addComment(TypeComments.javadoc("My comment..").get()));

        assertEquals(
                "GeneratedTransferObject [packageName=my.package, name=MyName, comment=My comment.., annotations=[], implements=[], enclosedTypes=[], constants=[], enumerations=[], properties=, methods=[]]",
                generatedTypeBuilder.toString());

        GeneratedType instance = generatedTypeBuilder.build();

        assertEquals("My description ...", instance.getDescription());
        assertEquals("myModuleName", instance.getModuleName());
        assertEquals("myReference", instance.getReference());
        assertEquals(SchemaPath.create(true, QName.create("test", "/path")).getPathFromRoot(), instance.getSchemaPath());
        assertEquals("My comment..", instance.getComment().getJavadoc());
    }
}
