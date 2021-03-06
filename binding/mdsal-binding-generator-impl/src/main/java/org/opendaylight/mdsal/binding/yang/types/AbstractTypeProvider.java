/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.binding.yang.types;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.mdsal.binding.model.util.BindingTypes.TYPE_OBJECT;
import static org.opendaylight.yangtools.yang.model.util.SchemaContextUtil.findDataSchemaNodeForRelativeXPath;
import static org.opendaylight.yangtools.yang.model.util.SchemaContextUtil.findDataTreeSchemaNode;
import static org.opendaylight.yangtools.yang.model.util.SchemaContextUtil.findParentModule;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.opendaylight.mdsal.binding.generator.spi.TypeProvider;
import org.opendaylight.mdsal.binding.generator.util.BaseYangTypesProvider;
import org.opendaylight.mdsal.binding.model.api.AccessModifier;
import org.opendaylight.mdsal.binding.model.api.ConcreteType;
import org.opendaylight.mdsal.binding.model.api.Enumeration;
import org.opendaylight.mdsal.binding.model.api.GeneratedProperty;
import org.opendaylight.mdsal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.mdsal.binding.model.api.JavaTypeName;
import org.opendaylight.mdsal.binding.model.api.Restrictions;
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.mdsal.binding.model.api.type.builder.EnumBuilder;
import org.opendaylight.mdsal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.mdsal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.mdsal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.mdsal.binding.model.api.type.builder.GeneratedTypeBuilderBase;
import org.opendaylight.mdsal.binding.model.api.type.builder.MethodSignatureBuilder;
import org.opendaylight.mdsal.binding.model.util.BaseYangTypes;
import org.opendaylight.mdsal.binding.model.util.BindingGeneratorUtil;
import org.opendaylight.mdsal.binding.model.util.BindingTypes;
import org.opendaylight.mdsal.binding.model.util.TypeConstants;
import org.opendaylight.mdsal.binding.model.util.Types;
import org.opendaylight.mdsal.binding.model.util.generated.type.builder.AbstractEnumerationBuilder;
import org.opendaylight.mdsal.binding.model.util.generated.type.builder.GeneratedPropertyBuilderImpl;
import org.opendaylight.mdsal.binding.spec.naming.BindingMapping;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.PathExpression;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.PatternConstraint;
import org.opendaylight.yangtools.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.ModuleDependencySort;
import org.opendaylight.yangtools.yang.model.util.PathExpressionImpl;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.opendaylight.yangtools.yang.model.util.type.BaseTypes;
import org.opendaylight.yangtools.yang.model.util.type.CompatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public abstract class AbstractTypeProvider implements TypeProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTypeProvider.class);
    private static final Pattern GROUPS_PATTERN = Pattern.compile("\\[(.*?)\\]");

    // Backwards compatibility: Union types used to be instantiated in YANG namespace, which is no longer
    // the case, as unions are emitted to their correct schema path.
    private static final SchemaPath UNION_PATH = SchemaPath.create(true,
        org.opendaylight.yangtools.yang.model.util.BaseTypes.UNION_QNAME);

    /**
     * Contains the schema data red from YANG files.
     */
    private final SchemaContext schemaContext;

    private final Map<String, Map<Optional<Revision>, Map<String, Type>>> genTypeDefsContextMap = new HashMap<>();

    /**
     * The map which maps schema paths to JAVA <code>Type</code>.
     */
    private final Map<SchemaPath, Type> referencedTypes = new HashMap<>();
    private final Map<Module, Set<Type>> additionalTypes = new HashMap<>();
    private final Map<SchemaNode, JavaTypeName> renames;

    /**
     * Creates new instance of class <code>TypeProviderImpl</code>.
     *
     * @param schemaContext contains the schema data red from YANG files
     * @param renames renaming table
     * @throws IllegalArgumentException if <code>schemaContext</code> equal null.
     */
    AbstractTypeProvider(final SchemaContext schemaContext, final Map<SchemaNode, JavaTypeName> renames) {
        Preconditions.checkArgument(schemaContext != null, "Schema Context cannot be null!");
        this.schemaContext = schemaContext;
        this.renames = requireNonNull(renames);
        resolveTypeDefsFromContext();
    }

    /**
     * Puts <code>refType</code> to map with key <code>refTypePath</code>.
     *
     * @param refTypePath schema path used as the map key
     * @param refType type which represents the map value
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if <code>refTypePath</code> equal null</li>
     *             <li>if <code>refType</code> equal null</li>
     *             </ul>
     *
     */
    public void putReferencedType(final SchemaPath refTypePath, final Type refType) {
        Preconditions.checkArgument(refTypePath != null,
                "Path reference of Enumeration Type Definition cannot be NULL!");
        Preconditions.checkArgument(refType != null, "Reference to Enumeration Type cannot be NULL!");
        referencedTypes.put(refTypePath, refType);
    }

    public Map<Module, Set<Type>> getAdditionalTypes() {
        return additionalTypes;
    }

    @Override
    public Type javaTypeForSchemaDefinitionType(final TypeDefinition<?> typeDefinition, final SchemaNode parentNode,
            final boolean lenientRelativeLeafrefs) {
        return javaTypeForSchemaDefinitionType(typeDefinition, parentNode, null, lenientRelativeLeafrefs);
    }

    /**
     * Converts schema definition type <code>typeDefinition</code> to JAVA <code>Type</code>.
     *
     * @param typeDefinition type definition which is converted to JAVA type
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if <code>typeDefinition</code> equal null</li>
     *             <li>if Qname of <code>typeDefinition</code> equal null</li>
     *             <li>if name of <code>typeDefinition</code> equal null</li>
     *             </ul>
     */
    @Override
    public Type javaTypeForSchemaDefinitionType(final TypeDefinition<?> typeDefinition, final SchemaNode parentNode,
            final Restrictions restrictions, final boolean lenientRelativeLeafrefs) {
        Preconditions.checkArgument(typeDefinition != null, "Type Definition cannot be NULL!");
        Preconditions.checkArgument(typeDefinition.getQName() != null,
                "Type Definition cannot have non specified QName (QName cannot be NULL!)");
        final String typedefName = typeDefinition.getQName().getLocalName();
        Preconditions.checkArgument(typedefName != null, "Type Definitions Local Name cannot be NULL!");

        // Deal with base types
        if (typeDefinition.getBaseType() == null) {
            // We have to deal with differing handling of decimal64. The old parser used a fixed Decimal64 type
            // and generated an enclosing ExtendedType to hold any range constraints. The new parser instantiates
            // a base type which holds these constraints.
            if (typeDefinition instanceof DecimalTypeDefinition) {
                final Type ret = BaseYangTypesProvider.INSTANCE.javaTypeForSchemaDefinitionType(typeDefinition,
                    parentNode, restrictions, lenientRelativeLeafrefs);
                if (ret != null) {
                    return ret;
                }
            }

            // Deal with leafrefs/identityrefs
            Type ret = javaTypeForLeafrefOrIdentityRef(typeDefinition, parentNode, lenientRelativeLeafrefs);
            if (ret != null) {
                return ret;
            }

            // FIXME: it looks as though we could be using the same codepath as above...
            ret = BaseYangTypes.javaTypeForYangType(typeDefinition.getQName().getLocalName());
            if (ret == null) {
                LOG.debug("Failed to resolve Java type for {}", typeDefinition);
            }

            return ret;
        }

        Type returnType = javaTypeForExtendedType(typeDefinition, lenientRelativeLeafrefs);
        if (restrictions != null && returnType instanceof GeneratedTransferObject) {
            final GeneratedTransferObject gto = (GeneratedTransferObject) returnType;
            final Module module = findParentModule(schemaContext, parentNode);
            final String basePackageName = BindingMapping.getRootPackageName(module.getQNameModule());
            final String packageName = BindingGeneratorUtil.packageNameForGeneratedType(basePackageName,
                typeDefinition.getPath());
            final String genTOName = BindingMapping.getClassName(typedefName);
            final String name = packageName + "." + genTOName;
            if (!returnType.getFullyQualifiedName().equals(name)) {
                returnType = shadedTOWithRestrictions(gto, restrictions);
            }
        }
        return returnType;
    }

    private GeneratedTransferObject shadedTOWithRestrictions(final GeneratedTransferObject gto,
            final Restrictions restrictions) {
        final GeneratedTOBuilder gtob = newGeneratedTOBuilder(gto.getIdentifier());
        final GeneratedTransferObject parent = gto.getSuperType();
        if (parent != null) {
            gtob.setExtendsType(parent);
        }
        gtob.setRestrictions(restrictions);
        for (GeneratedProperty gp : gto.getProperties()) {
            final GeneratedPropertyBuilder gpb = gtob.addProperty(gp.getName());
            gpb.setValue(gp.getValue());
            gpb.setReadOnly(gp.isReadOnly());
            gpb.setAccessModifier(gp.getAccessModifier());
            gpb.setReturnType(gp.getReturnType());
            gpb.setFinal(gp.isFinal());
            gpb.setStatic(gp.isStatic());
        }
        return gtob.build();
    }

    private boolean isLeafRefSelfReference(final LeafrefTypeDefinition leafref, final SchemaNode parentNode) {
        /*
         * First check if the leafref is an augment. If that is the case, skip it as it will be checked once augments
         * are resolved.
         */
        DataNodeContainer current = null;
        DataSchemaNode dataChildByName;
        for (QName next : parentNode.getPath().getPathFromRoot()) {
            if (current == null) {
                dataChildByName = schemaContext.getDataChildByName(next);
            } else {
                dataChildByName = current.getDataChildByName(next);
            }
            if (dataChildByName == null) {
                return false;
            }
            if (dataChildByName.isAugmenting()) {
                return false;
            }
            if (dataChildByName instanceof DataNodeContainer) {
                current = (DataNodeContainer) dataChildByName;
            }
        }

        // Then try to look up the expression.
        final PathExpression leafRefXPath = leafref.getPathStatement();
        final Module parentModule = getParentModule(parentNode);
        final SchemaNode leafRefValueNode;
        if (leafRefXPath.isAbsolute()) {
            leafRefValueNode = SchemaContextUtil.findDataTreeSchemaNode(schemaContext, parentModule.getQNameModule(),
                leafRefXPath);
        } else {
            leafRefValueNode = SchemaContextUtil.findDataSchemaNodeForRelativeXPath(schemaContext, parentModule,
                parentNode, new PathExpressionImpl(
                    GROUPS_PATTERN.matcher(leafRefXPath.getOriginalString()).replaceAll(""), false));
        }

        return leafRefValueNode != null && leafRefValueNode.equals(parentNode);
    }

    /**
     * Returns JAVA <code>Type</code> for instances of the type <code>LeafrefTypeDefinition</code> or
     * <code>IdentityrefTypeDefinition</code>.
     *
     * @param typeDefinition type definition which is converted to JAVA <code>Type</code>
     * @return JAVA <code>Type</code> instance for <code>typeDefinition</code>
     */
    private Type javaTypeForLeafrefOrIdentityRef(final TypeDefinition<?> typeDefinition, final SchemaNode parentNode,
            final boolean inGrouping) {
        if (typeDefinition instanceof LeafrefTypeDefinition) {
            final LeafrefTypeDefinition leafref = (LeafrefTypeDefinition) typeDefinition;
            Preconditions.checkArgument(!isLeafRefSelfReference(leafref, parentNode),
                "Leafref %s is referencing itself, incoming StackOverFlowError detected.", leafref);
            return provideTypeForLeafref(leafref, parentNode, inGrouping);
        } else if (typeDefinition instanceof IdentityrefTypeDefinition) {
            return provideTypeForIdentityref((IdentityrefTypeDefinition) typeDefinition);
        }

        return null;
    }

    /**
     * Returns JAVA <code>Type</code> for instances of the type <code>ExtendedType</code>.
     *
     * @param typeDefinition type definition which is converted to JAVA <code>Type</code>
     * @return JAVA <code>Type</code> instance for <code>typeDefinition</code>
     */
    private Type javaTypeForExtendedType(final TypeDefinition<?> typeDefinition, final boolean lenient) {
        final String typedefName = typeDefinition.getQName().getLocalName();
        final TypeDefinition<?> baseTypeDef = baseTypeDefForExtendedType(typeDefinition);
        Type returnType = javaTypeForLeafrefOrIdentityRef(baseTypeDef, typeDefinition, lenient);
        if (returnType == null) {
            if (baseTypeDef instanceof EnumTypeDefinition) {
                final EnumTypeDefinition enumTypeDef = (EnumTypeDefinition) baseTypeDef;
                returnType = provideTypeForEnum(enumTypeDef, typedefName, typeDefinition);
            } else {
                final Module module = findParentModule(schemaContext, typeDefinition);
                final Restrictions r = BindingGeneratorUtil.getRestrictions(typeDefinition);
                if (module != null) {
                    final Map<Optional<Revision>, Map<String, Type>> modulesByDate = genTypeDefsContextMap.get(
                        module.getName());
                    final Map<String, Type> genTOs = modulesByDate.get(module.getRevision());
                    if (genTOs != null) {
                        returnType = genTOs.get(typedefName);
                    }
                    if (returnType == null) {
                        returnType = BaseYangTypesProvider.INSTANCE.javaTypeForSchemaDefinitionType(baseTypeDef,
                            typeDefinition, r, lenient);
                    }
                }
            }
        }
        return returnType;
    }

    /**
     * Seeks for identity reference <code>idref</code> the JAVA <code>type</code>.
     *
     * <p>
     * <i>Example:<br />
     * If identy which is referenced via <code>idref</code> has name <b>Idn</b>
     * then returning type is <b>{@code Class<? extends Idn>}</b></i>
     *
     * @param idref identityref type definition for which JAVA <code>Type</code> is sought
     * @return JAVA <code>Type</code> of the identity which is referenced through <code>idref</code>
     */
    private Type provideTypeForIdentityref(final IdentityrefTypeDefinition idref) {
        final Collection<? extends IdentitySchemaNode> identities = idref.getIdentities();
        if (identities.size() > 1) {
            LOG.warn("Identity reference {} has multiple identities, using only the first one", idref);
        }

        final QName baseIdQName = identities.iterator().next().getQName();
        final Module module = schemaContext.findModule(baseIdQName.getModule()).orElse(null);
        IdentitySchemaNode identity = null;
        for (IdentitySchemaNode id : module.getIdentities()) {
            if (id.getQName().equals(baseIdQName)) {
                identity = id;
            }
        }
        Preconditions.checkArgument(identity != null, "Target identity '" + baseIdQName + "' do not exist");

        final String basePackageName = BindingMapping.getRootPackageName(module.getQNameModule());
        final JavaTypeName identifier = JavaTypeName.create(BindingGeneratorUtil.packageNameForGeneratedType(
            basePackageName, identity.getPath()), BindingMapping.getClassName(identity.getQName()));
        return Types.classType(Types.wildcardTypeFor(identifier));
    }

    /**
     * Converts <code>typeDefinition</code> to concrete JAVA <code>Type</code>.
     *
     * @param typeDefinition
     *            type definition which should be converted to JAVA
     *            <code>Type</code>
     * @return JAVA <code>Type</code> which represents
     *         <code>typeDefinition</code>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if <code>typeDefinition</code> equal null</li>
     *             <li>if Q name of <code>typeDefinition</code></li>
     *             <li>if name of <code>typeDefinition</code></li>
     *             </ul>
     */
    public Type generatedTypeForExtendedDefinitionType(final TypeDefinition<?> typeDefinition,
            final SchemaNode parentNode) {
        Preconditions.checkArgument(typeDefinition != null, "Type Definition cannot be NULL!");
        if (typeDefinition.getQName() == null) {
            throw new IllegalArgumentException("Type Definition cannot have unspecified QName (QName cannot be NULL!)");
        }
        Preconditions.checkArgument(typeDefinition.getQName().getLocalName() != null,
                "Type Definitions Local Name cannot be NULL!");

        final TypeDefinition<?> baseTypeDef = baseTypeDefForExtendedType(typeDefinition);
        if (baseTypeDef instanceof LeafrefTypeDefinition || baseTypeDef instanceof IdentityrefTypeDefinition) {
            /*
             * This is backwards compatibility baggage from way back when. The problem at hand is inconsistency between
             * the fact that identity is mapped to a Class, which is also returned from leaves which specify it like
             * this:
             *
             *     identity iden;
             *
             *     container foo {
             *         leaf foo {
             *             type identityref {
             *                 base iden;
             *             }
             *         }
             *     }
             *
             * This results in getFoo() returning Class<? extends Iden>, which looks fine on the surface, but gets more
             * dicey when we throw in:
             *
             *     typedef bar-ref {
             *         type identityref {
             *             base iden;
             *         }
             *     }
             *
             *     container bar {
             *         leaf bar {
             *             type bar-ref;
             *         }
             *     }
             *
             * Now we have competing requirements: typedef would like us to use encapsulation to capture the defined
             * type, while getBar() wants us to retain shape with getFoo(), as it should not matter how the identityref
             * is formed.
             *
             * In this particular case getFoo() won just after the Binding Spec was frozen, hence we do not generate
             * an encapsulation for identityref typedefs.
             *
             * In case you are thinking we could get by having foo-ref map to a subclass of Iden, that is not a good
             * option, as it would look as though it is the product of a different construct:
             *
             *     identity bar-ref {
             *         base iden;
             *     }
             *
             * Leading to a rather nice namespace clash and also slight incompatibility with unknown third-party
             * sub-identities of iden.
             *
             * The story behind leafrefs is probably similar, but that needs to be ascertained.
             */
            return null;
        }

        final Module module = findParentModule(schemaContext, parentNode);
        if (module != null) {
            final Map<Optional<Revision>, Map<String, Type>> modulesByDate = genTypeDefsContextMap.get(
                module.getName());
            final Map<String, Type> genTOs = modulesByDate.get(module.getRevision());
            if (genTOs != null) {
                return genTOs.get(typeDefinition.getQName().getLocalName());
            }
        }
        return null;
    }

    /**
     * Gets base type definition for <code>extendTypeDef</code>. The method is
     * recursively called until non <code>ExtendedType</code> type is found.
     *
     * @param extendTypeDef
     *            type definition for which is the base type definition sought
     * @return type definition which is base type for <code>extendTypeDef</code>
     * @throws IllegalArgumentException
     *             if <code>extendTypeDef</code> equal null
     */
    private static TypeDefinition<?> baseTypeDefForExtendedType(final TypeDefinition<?> extendTypeDef) {
        Preconditions.checkArgument(extendTypeDef != null, "Type Definition reference cannot be NULL!");

        TypeDefinition<?> ret = extendTypeDef;
        while (ret.getBaseType() != null) {
            ret = ret.getBaseType();
        }

        return ret;
    }

    /**
     * Converts <code>leafrefType</code> to JAVA <code>Type</code>. The path of <code>leafrefType</code> is followed
     * to find referenced node and its <code>Type</code> is returned.
     *
     * @param leafrefType leafref type definition for which is the type sought
     * @param parentNode parent node of the leaf being resolved
     * @param inGrouping true if we are resolving the type within a grouping.
     * @return JAVA <code>Type</code> of data schema node which is referenced in <code>leafrefType</code>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if <code>leafrefType</code> equal null</li>
     *             <li>if path statement of <code>leafrefType</code> equal null</li>
     *             </ul>
     */
    @VisibleForTesting
    Type provideTypeForLeafref(final LeafrefTypeDefinition leafrefType, final SchemaNode parentNode,
            final boolean inGrouping) {
        Preconditions.checkArgument(leafrefType != null, "Leafref Type Definition reference cannot be NULL!");

        final PathExpression xpath = leafrefType.getPathStatement();
        Preconditions.checkArgument(xpath != null, "The Path Statement for Leafref Type Definition cannot be NULL!");

        final String strXPath = xpath.getOriginalString();
        if (strXPath.indexOf('[') != -1) {
            // XXX: why are we special-casing this?
            return Types.objectType();
        }

        final Module module = findParentModule(schemaContext, parentNode);
        Preconditions.checkArgument(module != null, "Failed to find module for parent %s", parentNode);

        final SchemaNode dataNode;
        if (xpath.isAbsolute()) {
            dataNode = findDataTreeSchemaNode(schemaContext, module.getQNameModule(), xpath);
        } else {
            dataNode = findDataSchemaNodeForRelativeXPath(schemaContext, module, parentNode, xpath);
            if (dataNode == null && inGrouping) {
                // Relative path within a grouping may end up being unresolvable because it may refer outside
                // the grouping, in which case it is polymorphic based on instantiation, for example:
                //
                // grouping foo {
                //     leaf foo {
                //         type leafref {
                //             path "../../bar";
                //         }
                //     }
                // }
                //
                // container one {
                //     leaf bar {
                //         type string;
                //     }
                //     uses foo;
                // }
                //
                // container two {
                //     leaf bar {
                //         type uint16;
                //     }
                //     uses foo;
                // }
                LOG.debug("Leafref type {} not found in parent {}, assuming polymorphic object", leafrefType,
                    parentNode);
                return Types.objectType();
            }
        }
        Preconditions.checkArgument(dataNode != null, "Failed to find leafref target: %s in module %s (%s)",
                strXPath, this.getParentModule(parentNode).getName(), parentNode.getQName().getModule());

        // FIXME: this block seems to be some weird magic hack. Analyze and refactor it.
        Type returnType = null;
        if (leafContainsEnumDefinition(dataNode)) {
            returnType = referencedTypes.get(dataNode.getPath());
        } else if (leafListContainsEnumDefinition(dataNode)) {
            returnType = Types.listTypeFor(referencedTypes.get(dataNode.getPath()));
        }
        if (returnType == null) {
            returnType = resolveTypeFromDataSchemaNode(dataNode, inGrouping);
        }
        Preconditions.checkArgument(returnType != null, "Failed to find leafref target: %s in module %s (%s)",
                strXPath, this.getParentModule(parentNode).getName(), parentNode.getQName().getModule(), this);
        return returnType;
    }

    /**
     * Checks if <code>dataNode</code> is <code>LeafSchemaNode</code> and if it so then checks if it is of type
     * <code>EnumTypeDefinition</code>.
     *
     * @param dataNode data schema node for which is checked if it is leaf and if it is of enum type
     * @return boolean value
     *         <ul>
     *         <li>true - if <code>dataNode</code> is leaf of type enumeration</li>
     *         <li>false - other cases</li>
     *         </ul>
     */
    private static boolean leafContainsEnumDefinition(final SchemaNode dataNode) {
        if (dataNode instanceof LeafSchemaNode) {
            final LeafSchemaNode leaf = (LeafSchemaNode) dataNode;
            return CompatUtils.compatType(leaf) instanceof EnumTypeDefinition;
        }
        return false;
    }

    /**
     * Checks if <code>dataNode</code> is <code>LeafListSchemaNode</code> and if it so then checks if it is of type
     * <code>EnumTypeDefinition</code>.
     *
     * @param dataNode data schema node for which is checked if it is leaflist and if it is of enum type
     * @return boolean value
     *         <ul>
     *         <li>true - if <code>dataNode</code> is leaflist of type
     *         enumeration</li>
     *         <li>false - other cases</li>
     *         </ul>
     */
    private static boolean leafListContainsEnumDefinition(final SchemaNode dataNode) {
        if (dataNode instanceof LeafListSchemaNode) {
            final LeafListSchemaNode leafList = (LeafListSchemaNode) dataNode;
            return leafList.getType() instanceof EnumTypeDefinition;
        }
        return false;
    }

    /**
     * Converts <code>enumTypeDef</code> to {@link Enumeration enumeration}.
     *
     * @param enumTypeDef enumeration type definition which is converted to enumeration
     * @param enumName string with name which is used as the enumeration name
     * @return enumeration type which is built with data (name, enum values) from <code>enumTypeDef</code>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if <code>enumTypeDef</code> equals null</li>
     *             <li>if enum values of <code>enumTypeDef</code> equal null</li>
     *             <li>if Q name of <code>enumTypeDef</code> equal null</li>
     *             <li>if name of <code>enumTypeDef</code> equal null</li>
     *             </ul>
     */
    private Enumeration provideTypeForEnum(final EnumTypeDefinition enumTypeDef, final String enumName,
            final SchemaNode parentNode) {
        Preconditions.checkArgument(enumTypeDef != null, "EnumTypeDefinition reference cannot be NULL!");
        Preconditions.checkArgument(enumTypeDef.getValues() != null,
                "EnumTypeDefinition MUST contain at least ONE value definition!");
        Preconditions.checkArgument(enumTypeDef.getQName() != null, "EnumTypeDefinition MUST contain NON-NULL QName!");
        Preconditions.checkArgument(enumTypeDef.getQName().getLocalName() != null,
                "Local Name in EnumTypeDefinition QName cannot be NULL!");

        final Module module = findParentModule(schemaContext, parentNode);
        final AbstractEnumerationBuilder enumBuilder = newEnumerationBuilder(JavaTypeName.create(
            BindingMapping.getRootPackageName(module.getQNameModule()), BindingMapping.getClassName(enumName)));
        addEnumDescription(enumBuilder, enumTypeDef);
        enumTypeDef.getReference().ifPresent(enumBuilder::setReference);
        enumBuilder.setModuleName(module.getName());
        enumBuilder.setSchemaPath(enumTypeDef.getPath());
        enumBuilder.updateEnumPairsFromEnumTypeDef(enumTypeDef);
        return enumBuilder.toInstance(null);
    }

    /**
     * Adds enumeration to <code>typeBuilder</code>. The enumeration data are taken from <code>enumTypeDef</code>.
     *
     * @param enumTypeDef enumeration type definition is source of enumeration data for <code>typeBuilder</code>
     * @param enumName string with the name of enumeration
     * @param typeBuilder generated type builder to which is enumeration added
     * @return enumeration type which contains enumeration data form <code>enumTypeDef</code>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if <code>enumTypeDef</code> equals null</li>
     *             <li>if enum values of <code>enumTypeDef</code> equal null</li>
     *             <li>if Q name of <code>enumTypeDef</code> equal null</li>
     *             <li>if name of <code>enumTypeDef</code> equal null</li>
     *             <li>if name of <code>typeBuilder</code> equal null</li>
     *             </ul>
     *
     */
    private Enumeration addInnerEnumerationToTypeBuilder(final EnumTypeDefinition enumTypeDef,
            final String enumName, final GeneratedTypeBuilderBase<?> typeBuilder) {
        Preconditions.checkArgument(enumTypeDef != null, "EnumTypeDefinition reference cannot be NULL!");
        Preconditions.checkArgument(enumTypeDef.getValues() != null,
                "EnumTypeDefinition MUST contain at least ONE value definition!");
        Preconditions.checkArgument(enumTypeDef.getQName() != null, "EnumTypeDefinition MUST contain NON-NULL QName!");
        Preconditions.checkArgument(enumTypeDef.getQName().getLocalName() != null,
                "Local Name in EnumTypeDefinition QName cannot be NULL!");
        Preconditions.checkArgument(typeBuilder != null, "Generated Type Builder reference cannot be NULL!");

        final EnumBuilder enumBuilder = typeBuilder.addEnumeration(BindingMapping.getClassName(enumName));

        addEnumDescription(enumBuilder, enumTypeDef);
        enumBuilder.updateEnumPairsFromEnumTypeDef(enumTypeDef);
        return enumBuilder.toInstance(enumBuilder);
    }

    public abstract void addEnumDescription(EnumBuilder enumBuilder, EnumTypeDefinition enumTypeDef);

    public abstract AbstractEnumerationBuilder newEnumerationBuilder(JavaTypeName identifier);

    public abstract GeneratedTOBuilder newGeneratedTOBuilder(JavaTypeName identifier);

    public abstract GeneratedTypeBuilder newGeneratedTypeBuilder(JavaTypeName identifier);

    /**
     * Converts the pattern constraints to the list of the strings which represents these constraints.
     *
     * @param patternConstraints list of pattern constraints
     * @return list of strings which represents the constraint patterns
     */
    public abstract Map<String, String> resolveRegExpressions(List<PatternConstraint> patternConstraints);

    abstract void addCodegenInformation(GeneratedTypeBuilderBase<?> genTOBuilder, TypeDefinition<?> typeDef);

    /**
     * Converts the pattern constraints from <code>typedef</code> to the list of the strings which represents these
     * constraints.
     *
     * @param typedef extended type in which are the pattern constraints sought
     * @return list of strings which represents the constraint patterns
     * @throws IllegalArgumentException if <code>typedef</code> equals null
     *
     */
    private Map<String, String> resolveRegExpressionsFromTypedef(final TypeDefinition<?> typedef) {
        if (!(typedef instanceof StringTypeDefinition)) {
            return ImmutableMap.of();
        }

        // TODO: run diff against base ?
        return resolveRegExpressions(((StringTypeDefinition) typedef).getPatternConstraints());
    }

    /**
     * Converts <code>dataNode</code> to JAVA <code>Type</code>.
     *
     * @param dataNode contains information about YANG type
     * @return JAVA <code>Type</code> representation of <code>dataNode</code>
     */
    private Type resolveTypeFromDataSchemaNode(final SchemaNode dataNode, final boolean inGrouping) {
        Type returnType = null;
        if (dataNode != null) {
            if (dataNode instanceof LeafSchemaNode) {
                final LeafSchemaNode leaf = (LeafSchemaNode) dataNode;
                final TypeDefinition<?> type = CompatUtils.compatType(leaf);
                returnType = javaTypeForSchemaDefinitionType(type, leaf, inGrouping);
            } else if (dataNode instanceof LeafListSchemaNode) {
                final LeafListSchemaNode leafList = (LeafListSchemaNode) dataNode;
                returnType = javaTypeForSchemaDefinitionType(leafList.getType(), leafList, inGrouping);
            }
        }
        return returnType;
    }

    /**
     * Passes through all modules and through all its type definitions and convert it to generated types.
     *
     * <p>
     * The modules are first sorted by mutual dependencies. The modules are sequentially passed. All type definitions
     * of a module are at the beginning sorted so that type definition with less amount of references to other type
     * definition are processed first.<br>
     * For each module is created mapping record in the map
     * {@link AbstractTypeProvider#genTypeDefsContextMap genTypeDefsContextMap}
     * which map current module name to the map which maps type names to returned types (generated types).
     */
    private void resolveTypeDefsFromContext() {
        final List<Module> modulesSortedByDependency = ModuleDependencySort.sort(schemaContext.getModules());

        for (Module module : modulesSortedByDependency) {
            Map<Optional<Revision>, Map<String, Type>> dateTypeMap = genTypeDefsContextMap.computeIfAbsent(
                module.getName(), key -> new HashMap<>());
            dateTypeMap.put(module.getRevision(), Collections.emptyMap());
            genTypeDefsContextMap.put(module.getName(), dateTypeMap);
        }

        for (Module module : modulesSortedByDependency) {
            if (module != null) {
                final String basePackageName = BindingMapping.getRootPackageName(module.getQNameModule());
                if (basePackageName != null) {
                    final List<TypeDefinition<?>> typeDefinitions = TypedefResolver.getAllTypedefs(module);
                    for (TypeDefinition<?> typedef : sortTypeDefinitionAccordingDepth(typeDefinitions)) {
                        typedefToGeneratedType(basePackageName, module, typedef);
                    }
                }
            }
        }
    }

    /**
     * Create Type for specified type definition.
     *
     * @param basePackageName string with name of package to which the module belongs
     * @param module string with the name of the module for to which the <code>typedef</code> belongs
     * @param typedef type definition of the node for which should be created JAVA <code>Type</code>
     *                (usually generated TO)
     * @return JAVA <code>Type</code> representation of <code>typedef</code> or
     *         <code>null</code> value if <code>basePackageName</code> or
     *         <code>modulName</code> or <code>typedef</code> or Q name of
     *         <code>typedef</code> equals <code>null</code>
     */
    private Type typedefToGeneratedType(final String basePackageName, final Module module,
            final TypeDefinition<?> typedef) {
        final TypeDefinition<?> baseTypedef = typedef.getBaseType();

        // See generatedTypeForExtendedDefinitionType() above for rationale behind this special case.
        if (baseTypedef instanceof LeafrefTypeDefinition || baseTypedef instanceof IdentityrefTypeDefinition) {
            return null;
        }

        final String typedefName = typedef.getQName().getLocalName();

        final Type returnType;
        if (baseTypedef.getBaseType() != null) {
            returnType = provideGeneratedTOFromExtendedType(typedef, baseTypedef, basePackageName,
                module.getName());
        } else if (baseTypedef instanceof UnionTypeDefinition) {
            final GeneratedTOBuilder genTOBuilder = provideGeneratedTOBuilderForUnionTypeDef(
                JavaTypeName.create(basePackageName, BindingMapping.getClassName(typedef.getQName())),
                (UnionTypeDefinition) baseTypedef, typedef);
            genTOBuilder.setTypedef(true);
            genTOBuilder.setIsUnion(true);
            addUnitsToGenTO(genTOBuilder, typedef.getUnits().orElse(null));
            makeSerializable(genTOBuilder);
            returnType = genTOBuilder.build();

            // Define a corresponding union builder. Typedefs are always anchored at a Java package root,
            // so we are placing the builder alongside the union.
            final GeneratedTOBuilder unionBuilder = newGeneratedTOBuilder(
                JavaTypeName.create(genTOBuilder.getPackageName(), genTOBuilder.getName() + "Builder"));
            unionBuilder.setIsUnionBuilder(true);
            final MethodSignatureBuilder method = unionBuilder.addMethod("getDefaultInstance");
            method.setReturnType(returnType);
            method.addParameter(Types.STRING, "defaultValue");
            method.setAccessModifier(AccessModifier.PUBLIC);
            method.setStatic(true);
            additionalTypes.computeIfAbsent(module, key -> new HashSet<>()).add(unionBuilder.build());
        } else if (baseTypedef instanceof EnumTypeDefinition) {
            // enums are automatically Serializable
            final EnumTypeDefinition enumTypeDef = (EnumTypeDefinition) baseTypedef;
            // TODO units for typedef enum
            returnType = provideTypeForEnum(enumTypeDef, typedefName, typedef);
        } else if (baseTypedef instanceof BitsTypeDefinition) {
            final GeneratedTOBuilder genTOBuilder = provideGeneratedTOBuilderForBitsTypeDefinition(
                JavaTypeName.create(basePackageName, BindingMapping.getClassName(typedef.getQName())),
                (BitsTypeDefinition) baseTypedef, module.getName());
            genTOBuilder.setTypedef(true);
            addUnitsToGenTO(genTOBuilder, typedef.getUnits().orElse(null));
            makeSerializable(genTOBuilder);
            returnType = genTOBuilder.build();
        } else {
            final Type javaType = javaTypeForSchemaDefinitionType(baseTypedef, typedef);
            returnType = wrapJavaTypeIntoTO(basePackageName, typedef, javaType, module.getName());
        }
        if (returnType != null) {
            final Map<Optional<Revision>, Map<String, Type>> modulesByDate =
                    genTypeDefsContextMap.get(module.getName());
            final Optional<Revision> moduleRevision = module.getRevision();
            Map<String, Type> typeMap = modulesByDate.get(moduleRevision);
            if (typeMap != null) {
                if (typeMap.isEmpty()) {
                    typeMap = new HashMap<>(4);
                    modulesByDate.put(moduleRevision, typeMap);
                }
                typeMap.put(typedefName, returnType);
            }
            return returnType;
        }
        return null;
    }

    /**
     * Wraps base YANG type to generated TO.
     *
     * @param basePackageName string with name of package to which the module belongs
     * @param typedef type definition which is converted to the TO
     * @param javaType JAVA <code>Type</code> to which is <code>typedef</code> mapped
     * @return generated transfer object which represent<code>javaType</code>
     */
    private GeneratedTransferObject wrapJavaTypeIntoTO(final String basePackageName, final TypeDefinition<?> typedef,
            final Type javaType, final String moduleName) {
        requireNonNull(javaType, "javaType cannot be null");

        final GeneratedTOBuilder genTOBuilder = typedefToTransferObject(basePackageName, typedef, moduleName);
        genTOBuilder.setRestrictions(BindingGeneratorUtil.getRestrictions(typedef));
        final GeneratedPropertyBuilder genPropBuilder = genTOBuilder.addProperty(TypeConstants.VALUE_PROP);
        genPropBuilder.setReturnType(javaType);

        genTOBuilder.addEqualsIdentity(genPropBuilder);
        genTOBuilder.addHashIdentity(genPropBuilder);
        genTOBuilder.addToStringProperty(genPropBuilder);
        genTOBuilder.addImplementsType(BindingTypes.scalarTypeObject(javaType));
        if (typedef.getStatus() == Status.DEPRECATED) {
            genTOBuilder.addAnnotation("java.lang", "Deprecated");
        }
        if (javaType instanceof ConcreteType && "String".equals(javaType.getName()) && typedef.getBaseType() != null) {
            addStringRegExAsConstant(genTOBuilder, resolveRegExpressionsFromTypedef(typedef));
        }
        addUnitsToGenTO(genTOBuilder, typedef.getUnits().orElse(null));
        genTOBuilder.setTypedef(true);
        makeSerializable(genTOBuilder);
        return genTOBuilder.build();
    }

    /**
     * Converts output list of generated TO builders to one TO builder (first
     * from list) which contains the remaining builders as its enclosing TO.
     *
     * @param typeName new type identifier
     * @param typedef type definition which should be of type {@link UnionTypeDefinition}
     * @return generated TO builder with the list of enclosed generated TO builders
     */
    public GeneratedTOBuilder provideGeneratedTOBuilderForUnionTypeDef(final JavaTypeName typeName,
            final UnionTypeDefinition typedef, final TypeDefinition<?> parentNode) {
        final List<GeneratedTOBuilder> builders = provideGeneratedTOBuildersForUnionTypeDef(typeName, typedef,
            parentNode);
        Preconditions.checkState(!builders.isEmpty(), "No GeneratedTOBuilder objects generated from union %s", typedef);

        final GeneratedTOBuilder resultTOBuilder = builders.remove(0);
        builders.forEach(resultTOBuilder::addEnclosingTransferObject);
        return resultTOBuilder;
    }

    /**
     * Converts <code>typedef</code> to generated TO with <code>typeDefName</code>. Every union type from
     * <code>typedef</code> is added to generated TO builder as property.
     *
     * @param typeName new type identifier
     * @param typedef type definition which should be of type <code>UnionTypeDefinition</code>
     * @return generated TO builder which represents <code>typedef</code>
     * @throws NullPointerException
     *             <ul>
     *             <li>if <code>basePackageName</code> is null</li>
     *             <li>if <code>typedef</code> is null</li>
     *             <li>if Qname of <code>typedef</code> is null</li>
     *             </ul>
     */
    public List<GeneratedTOBuilder> provideGeneratedTOBuildersForUnionTypeDef(final JavaTypeName typeName,
            final UnionTypeDefinition typedef, final SchemaNode parentNode) {
        requireNonNull(typedef, "Type Definition cannot be NULL!");
        requireNonNull(typedef.getQName(), "Type definition QName cannot be NULL!");

        final List<GeneratedTOBuilder> generatedTOBuilders = new ArrayList<>();
        final List<TypeDefinition<?>> unionTypes = typedef.getTypes();
        final Module module = findParentModule(schemaContext, parentNode);

        final GeneratedTOBuilder unionGenTOBuilder = newGeneratedTOBuilder(typeName);
        unionGenTOBuilder.setIsUnion(true);
        unionGenTOBuilder.setSchemaPath(typedef.getPath());
        unionGenTOBuilder.setModuleName(module.getName());
        unionGenTOBuilder.addImplementsType(TYPE_OBJECT);
        addCodegenInformation(unionGenTOBuilder, typedef);
        generatedTOBuilders.add(unionGenTOBuilder);

        // Pattern string is the key, XSD regex is the value. The reason for this choice is that the pattern carries
        // also negation information and hence guarantees uniqueness.
        final Map<String, String> expressions = new HashMap<>();
        for (TypeDefinition<?> unionType : unionTypes) {
            final String unionTypeName = unionType.getQName().getLocalName();

            // If we have a base type we should follow the type definition backwards, except for identityrefs, as those
            // do not follow type encapsulation -- we use the general case for that.
            if (unionType.getBaseType() != null  && !(unionType instanceof IdentityrefTypeDefinition)) {
                resolveExtendedSubtypeAsUnion(unionGenTOBuilder, unionType, expressions, parentNode);
            } else if (unionType instanceof UnionTypeDefinition) {
                generatedTOBuilders.addAll(resolveUnionSubtypeAsUnion(unionGenTOBuilder,
                    (UnionTypeDefinition) unionType, parentNode));
            } else if (unionType instanceof EnumTypeDefinition) {
                final Enumeration enumeration = addInnerEnumerationToTypeBuilder((EnumTypeDefinition) unionType,
                        unionTypeName, unionGenTOBuilder);
                updateUnionTypeAsProperty(unionGenTOBuilder, enumeration, unionTypeName);
            } else {
                final Type javaType = javaTypeForSchemaDefinitionType(unionType, parentNode);
                updateUnionTypeAsProperty(unionGenTOBuilder, javaType, unionTypeName);
            }
        }
        addStringRegExAsConstant(unionGenTOBuilder, expressions);

        storeGenTO(typedef, unionGenTOBuilder, parentNode);

        return generatedTOBuilders;
    }

    /**
     * Wraps code which handles the case when union subtype is also of the type <code>UnionType</code>.
     *
     * <p>
     * In this case the new generated TO is created for union subtype (recursive call of method
     * {@link #provideGeneratedTOBuildersForUnionTypeDef(String, UnionTypeDefinition, String, SchemaNode)}
     * provideGeneratedTOBuilderForUnionTypeDef} and in parent TO builder <code>parentUnionGenTOBuilder</code> is
     * created property which type is equal to new generated TO.
     *
     * @param parentUnionGenTOBuilder generated TO builder to which is the property with the child union subtype added
     * @param basePackageName string with the name of the module package
     * @param unionSubtype type definition which represents union subtype
     * @return list of generated TO builders. The number of the builders can be bigger one due to recursive call of
     *         <code>provideGeneratedTOBuildersForUnionTypeDef</code> method.
     */
    private List<GeneratedTOBuilder> resolveUnionSubtypeAsUnion(final GeneratedTOBuilder parentUnionGenTOBuilder,
            final UnionTypeDefinition unionSubtype, final SchemaNode parentNode) {
        final JavaTypeName newTOBuilderName = parentUnionGenTOBuilder.getIdentifier().createSibling(
            provideAvailableNameForGenTOBuilder(parentUnionGenTOBuilder.getName()));
        final List<GeneratedTOBuilder> subUnionGenTOBUilders = provideGeneratedTOBuildersForUnionTypeDef(
            newTOBuilderName, unionSubtype, parentNode);

        final GeneratedPropertyBuilder propertyBuilder;
        propertyBuilder = parentUnionGenTOBuilder.addProperty(BindingMapping.getPropertyName(
            newTOBuilderName.simpleName()));
        propertyBuilder.setReturnType(subUnionGenTOBUilders.get(0).build());
        parentUnionGenTOBuilder.addEqualsIdentity(propertyBuilder);
        parentUnionGenTOBuilder.addToStringProperty(propertyBuilder);

        return subUnionGenTOBUilders;
    }

    /**
     * Wraps code which handle case when union subtype is of the type <code>ExtendedType</code>. If TO for this type
     * already exists it is used for the creation of the property in <code>parentUnionGenTOBuilder</code>. Otherwise
     * the base type is used for the property creation.
     *
     * @param parentUnionGenTOBuilder generated TO builder in which new property is created
     * @param unionSubtype type definition of the <code>ExtendedType</code> type which represents union subtype
     * @param expressions list of strings with the regular expressions
     * @param parentNode parent Schema Node for Extended Subtype
     */
    private void resolveExtendedSubtypeAsUnion(final GeneratedTOBuilder parentUnionGenTOBuilder,
            final TypeDefinition<?> unionSubtype, final Map<String, String> expressions, final SchemaNode parentNode) {
        final String unionTypeName = unionSubtype.getQName().getLocalName();
        final Type genTO = findGenTO(unionTypeName, unionSubtype);
        if (genTO != null) {
            updateUnionTypeAsProperty(parentUnionGenTOBuilder, genTO, genTO.getName());
            return;
        }

        final TypeDefinition<?> baseType = baseTypeDefForExtendedType(unionSubtype);
        if (unionTypeName.equals(baseType.getQName().getLocalName())) {
            final Type javaType = BaseYangTypesProvider.INSTANCE.javaTypeForSchemaDefinitionType(baseType, parentNode,
                BindingGeneratorUtil.getRestrictions(unionSubtype));
            if (javaType != null) {
                updateUnionTypeAsProperty(parentUnionGenTOBuilder, javaType, unionTypeName);
            }
        } else if (baseType instanceof LeafrefTypeDefinition) {
            final Type javaType = javaTypeForSchemaDefinitionType(baseType, parentNode);
            boolean typeExist = false;
            for (GeneratedPropertyBuilder generatedPropertyBuilder : parentUnionGenTOBuilder.getProperties()) {
                final Type origType = ((GeneratedPropertyBuilderImpl) generatedPropertyBuilder).getReturnType();
                if (origType != null && javaType != null && javaType == origType) {
                    typeExist = true;
                    break;
                }
            }
            if (!typeExist && javaType != null) {
                updateUnionTypeAsProperty(parentUnionGenTOBuilder, javaType,
                    javaType.getName() + parentUnionGenTOBuilder.getName() + "Value");
            }
        }
        if (baseType instanceof StringTypeDefinition) {
            expressions.putAll(resolveRegExpressionsFromTypedef(unionSubtype));
        }
    }

    /**
     * Searches for generated TO for <code>searchedTypeDef</code> type  definition
     * in {@link #genTypeDefsContextMap genTypeDefsContextMap}.
     *
     * @param searchedTypeName string with name of <code>searchedTypeDef</code>
     * @return generated TO for <code>searchedTypeDef</code> or <code>null</code> it it doesn't exist
     */
    private Type findGenTO(final String searchedTypeName, final SchemaNode parentNode) {
        final Module typeModule = findParentModule(schemaContext, parentNode);
        if (typeModule != null && typeModule.getName() != null) {
            final Map<Optional<Revision>, Map<String, Type>> modulesByDate = genTypeDefsContextMap.get(
                typeModule.getName());
            final Map<String, Type> genTOs = modulesByDate.get(typeModule.getRevision());
            if (genTOs != null) {
                return genTOs.get(searchedTypeName);
            }
        }
        return null;
    }

    /**
     * Stores generated TO created from <code>genTOBuilder</code> for <code>newTypeDef</code>
     * to {@link #genTypeDefsContextMap genTypeDefsContextMap} if the module for <code>newTypeDef</code> exists.
     *
     * @param newTypeDef type definition for which is <code>genTOBuilder</code> created
     * @param genTOBuilder generated TO builder which is converted to generated TO and stored
     */
    private void storeGenTO(final TypeDefinition<?> newTypeDef, final GeneratedTOBuilder genTOBuilder,
            final SchemaNode parentNode) {
        if (!(newTypeDef instanceof UnionTypeDefinition)) {
            final Module parentModule = findParentModule(schemaContext, parentNode);
            if (parentModule != null && parentModule.getName() != null) {
                final Map<Optional<Revision>, Map<String, Type>> modulesByDate = genTypeDefsContextMap.get(
                    parentModule.getName());
                final Map<String, Type> genTOsMap = modulesByDate.get(parentModule.getRevision());
                genTOsMap.put(newTypeDef.getQName().getLocalName(), genTOBuilder.build());
            }
        }
    }

    /**
     * Adds a new property with the name <code>propertyName</code> and with type <code>type</code>
     * to <code>unonGenTransObject</code>.
     *
     * @param unionGenTransObject generated TO to which should be property added
     * @param type JAVA <code>type</code> of the property which should be added to <code>unionGentransObject</code>
     * @param propertyName string with name of property which should be added to <code>unionGentransObject</code>
     */
    private static void updateUnionTypeAsProperty(final GeneratedTOBuilder unionGenTransObject, final Type type,
            final String propertyName) {
        if (unionGenTransObject != null && type != null && !unionGenTransObject.containsProperty(propertyName)) {
            final GeneratedPropertyBuilder propBuilder = unionGenTransObject
                    .addProperty(BindingMapping.getPropertyName(propertyName));
            propBuilder.setReturnType(type);

            unionGenTransObject.addEqualsIdentity(propBuilder);
            unionGenTransObject.addHashIdentity(propBuilder);
            unionGenTransObject.addToStringProperty(propBuilder);
        }
    }

    /**
     * Converts <code>typedef</code> to the generated TO builder.
     *
     * @param basePackageName string with name of package to which the module belongs
     * @param typedef type definition from which is the generated TO builder created
     * @return generated TO builder which contains data from <code>typedef</code> and <code>basePackageName</code>
     */
    private GeneratedTOBuilder typedefToTransferObject(final String basePackageName,
            final TypeDefinition<?> typedef, final String moduleName) {
        JavaTypeName name = renames.get(typedef);
        if (name == null) {
            name = JavaTypeName.create(
                BindingGeneratorUtil.packageNameForGeneratedType(basePackageName, typedef.getPath()),
                BindingMapping.getClassName(typedef.getQName().getLocalName()));
        }

        final GeneratedTOBuilder newType = newGeneratedTOBuilder(name);
        newType.setSchemaPath(typedef.getPath());
        newType.setModuleName(moduleName);
        addCodegenInformation(newType, typedef);
        return newType;
    }

    /**
     * Converts <code>typeDef</code> which should be of the type <code>BitsTypeDefinition</code>
     * to <code>GeneratedTOBuilder</code>. All the bits of the typeDef are added to returning generated TO as
     * properties.
     *
     * @param typeName new type identifier
     * @param typeDef type definition from which is the generated TO builder created
     * @return generated TO builder which represents <code>typeDef</code>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if <code>typeDef</code> equals null</li>
     *             <li>if <code>basePackageName</code> equals null</li>
     *             </ul>
     */
    public GeneratedTOBuilder provideGeneratedTOBuilderForBitsTypeDefinition(final JavaTypeName typeName,
            final BitsTypeDefinition typeDef, final String moduleName) {
        final GeneratedTOBuilder genTOBuilder = newGeneratedTOBuilder(typeName);
        genTOBuilder.setSchemaPath(typeDef.getPath());
        genTOBuilder.setModuleName(moduleName);
        genTOBuilder.setBaseType(typeDef);
        genTOBuilder.addImplementsType(TYPE_OBJECT);
        addCodegenInformation(genTOBuilder, typeDef);

        for (Bit bit : typeDef.getBits()) {
            final String name = bit.getName();
            GeneratedPropertyBuilder genPropertyBuilder = genTOBuilder.addProperty(
                BindingMapping.getPropertyName(name));
            genPropertyBuilder.setReadOnly(true);
            genPropertyBuilder.setReturnType(BaseYangTypes.BOOLEAN_TYPE);

            genTOBuilder.addEqualsIdentity(genPropertyBuilder);
            genTOBuilder.addHashIdentity(genPropertyBuilder);
            genTOBuilder.addToStringProperty(genPropertyBuilder);
        }

        return genTOBuilder;
    }

    /**
     * Adds to the <code>genTOBuilder</code> the constant which contains regular expressions from
     * the <code>regularExpressions</code>.
     *
     * @param genTOBuilder generated TO builder to which are <code>regular expressions</code> added
     * @param expressions list of string which represent regular expressions
     */
    private static void addStringRegExAsConstant(final GeneratedTOBuilder genTOBuilder,
            final Map<String, String> expressions) {
        if (!expressions.isEmpty()) {
            genTOBuilder.addConstant(Types.listTypeFor(BaseYangTypes.STRING_TYPE), TypeConstants.PATTERN_CONSTANT_NAME,
                ImmutableMap.copyOf(expressions));
        }
    }

    /**
     * Creates generated TO with data about inner extended type <code>innerExtendedType</code>, about the package name
     * <code>typedefName</code> and about the generated TO name <code>typedefName</code>.
     *
     * <p>
     * It is assumed that <code>innerExtendedType</code> is already present in
     * {@link AbstractTypeProvider#genTypeDefsContextMap genTypeDefsContextMap} to be possible set it as extended type
     * for the returning generated TO.
     *
     * @param typedef Type Definition
     * @param innerExtendedType extended type which is part of some other extended type
     * @param basePackageName string with the package name of the module
     * @param moduleName Module Name
     * @return generated TO which extends generated TO for <code>innerExtendedType</code>
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>if <code>extendedType</code> equals null</li>
     *             <li>if <code>basePackageName</code> equals null</li>
     *             <li>if <code>typedefName</code> equals null</li>
     *             </ul>
     */
    private GeneratedTransferObject provideGeneratedTOFromExtendedType(final TypeDefinition<?> typedef,
            final TypeDefinition<?> innerExtendedType, final String basePackageName, final String moduleName) {
        Preconditions.checkArgument(innerExtendedType != null, "Extended type cannot be NULL!");
        Preconditions.checkArgument(basePackageName != null, "String with base package name cannot be NULL!");

        final GeneratedTOBuilder genTOBuilder = newGeneratedTOBuilder(JavaTypeName.create(basePackageName,
            BindingMapping.getClassName(typedef.getQName())));
        genTOBuilder.setSchemaPath(typedef.getPath());
        genTOBuilder.setModuleName(moduleName);
        genTOBuilder.setTypedef(true);
        addCodegenInformation(genTOBuilder, typedef);

        final Restrictions r = BindingGeneratorUtil.getRestrictions(typedef);
        genTOBuilder.setRestrictions(r);
        addStringRegExAsConstant(genTOBuilder, resolveRegExpressionsFromTypedef(typedef));

        if (typedef.getStatus() == Status.DEPRECATED) {
            genTOBuilder.addAnnotation("java.lang", "Deprecated");
        }

        if (baseTypeDefForExtendedType(innerExtendedType) instanceof UnionTypeDefinition) {
            genTOBuilder.setIsUnion(true);
        }

        Map<Optional<Revision>, Map<String, Type>> modulesByDate = null;
        Map<String, Type> typeMap = null;
        final Module parentModule = findParentModule(schemaContext, innerExtendedType);
        if (parentModule != null) {
            modulesByDate = genTypeDefsContextMap.get(parentModule.getName());
            typeMap = modulesByDate.get(parentModule.getRevision());
        }

        if (typeMap != null) {
            final String innerTypeDef = innerExtendedType.getQName().getLocalName();
            final Type type = typeMap.get(innerTypeDef);
            if (type instanceof GeneratedTransferObject) {
                genTOBuilder.setExtendsType((GeneratedTransferObject) type);
            }
        }
        addUnitsToGenTO(genTOBuilder, typedef.getUnits().orElse(null));
        makeSerializable(genTOBuilder);

        return genTOBuilder.build();
    }

    /**
     * Add {@link java.io.Serializable} to implemented interfaces of this TO. Also compute and add serialVersionUID
     * property.
     *
     * @param gto transfer object which needs to be made serializable
     */
    private static void makeSerializable(final GeneratedTOBuilder gto) {
        gto.addImplementsType(Types.serializableType());
        final GeneratedPropertyBuilder prop = new GeneratedPropertyBuilderImpl("serialVersionUID");
        prop.setValue(Long.toString(BindingGeneratorUtil.computeDefaultSUID(gto)));
        gto.setSUID(prop);
    }

    /**
     * Finds out for each type definition how many immersion (depth) is necessary to get to the base type. Every type
     * definition is inserted to the map which key is depth and value is list of type definitions with equal depth.
     * In next step are lists from this map concatenated to one list in ascending order according to their depth. All
     * type definitions are in the list behind all type definitions on which depends.
     *
     * @param unsortedTypeDefinitions list of type definitions which should be sorted by depth
     * @return list of type definitions sorted according their each other dependencies (type definitions which are
     *              dependent on other type definitions are in list behind them).
     */
    private static List<TypeDefinition<?>> sortTypeDefinitionAccordingDepth(
            final Collection<TypeDefinition<?>> unsortedTypeDefinitions) {
        final List<TypeDefinition<?>> sortedTypeDefinition = new ArrayList<>();

        final Map<Integer, List<TypeDefinition<?>>> typeDefinitionsDepths = new TreeMap<>();
        for (TypeDefinition<?> unsortedTypeDefinition : unsortedTypeDefinitions) {
            final Integer depth = getTypeDefinitionDepth(unsortedTypeDefinition);
            List<TypeDefinition<?>> typeDefinitionsConcreteDepth =
                typeDefinitionsDepths.computeIfAbsent(depth, k -> new ArrayList<>());
            typeDefinitionsConcreteDepth.add(unsortedTypeDefinition);
        }

        // SortedMap guarantees order corresponding to keys in ascending order
        for (List<TypeDefinition<?>> v : typeDefinitionsDepths.values()) {
            sortedTypeDefinition.addAll(v);
        }

        return sortedTypeDefinition;
    }

    /**
     * Returns how many immersion is necessary to get from the type definition to the base type.
     *
     * @param typeDefinition type definition for which is depth sought.
     * @return number of immersions which are necessary to get from the type definition to the base type
     */
    private static int getTypeDefinitionDepth(final TypeDefinition<?> typeDefinition) {
        // FIXME: rewrite this in a non-recursive manner
        if (typeDefinition == null) {
            return 1;
        }
        final TypeDefinition<?> baseType = typeDefinition.getBaseType();
        if (baseType == null) {
            return 1;
        }

        int depth = 1;
        if (baseType.getBaseType() != null) {
            depth = depth + getTypeDefinitionDepth(baseType);
        } else if (baseType instanceof UnionTypeDefinition) {
            final List<TypeDefinition<?>> childTypeDefinitions = ((UnionTypeDefinition) baseType).getTypes();
            int maxChildDepth = 0;
            int childDepth = 1;
            for (TypeDefinition<?> childTypeDefinition : childTypeDefinitions) {
                childDepth = childDepth + getTypeDefinitionDepth(childTypeDefinition);
                if (childDepth > maxChildDepth) {
                    maxChildDepth = childDepth;
                }
            }
            return maxChildDepth;
        }
        return depth;
    }

    /**
     * Returns string which contains the same value as <code>name</code> but integer suffix is incremented by one. If
     * <code>name</code> contains no number suffix, a new suffix initialized at 1 is added. A suffix is actually
     * composed of a '$' marker, which is safe, as no YANG identifier can contain '$', and a unsigned decimal integer.
     *
     * @param name string with name of augmented node
     * @return string with the number suffix incremented by one (or 1 is added)
     */
    private static String provideAvailableNameForGenTOBuilder(final String name) {
        final int dollar = name.indexOf('$');
        if (dollar == -1) {
            return name + "$1";
        }

        final int newSuffix = Integer.parseUnsignedInt(name.substring(dollar + 1)) + 1;
        Preconditions.checkState(newSuffix > 0, "Suffix counter overflow");
        return name.substring(0, dollar + 1) + newSuffix;
    }

    public static void addUnitsToGenTO(final GeneratedTOBuilder to, final String units) {
        if (!Strings.isNullOrEmpty(units)) {
            to.addConstant(Types.STRING, "_UNITS", "\"" + units + "\"");
            final GeneratedPropertyBuilder prop = new GeneratedPropertyBuilderImpl("UNITS");
            prop.setReturnType(Types.STRING);
            to.addToStringProperty(prop);
        }
    }

    @Override
    public String getTypeDefaultConstruction(final LeafSchemaNode node) {
        return getTypeDefaultConstruction(node, (String) node.getType().getDefaultValue().orElse(null));
    }

    public String getTypeDefaultConstruction(final LeafSchemaNode node, final String defaultValue) {
        final TypeDefinition<?> type = CompatUtils.compatType(node);
        final QName typeQName = type.getQName();
        final TypeDefinition<?> base = baseTypeDefForExtendedType(type);
        requireNonNull(type, () -> "Cannot provide default construction for null type of " + node);
        requireNonNull(defaultValue, () -> "Cannot provide default construction for null default statement of "
            + node);

        final StringBuilder sb = new StringBuilder();
        String result = null;
        if (base instanceof BinaryTypeDefinition) {
            result = binaryToDef(defaultValue);
        } else if (base instanceof BitsTypeDefinition) {
            String parentName;
            String className;
            final Module parent = getParentModule(node);
            final Iterator<QName> path = node.getPath().getPathFromRoot().iterator();
            path.next();
            if (!path.hasNext()) {
                parentName = BindingMapping.getClassName(parent.getName()) + "Data";
                final String basePackageName = BindingMapping.getRootPackageName(parent.getQNameModule());
                className = basePackageName + "." + parentName + "." + BindingMapping.getClassName(node.getQName());
            } else {
                final String basePackageName = BindingMapping.getRootPackageName(parent.getQNameModule());
                final String packageName = BindingGeneratorUtil.packageNameForGeneratedType(basePackageName,
                    type.getPath());
                parentName = BindingMapping.getClassName(parent.getName());
                className = packageName + "." + parentName + "." + BindingMapping.getClassName(node.getQName());
            }
            result = bitsToDef((BitsTypeDefinition) base, className, defaultValue, type.getBaseType() != null);
        } else if (base instanceof BooleanTypeDefinition) {
            result = typeToBooleanDef(defaultValue);
        } else if (base instanceof DecimalTypeDefinition) {
            result = typeToDef(BigDecimal.class, defaultValue);
        } else if (base instanceof EmptyTypeDefinition) {
            result = typeToBooleanDef(defaultValue);
        } else if (base instanceof EnumTypeDefinition) {
            final char[] defValArray = defaultValue.toCharArray();
            final char first = Character.toUpperCase(defaultValue.charAt(0));
            defValArray[0] = first;
            final String newDefVal = new String(defValArray);
            String className;
            if (type.getBaseType() != null) {
                final Module m = getParentModule(type);
                final String basePackageName = BindingMapping.getRootPackageName(m.getQNameModule());
                final String packageName = BindingGeneratorUtil.packageNameForGeneratedType(basePackageName,
                    type.getPath());
                className = packageName + "." + BindingMapping.getClassName(typeQName);
            } else {
                final Module parentModule = getParentModule(node);
                final String basePackageName = BindingMapping.getRootPackageName(parentModule.getQNameModule());
                final String packageName = BindingGeneratorUtil.packageNameForGeneratedType(basePackageName,
                    node.getPath());
                className = packageName + "." + BindingMapping.getClassName(node.getQName());
            }
            result = className + "." + newDefVal;
        } else if (base instanceof IdentityrefTypeDefinition) {
            throw new UnsupportedOperationException("Cannot get default construction for identityref type");
        } else if (base instanceof InstanceIdentifierTypeDefinition) {
            throw new UnsupportedOperationException("Cannot get default construction for instance-identifier type");
        } else if (BaseTypes.isInt8(base)) {
            result = typeToValueOfDef(Byte.class, defaultValue);
        } else if (BaseTypes.isInt16(base)) {
            result = typeToValueOfDef(Short.class, defaultValue);
        } else if (BaseTypes.isInt32(base)) {
            result = typeToValueOfDef(Integer.class, defaultValue);
        } else if (BaseTypes.isInt64(base)) {
            result = typeToValueOfDef(Long.class, defaultValue);
        } else if (base instanceof LeafrefTypeDefinition) {
            result = leafrefToDef(node, (LeafrefTypeDefinition) base, defaultValue);
        } else if (base instanceof StringTypeDefinition) {
            result = "\"" + defaultValue + "\"";
        } else if (BaseTypes.isUint8(base)) {
            result = typeToValueOfDef(Uint8.class, defaultValue);
        } else if (BaseTypes.isUint16(base)) {
            result = typeToValueOfDef(Uint16.class, defaultValue);
        } else if (BaseTypes.isUint32(base)) {
            result = typeToValueOfDef(Uint32.class, defaultValue);
        } else if (BaseTypes.isUint64(base)) {
            result = typeToValueOfDef(Uint64.class, defaultValue);
        } else if (base instanceof UnionTypeDefinition) {
            result = unionToDef(node);
        } else {
            result = "";
        }
        sb.append(result);

        if (type.getBaseType() != null && !(base instanceof LeafrefTypeDefinition)
                && !(base instanceof EnumTypeDefinition) && !(base instanceof UnionTypeDefinition)) {
            final Module m = getParentModule(type);
            final String basePackageName = BindingMapping.getRootPackageName(m.getQNameModule());
            final String packageName = BindingGeneratorUtil.packageNameForGeneratedType(basePackageName,
                type.getPath());
            final String className = packageName + "." + BindingMapping.getClassName(typeQName);
            sb.insert(0, "new " + className + "(");
            sb.insert(sb.length(), ')');
        }

        return sb.toString();
    }

    private static String typeToDef(final Class<?> clazz, final String defaultValue) {
        return "new " + clazz.getName() + "(\"" + defaultValue + "\")";
    }

    private static String typeToValueOfDef(final Class<?> clazz, final String defaultValue) {
        return clazz.getName() + ".valueOf(\"" + defaultValue + "\")";
    }

    private static String typeToBooleanDef(final String defaultValue) {
        switch (defaultValue) {
            case "false":
                return "java.lang.Boolean.FALSE";
            case "true":
                return "java.lang.Boolean.TRUE";
            default:
                return typeToValueOfDef(Boolean.class, defaultValue);
        }
    }

    private static String binaryToDef(final String defaultValue) {
        final StringBuilder sb = new StringBuilder();
        final byte[] encoded = Base64.getDecoder().decode(defaultValue);
        sb.append("new byte[] {");
        for (int i = 0; i < encoded.length; i++) {
            sb.append(encoded[i]);
            if (i != encoded.length - 1) {
                sb.append(", ");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static final Comparator<Bit> BIT_NAME_COMPARATOR = Comparator.comparing(Bit::getName);

    private static String bitsToDef(final BitsTypeDefinition type, final String className, final String defaultValue,
            final boolean isExt) {
        final List<Bit> bits = new ArrayList<>(type.getBits());
        bits.sort(BIT_NAME_COMPARATOR);
        final StringBuilder sb = new StringBuilder();
        if (!isExt) {
            sb.append("new ");
            sb.append(className);
            sb.append('(');
        }
        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i).getName().equals(defaultValue)) {
                sb.append(true);
            } else {
                sb.append(false);
            }
            if (i != bits.size() - 1) {
                sb.append(", ");
            }
        }
        if (!isExt) {
            sb.append(')');
        }
        return sb.toString();
    }

    private Module getParentModule(final SchemaNode node) {
        final QName qname = node.getPath().getPathFromRoot().iterator().next();
        return schemaContext.findModule(qname.getModule()).orElse(null);
    }

    private String leafrefToDef(final LeafSchemaNode parentNode, final LeafrefTypeDefinition leafrefType,
            final String defaultValue) {
        Preconditions.checkArgument(leafrefType != null, "Leafref Type Definition reference cannot be NULL!");
        Preconditions.checkArgument(leafrefType.getPathStatement() != null,
                "The Path Statement for Leafref Type Definition cannot be NULL!");

        final PathExpression xpath = leafrefType.getPathStatement();
        final String strXPath = xpath.getOriginalString();

        if (strXPath != null) {
            if (strXPath.indexOf('[') == -1) {
                final Module module = findParentModule(schemaContext, parentNode);
                if (module != null) {
                    final SchemaNode dataNode;
                    if (xpath.isAbsolute()) {
                        dataNode = findDataTreeSchemaNode(schemaContext, module.getQNameModule(), xpath);
                    } else {
                        dataNode = findDataSchemaNodeForRelativeXPath(schemaContext, module, parentNode, xpath);
                    }
                    final String result = getTypeDefaultConstruction((LeafSchemaNode) dataNode, defaultValue);
                    return result;
                }
            } else {
                return "new java.lang.Object()";
            }
        }

        return null;
    }

    private String unionToDef(final LeafSchemaNode node) {
        final TypeDefinition<?> type = CompatUtils.compatType(node);
        String parentName;
        String className;

        if (type.getBaseType() != null) {
            final QName typeQName = type.getQName();
            Module module = null;
            final Collection<? extends Module> modules = schemaContext.findModules(typeQName.getNamespace());
            if (modules.size() > 1) {
                for (Module m : modules) {
                    if (m.getRevision().equals(typeQName.getRevision())) {
                        module = m;
                        break;
                    }
                }
                if (module == null) {
                    final List<Module> modulesList = new ArrayList<>(modules);
                    modulesList.sort((o1, o2) -> Revision.compare(o1.getRevision(), o2.getRevision()));
                    module = modulesList.get(0);
                }
            } else {
                module = modules.iterator().next();
            }

            final String basePackageName = BindingMapping.getRootPackageName(module.getQNameModule());
            className = basePackageName + "." + BindingMapping.getClassName(typeQName);
        } else {
            final Iterator<QName> path = node.getPath().getPathFromRoot().iterator();
            final QName first = path.next();
            final Module parent = schemaContext.findModule(first.getModule()).orElse(null);
            final String basePackageName = BindingMapping.getRootPackageName(parent.getQNameModule());
            if (!path.hasNext()) {
                parentName = BindingMapping.getClassName(parent.getName()) + "Data";
                className = basePackageName + "." + parentName + "." + BindingMapping.getClassName(node.getQName());
            } else {
                final String packageName = BindingGeneratorUtil.packageNameForGeneratedType(basePackageName,
                    UNION_PATH);
                className = packageName + "." + BindingMapping.getClassName(node.getQName());
            }
        }
        return union(className, (String) node.getType().getDefaultValue().orElse(null), node);
    }

    private static String union(final String className, final String defaultValue, final LeafSchemaNode node) {
        return new StringBuilder()
                .append("new ")
                .append(className)
                .append("(\"")
                .append(defaultValue)
                .append("\".toCharArray())")
                .toString();
    }

    @Override
    public String getConstructorPropertyName(final SchemaNode node) {
        return node instanceof TypeDefinition<?> ? TypeConstants.VALUE_PROP : "";
    }

    @Override
    public String getParamNameFromType(final TypeDefinition<?> type) {
        return BindingMapping.getPropertyName(type.getQName().getLocalName());
    }
}
