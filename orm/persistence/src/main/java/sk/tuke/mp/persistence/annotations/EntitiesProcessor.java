package sk.tuke.mp.persistence.annotations;

import com.squareup.javapoet.*;
import sk.tuke.mp.persistence.infrastructure.IModelBuilder;
import sk.tuke.mp.persistence.infrastructure.PropertyAnnotations;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
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

            configureMethodBuilder.addCode(createCodeBlock(typeElement));
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
    private CodeBlock createCodeBlock(TypeElement typeElement)
    {
        Entity ea = typeElement.getAnnotation(Entity.class);
        CodeBlock.Builder builder = CodeBlock.builder();
        for(Element elem : typeElement.getEnclosedElements())
        {
            if(elem.getKind() != ElementKind.FIELD) continue;

            VariableElement field = (VariableElement)elem;
            Column column = field.getAnnotation(Column.class);
            Id id = field.getAnnotation(Id.class);
            LazyFetch lazy = field.getAnnotation(LazyFetch.class);

            StringBuilder propConfig = new StringBuilder();
            if(!Objects.equals(column.name(), ""))
            {
                propConfig.append(".setColumnName(\"").append(column.name()).append("\")");
            }
            if(id != null)
                propConfig.append(".setPrimaryKey()");
            if(lazy != null)
            {
                try {
                    lazy.targetEntity();
                } catch (MirroredTypeException e)
                {
                    propConfig.append(".setLazyImplementation(").append(e.getTypeMirror().toString()).append(".class").append(")");
                }
            }
            if(column.required())
            {
                propConfig.append(".setRequired()");
            }
            if(!Objects.equals(column.getter(), ""))
            {
                //TODO
            }
            if(!Objects.equals(column.setter(), ""))
            {
                //TODO
            }
            propConfig.append(".setAnnotation(\"").append(PropertyAnnotations.MAX_LENGTH).append("\",").append(column.maxLength()).append(")");

            builder.addStatement("modelBuilder.entity($S, $S).property($S, $S)" + propConfig.toString(), typeElement.getQualifiedName(), ea.name(), toCodeName(field.asType()), field.getSimpleName().toString());
        }

        return builder.build();
    }
    private String toCodeName(TypeMirror type)
    {
        if(type.getKind().isPrimitive())
        {
            switch(type.getKind())
            {
                case INT:
                    return "java.lang.Integer";
                case DOUBLE:
                    return "java.lang.Double";
                default:
                    return "ERROR_UNSUPPORTED_TYPE";
            }
        }
        else
        {
            return type.toString();
        }
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }
}
