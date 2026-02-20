package app.partsvibe.uicomponents.thymeleaf;

import static java.util.Collections.emptySet;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.TemplateManager;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

class PaginationElementTagProcessor extends AbstractElementModelProcessor {
    private static final int PRECEDENCE = 1000;
    private static final String TAG_NAME = "pagination";
    private static final String TEMPLATE_NAME = "ui/components/pagination";
    private static final String DATA_VARIABLE = "data";
    private final String dialectPrefix;

    PaginationElementTagProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, TAG_NAME, true, null, false, PRECEDENCE);
        this.dialectPrefix = dialectPrefix;
    }

    @Override
    protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
        IProcessableElementTag tag = firstElementTag(model);
        if (tag == null) {
            throw new TemplateProcessingException("No element tag found for <app:pagination>");
        }

        String dataExpression = requireAttr(tag, "data");
        Object data = resolveExpression(context, dataExpression);
        structureHandler.setLocalVariable(DATA_VARIABLE, data);

        IModel componentModel = parseTemplateModel(context, TEMPLATE_NAME);
        model.reset();
        model.addModel(componentModel);
    }

    private String requireAttr(IProcessableElementTag tag, String name) {
        String value = tag.getAttributeValue(dialectPrefix, name);
        if (value == null || value.isBlank()) {
            throw new TemplateProcessingException(
                    "Missing required attribute '" + dialectPrefix + ":" + name + "' on <app:pagination>");
        }
        return value;
    }

    private static IProcessableElementTag firstElementTag(IModel model) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i) instanceof IProcessableElementTag elementTag) {
                return elementTag;
            }
        }
        return null;
    }

    private static Object resolveExpression(ITemplateContext context, String expression) {
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        return parser.parseExpression(context, expression).execute(context);
    }

    private static IModel parseTemplateModel(ITemplateContext context, String templateName) {
        TemplateManager templateManager = context.getConfiguration().getTemplateManager();
        TemplateModel templateModel =
                templateManager.parseStandalone(context, templateName, emptySet(), TemplateMode.HTML, true, true);
        return templateModel;
    }
}
