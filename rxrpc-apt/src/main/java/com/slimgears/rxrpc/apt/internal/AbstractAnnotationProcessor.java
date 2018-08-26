package com.slimgears.rxrpc.apt.internal;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.slimgears.rxrpc.apt.data.Environment;
import com.slimgears.rxrpc.apt.util.LogUtils;
import com.slimgears.util.stream.Safe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;
import java.util.stream.Stream;

import static com.slimgears.util.stream.Streams.ofType;

public abstract class AbstractAnnotationProcessor extends AbstractProcessor {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Injector injector = Guice.createInjector(new EnvironmentModule(processingEnv));
        injector.injectMembers(this);

        try (LogUtils.SelfClosable ignored1 = LogUtils.applyLogging(processingEnv);
             Safe.Closable ignored2 = Environment.withEnvironment(processingEnv, roundEnv)) {
            onStart();

            return annotations
                    .stream()
                    .map(a -> processAnnotation(a, roundEnv))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);
        } finally {
            if (annotations.isEmpty()) {
                onComplete();
            }
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return processingEnv.getSourceVersion();
    }

    protected void onStart() {
    }

    protected void onComplete() {
    }

    protected boolean processAnnotation(TypeElement annotationType, RoundEnvironment roundEnv) {
        return roundEnv.getElementsAnnotatedWith(annotationType)
                .stream()
                .flatMap(e -> Stream
                        .of(
                                ofType(TypeElement.class, Stream.of(e)).map(_e -> processType(annotationType, _e)),
                                ofType(ExecutableElement.class, Stream.of(e)).map(_e -> processMethod(annotationType, _e)),
                                ofType(VariableElement.class, Stream.of(e)).map(_e -> processField(annotationType, _e)))
                        .flatMap(s -> s))
                .reduce(Boolean::logicalOr)
                .orElse(false);
    }

    protected boolean processAnnotation(Class annotationType, RoundEnvironment roundEnv) {
        return processAnnotation(
                processingEnv.getElementUtils().getTypeElement(annotationType.getCanonicalName()),
                roundEnv);
    }

    protected boolean processType(TypeElement annotationType, TypeElement typeElement) { return false; }
    protected boolean processMethod(TypeElement annotationType, ExecutableElement methodElement) { return false; }
    protected boolean processField(TypeElement annotationType, VariableElement variableElement) { return false; }
}
