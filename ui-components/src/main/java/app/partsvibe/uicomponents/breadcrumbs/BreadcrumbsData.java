package app.partsvibe.uicomponents.breadcrumbs;

import java.util.List;

public record BreadcrumbsData(List<? extends BreadcrumbItemModel> items) implements BreadcrumbsModel {}
