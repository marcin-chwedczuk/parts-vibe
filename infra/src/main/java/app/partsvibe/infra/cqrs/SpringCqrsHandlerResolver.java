package app.partsvibe.infra.cqrs;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.CommandBehavior;
import app.partsvibe.shared.cqrs.CommandHandler;
import app.partsvibe.shared.cqrs.Query;
import app.partsvibe.shared.cqrs.QueryBehavior;
import app.partsvibe.shared.cqrs.QueryHandler;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Component
public class SpringCqrsHandlerResolver {
    private final ListableBeanFactory beanFactory;

    SpringCqrsHandlerResolver(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @SuppressWarnings("unchecked")
    <C extends Command<R>, R> CommandHandler<C, R> resolveCommandHandler(C command) {
        var commandType = command.getClass();
        var matchingBeanNames = findMatchingBeanNames(CommandHandler.class, commandType);

        if (matchingBeanNames.isEmpty()) {
            throw new CqrsHandlerResolutionException(
                    "No command handler found. commandClass=%s".formatted(commandType.getName()));
        }
        if (matchingBeanNames.size() > 1) {
            throw new CqrsHandlerResolutionException("Multiple command handlers found. commandClass=%s, handlerBeans=%s"
                    .formatted(commandType.getName(), matchingBeanNames));
        }

        return (CommandHandler<C, R>) beanFactory.getBean(matchingBeanNames.getFirst(), CommandHandler.class);
    }

    @SuppressWarnings("unchecked")
    <Q extends Query<R>, R> QueryHandler<Q, R> resolveQueryHandler(Q query) {
        var queryType = query.getClass();
        var matchingBeanNames = findMatchingBeanNames(QueryHandler.class, queryType);

        if (matchingBeanNames.isEmpty()) {
            throw new CqrsHandlerResolutionException(
                    "No query handler found. queryClass=%s".formatted(queryType.getName()));
        }
        if (matchingBeanNames.size() > 1) {
            throw new CqrsHandlerResolutionException("Multiple query handlers found. queryClass=%s, handlerBeans=%s"
                    .formatted(queryType.getName(), matchingBeanNames));
        }

        return (QueryHandler<Q, R>) beanFactory.getBean(matchingBeanNames.getFirst(), QueryHandler.class);
    }

    @SuppressWarnings("unchecked")
    <C extends Command<R>, R> List<CommandBehavior<C, R>> resolveCommandBehaviors(C command) {
        var matchingBeanNames = findMatchingBeanNames(CommandBehavior.class, command.getClass());
        var behaviors = new ArrayList<CommandBehavior<C, R>>();
        for (var beanName : matchingBeanNames) {
            behaviors.add((CommandBehavior<C, R>) beanFactory.getBean(beanName, CommandBehavior.class));
        }
        AnnotationAwareOrderComparator.sort(behaviors);
        return List.copyOf(behaviors);
    }

    @SuppressWarnings("unchecked")
    <Q extends Query<R>, R> List<QueryBehavior<Q, R>> resolveQueryBehaviors(Q query) {
        var matchingBeanNames = findMatchingBeanNames(QueryBehavior.class, query.getClass());
        var behaviors = new ArrayList<QueryBehavior<Q, R>>();
        for (var beanName : matchingBeanNames) {
            behaviors.add((QueryBehavior<Q, R>) beanFactory.getBean(beanName, QueryBehavior.class));
        }
        AnnotationAwareOrderComparator.sort(behaviors);
        return List.copyOf(behaviors);
    }

    private List<String> findMatchingBeanNames(Class<?> rawType, Class<?> requestType) {
        var matchingBeanNames = new ArrayList<String>();
        for (var beanName : beanFactory.getBeanNamesForType(rawType)) {
            var beanClass = beanFactory.getType(beanName);
            if (beanClass == null) {
                continue;
            }

            var resolvedBeanType = ResolvableType.forClass(beanClass).as(rawType);
            if (resolvedBeanType == ResolvableType.NONE) {
                continue;
            }

            var supportedRequestType = resolvedBeanType.getGeneric(0);
            if (supportedRequestType == ResolvableType.NONE) {
                continue;
            }

            if (supportedRequestType.isAssignableFrom(ResolvableType.forClass(requestType))) {
                matchingBeanNames.add(beanName);
            }
        }
        return matchingBeanNames;
    }
}
