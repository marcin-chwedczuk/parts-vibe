package app.partsvibe.uicomponents.thymeleaf;

import static java.util.Collections.emptySet;
import static org.thymeleaf.standard.processor.StandardReplaceTagProcessor.PRECEDENCE;

import app.partsvibe.uicomponents.confirmation.ConfirmationDialogModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.TemplateManager;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEnd;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.ITemplateStart;
import org.thymeleaf.model.IText;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

class ConfirmationDialogElementTagProcessor extends AbstractElementModelProcessor {
    private static final String TAG_NAME = "confirmation-dialog";
    private static final String TEMPLATE_NAME = "ui/components/confirmation-dialog";
    private static final String METHOD_POST = "POST";
    private static final String SLOT_TAG_NAME = "slot";
    private static final String SLOT_NAME_ATTR = "name";
    private static final String SLOT_BODY = "body";
    private static final String DATA_VARIABLE = "data";
    private final String dialectPrefix;

    ConfirmationDialogElementTagProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, TAG_NAME, true, null, false, PRECEDENCE);
        this.dialectPrefix = dialectPrefix;
    }

    @Override
    protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
        IProcessableElementTag tag = firstElementTag(model);
        if (tag == null) {
            throw new TemplateProcessingException("No element tag found for <app:confirmation-dialog>");
        }

        String dataExpression = requireAttr(tag, "data");
        Object resolved = resolveExpression(context, dataExpression);
        if (resolved == null) {
            throw new TemplateProcessingException(
                    "Attribute '" + dialectPrefix + ":data' on <app:confirmation-dialog> resolved to null");
        }
        if (!(resolved instanceof ConfirmationDialogModel data)) {
            throw new TemplateProcessingException("Attribute '" + dialectPrefix
                    + ":data' on <app:confirmation-dialog> must resolve to "
                    + ConfirmationDialogModel.class.getSimpleName()
                    + ", got: " + resolved.getClass().getName());
        }

        ensureActionMethod(data.actionMethod());
        requireNonBlank(data.dialogId(), "dialogId");
        requireNonBlank(data.actionUrl(), "actionUrl");

        Map<String, List<ITemplateEvent>> providedSlots = extractProvidedSlots(model);
        if (!hasRenderableContent(providedSlots.get(SLOT_BODY))) {
            List<ITemplateEvent> defaultBody = extractDefaultBodyContent(model, providedSlots);
            if (hasRenderableContent(defaultBody)) {
                providedSlots.put(SLOT_BODY, defaultBody);
            }
        }

        structureHandler.setLocalVariable(DATA_VARIABLE, data);

        IModel componentTemplate = parseTemplateModel(context, TEMPLATE_NAME);
        IModel merged = fillTemplateSlots(context.getModelFactory(), componentTemplate, providedSlots);

        model.reset();
        model.addModel(merged);
    }

    private String requireAttr(IProcessableElementTag tag, String name) {
        String value = tag.getAttributeValue(dialectPrefix, name);
        if (value == null || value.isBlank()) {
            throw new TemplateProcessingException(
                    "Missing required attribute '" + dialectPrefix + ":" + name + "' on <app:confirmation-dialog>");
        }
        return value;
    }

    private void ensureActionMethod(String actionMethod) {
        String method = requireNonBlank(actionMethod, "actionMethod");
        if (!METHOD_POST.equalsIgnoreCase(method)) {
            throw new TemplateProcessingException(
                    "Attribute '" + dialectPrefix + ":data.actionMethod' on <app:confirmation-dialog> must be POST");
        }
    }

    private String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new TemplateProcessingException(
                    "Attribute '" + dialectPrefix + ":data." + name + "' on <app:confirmation-dialog> is required");
        }
        return value;
    }

    private static Object resolveExpression(ITemplateContext context, String expression) {
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        return parser.parseExpression(context, expression).execute(context);
    }

    private static IProcessableElementTag firstElementTag(IModel model) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i) instanceof IProcessableElementTag elementTag) {
                return elementTag;
            }
        }
        return null;
    }

    private static IModel parseTemplateModel(ITemplateContext context, String templateName) {
        TemplateManager templateManager = context.getConfiguration().getTemplateManager();
        TemplateModel templateModel =
                templateManager.parseStandalone(context, templateName, emptySet(), TemplateMode.HTML, true, true);
        return templateModel;
    }

    private Map<String, List<ITemplateEvent>> extractProvidedSlots(IModel invocationModel) {
        Map<String, List<ITemplateEvent>> slots = new HashMap<>();
        for (int i = 0; i < invocationModel.size(); i++) {
            ITemplateEvent event = invocationModel.get(i);
            if (event instanceof IProcessableElementTag slotTag && slotTag.hasAttribute(dialectPrefix, "slot")) {
                String slotName = slotTag.getAttributeValue(dialectPrefix, "slot");
                if (slots.containsKey(slotName)) {
                    throw new TemplateProcessingException(
                            "Duplicate slot definition '" + slotName + "' in <app:confirmation-dialog>");
                }
                List<ITemplateEvent> subtree = subTreeFrom(invocationModel, event);
                slots.put(slotName, innerContent(subtree));
            }
        }
        return slots;
    }

    private List<ITemplateEvent> extractDefaultBodyContent(
            IModel invocationModel, Map<String, List<ITemplateEvent>> providedSlots) {
        IProcessableElementTag root = firstElementTag(invocationModel);
        if (root == null) {
            return List.of();
        }
        List<ITemplateEvent> content = innerContent(subTreeFrom(invocationModel, root));
        List<ITemplateEvent> result = new ArrayList<>();

        for (int i = 0; i < content.size(); i++) {
            ITemplateEvent event = content.get(i);
            if (event instanceof IProcessableElementTag slotTag && slotTag.hasAttribute(dialectPrefix, "slot")) {
                i += subTreeFrom(content, i).size() - 1;
                continue;
            }
            result.add(event);
        }

        return result;
    }

    private IModel fillTemplateSlots(
            IModelFactory modelFactory, IModel templateModel, Map<String, List<ITemplateEvent>> providedSlots) {
        List<ITemplateEvent> events = eventsOf(templateModel);
        for (int i = 0; i < events.size(); i++) {
            ITemplateEvent event = events.get(i);
            if (!(event instanceof IProcessableElementTag slotTag)) {
                continue;
            }
            if (!isTemplateSlot(slotTag)) {
                continue;
            }

            String slotName = slotName(slotTag);
            List<ITemplateEvent> slotSubtree = subTreeFrom(events, i);
            List<ITemplateEvent> replacementContent = providedSlots.get(slotName);
            if (!hasRenderableContent(replacementContent)) {
                replacementContent = innerContent(slotSubtree);
            }

            int slotLength = slotSubtree.size();
            for (int j = 0; j < slotLength; j++) {
                events.remove(i);
            }
            events.addAll(i, replacementContent);
            i += Math.max(0, replacementContent.size() - 1);
        }

        IModel merged = modelFactory.createModel();
        for (ITemplateEvent event : events) {
            if (event instanceof ITemplateStart || event instanceof ITemplateEnd) {
                continue;
            }
            merged.add(event);
        }
        return merged;
    }

    private boolean isTemplateSlot(IProcessableElementTag tag) {
        if (!tag.getElementCompleteName().equals(dialectPrefix + ":" + SLOT_TAG_NAME)) {
            return false;
        }
        return tag.hasAttribute(dialectPrefix, SLOT_NAME_ATTR);
    }

    private String slotName(IProcessableElementTag slotTag) {
        return slotTag.getAttributeValue(dialectPrefix, SLOT_NAME_ATTR);
    }

    private static List<ITemplateEvent> subTreeFrom(IModel model, ITemplateEvent start) {
        List<ITemplateEvent> subtree = new ArrayList<>();
        boolean started = false;
        int openTags = 0;

        for (int i = 0; i < model.size(); i++) {
            ITemplateEvent event = model.get(i);
            if (event == start) {
                started = true;
                subtree.add(event);
                if (event instanceof IOpenElementTag) {
                    openTags = 1;
                    continue;
                }
                return subtree;
            }
            if (!started) {
                continue;
            }
            subtree.add(event);
            if (event instanceof IOpenElementTag) {
                openTags++;
            } else if (event instanceof ICloseElementTag) {
                openTags--;
                if (openTags == 0) {
                    break;
                }
            }
        }
        return subtree;
    }

    private static List<ITemplateEvent> subTreeFrom(List<ITemplateEvent> events, int startIndex) {
        List<ITemplateEvent> subtree = new ArrayList<>();
        ITemplateEvent start = events.get(startIndex);
        subtree.add(start);
        if (!(start instanceof IOpenElementTag)) {
            return subtree;
        }

        int openTags = 1;
        for (int i = startIndex + 1; i < events.size(); i++) {
            ITemplateEvent event = events.get(i);
            subtree.add(event);
            if (event instanceof IOpenElementTag) {
                openTags++;
            } else if (event instanceof ICloseElementTag) {
                openTags--;
                if (openTags == 0) {
                    break;
                }
            }
        }
        return subtree;
    }

    private static List<ITemplateEvent> innerContent(List<ITemplateEvent> subtree) {
        if (subtree.size() < 2) {
            return new ArrayList<>();
        }
        return new ArrayList<>(subtree.subList(1, subtree.size() - 1));
    }

    private static List<ITemplateEvent> eventsOf(IModel model) {
        List<ITemplateEvent> events = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            events.add(model.get(i));
        }
        return events;
    }

    private static boolean hasRenderableContent(List<ITemplateEvent> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }
        for (ITemplateEvent event : events) {
            if (event instanceof IText text) {
                if (!text.getText().trim().isEmpty()) {
                    return true;
                }
                continue;
            }
            return true;
        }
        return false;
    }
}
