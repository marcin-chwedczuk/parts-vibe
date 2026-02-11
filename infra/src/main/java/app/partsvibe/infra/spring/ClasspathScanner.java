package app.partsvibe.infra.spring;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class ClasspathScanner {
    private final ListableBeanFactory beanFactory;

    public ClasspathScanner(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        List<String> basePackages = AutoConfigurationPackages.get(beanFactory);
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotationClass));

        Set<Class<?>> classes = new LinkedHashSet<>();
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        for (String basePackage : basePackages) {
            scanner.findCandidateComponents(basePackage).forEach(candidate -> {
                String className = candidate.getBeanClassName();
                if (className == null || className.isBlank()) {
                    return;
                }
                try {
                    classes.add(ClassUtils.forName(className, classLoader));
                } catch (ClassNotFoundException ex) {
                    throw new IllegalStateException("Failed to load annotated class: " + className, ex);
                }
            });
        }
        return Set.copyOf(classes);
    }
}
