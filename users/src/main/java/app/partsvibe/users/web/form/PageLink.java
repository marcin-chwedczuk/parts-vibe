package app.partsvibe.users.web.form;

import app.partsvibe.uicomponents.pagination.PaginationLinkModel;

public record PageLink(int pageNumber, String url) implements PaginationLinkModel {}
