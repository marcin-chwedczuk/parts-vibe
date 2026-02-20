package app.partsvibe.uicomponents.thymeleaf;

import java.util.Set;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

public class UiComponentsDialect extends AbstractProcessorDialect {
    public static final String DIALECT_PREFIX = "app";

    public UiComponentsDialect() {
        super("PartsVibe UI Components Dialect", DIALECT_PREFIX, 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(
                new PaginationElementTagProcessor(dialectPrefix),
                new ConfirmationDialogElementTagProcessor(dialectPrefix));
    }
}
