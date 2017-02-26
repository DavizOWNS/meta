package sk.tuke.mp.persistence.annotations;

import com.squareup.javapoet.*;
import sk.tuke.mp.persistence.infrastructure.IModelBuilder;
import sk.tuke.mp.persistence.infrastructure.PropertyAnnotations;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Set;

/**
 * Created by DAVID on 25.2.2017.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
        "sk.tuke.mp.persistence.annotations.Entity",
        "sk.tuke.mp.persistence.annotations.Column",
        "sk.tuke.mp.persistence.annotations.LazyFetch",
        "sk.tuke.mp.persistence.annotations.Id"
})
public class EntitiesProcessor extends AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    public EntitiesProcessor()
    {
        super();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    /*@Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new LinkedHashSet<String>();
        annotataions.add(Entity.class.getCanonicalName());
        return annotataions;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }*/

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing database model annotations...");

        MethodSpec.Builder configureMethodBuilder = createConfigureModelMethod();
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Entity annotation found in " + annotatedElement.getSimpleName());

            TypeElement typeElement = (TypeElement) annotatedElement;

            StringBuilder createTableComment = new StringBuilder();
            createTableComment.append("/*\n");
            CodeBlock codeBlock = createCodeBlock(typeElement, roundEnv, createTableComment);
            createTableComment.append("*/\n");

            configureMethodBuilder.addCode(createTableComment.toString());
            configureMethodBuilder.addCode(codeBlock);
            configureMethodBuilder.addCode("\n");
        }

        JavaFile javaFile = JavaFile.builder("sk.tuke.mp.persistence.generated",
                TypeSpec.classBuilder("ModelSnapshot")
                        .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                        .addMethod(configureMethodBuilder.build())
                        .build())
                .build();

        try {
            JavaFileObject jfo = filer.createSourceFile("ModelSnapshot");
            messager.printMessage(Diagnostic.Kind.NOTE, "Writing generated class to: " + jfo.toUri().toString());
            Writer writer = jfo.openWriter();
            javaFile.writeTo(writer);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private MethodSpec.Builder createConfigureModelMethod()
    {
        return MethodSpec.methodBuilder("configureModel")
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addParameter(IModelBuilder.class, "modelBuilder");
    }
    private CodeBlock createCodeBlock(TypeElement typeElement, RoundEnvironment roundEnv, StringBuilder queryBuilder)
    {
        Entity ea = typeElement.getAnnotation(Entity.class);
        CodeBlock.Builder builder = CodeBlock.builder();
        queryBuilder.append("CREATE TABLE ").append(ea.name()).append(" (\n");
        for(Element elem : typeElement.getEnclosedElements())
        {
            if(elem.getKind() != ElementKind.FIELD) continue;

            VariableElement field = (VariableElement)elem;
            Column column = field.getAnnotation(Column.class);
            Id id = field.getAnnotation(Id.class);
            LazyFetch lazy = field.getAnnotation(LazyFetch.class);

            StringBuilder propConfig = new StringBuilder();
            String columnName = field.getSimpleName().toString();
            if(!Objects.equals(column.name(), ""))
            {
                propConfig.append(".setColumnName(\"").append(column.name()).append("\")");
                columnName = column.name();
            }

            String sqlType = null;
            switch(field.asType().toString())
            {
                case "java.lang.Integer":
                    sqlType = "INT";
                    break;
                case "java.lang.Double":
                    sqlType = "DOUBLE";
                    break;
                case "java.lang.String":
                    if(column.maxLength() != 0)
                        sqlType = "VARCHAR(" + column.maxLength() + ")";
                    else
                        sqlType = "TEXT";
                    break;
                default:
                    sqlType = "INT";
            }
            queryBuilder.append("\t").append(columnName).append(" ").append(sqlType);

            if(column.required())
            {
                propConfig.append(".setRequired()");
                queryBuilder.append(" NOT NULL");
            }
            if(id != null) {
                propConfig.append(".setPrimaryKey()");
                queryBuilder.append(" PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)");
            }
            if(field.asType().getKind() == TypeKind.DECLARED)
            {
                String className = null;
                if(lazy != null)
                {
                    try {
                        lazy.targetEntity();
                    } catch (MirroredTypeException e)
                    {
                        className = e.getTypeMirror().toString();
                        propConfig.append(".setLazyImplementation(").append(className).append(".class").append(")");
                    }
                }

                if(className == null)
                    className = field.asType().toString();

                Element classElement = null;
                for(Element el : roundEnv.getElementsAnnotatedWith(Entity.class))
                {
                    TypeElement te = (TypeElement)el;
                    if(te.getQualifiedName().toString().equals(className)) {
                        classElement = te;
                        break;
                    }
                }
                if(classElement != null) {
                    String entityName = classElement.getAnnotation(Entity.class).name();
                    String propertyName = "";
                    propConfig.append(".references(\"").append(entityName).append("\", \"").append(propertyName).append("\")");
                    queryBuilder.append(" REFERENCES ").append(entityName);
                }
            }
            if(!Objects.equals(column.getter(), ""))
            {
                //TODO
            }
            if(!Objects.equals(column.setter(), ""))
            {
                //TODO
            }
            if(column.maxLength() < 0)
                messager.printMessage(Diagnostic.Kind.WARNING, "maxLength on column " + columnName + " is less than 0. Only values higher or equal to 0 are allowed.");
            propConfig.append(".setAnnotation(\"").append(PropertyAnnotations.MAX_LENGTH).append("\",").append(column.maxLength()).append(")");

            builder.addStatement("modelBuilder.entity($S, $S).property($S, $S)" + propConfig.toString(), typeElement.getQualifiedName(), ea.name(), toCodeName(field.asType()), field.getSimpleName().toString());
            queryBuilder.append("\n");
        }

        queryBuilder.append(")\n");
        return builder.build();
    }
    private String toCodeName(TypeMirror type)
    {
        switch(type.getKind())
        {
            case INT:
                return "java.lang.Integer";
            case DOUBLE:
                return "java.lang.Double";
            case DECLARED:
                return type.toString();
            default:
                return "ERROR_UNSUPPORTED_TYPE";
        }
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }
}
