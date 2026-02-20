# ui-components

Reusable Thymeleaf UI components for `parts-vibe`.

## Current components

- `app:pagination` renders the pagination component from `templates/ui/components/pagination.html`.
- `app:confirmation-dialog` renders a confirmation modal with form and slot-based content.

## Usage

Declare the dialect namespace on the template root:

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:app="http://partsvibe.app/ui-components">
```

Render pagination:

```html
<app:pagination app:data="${pageInfo}" />
```

Where `pageInfo` implements `app.partsvibe.uicomponents.pagination.PaginationModel`.

```html
<app:confirmation-dialog app:data="${deleteDialog}">
  <th:block app:slot="title">Confirm delete</th:block>
  <th:block app:slot="body">
    <p class="mb-0">Are you sure?</p>
  </th:block>
  <th:block app:slot="fields">
    <input type="hidden" name="_csrf" value="..." />
  </th:block>
  <th:block app:slot="cancel">Cancel</th:block>
  <th:block app:slot="confirm">Delete</th:block>
</app:confirmation-dialog>
```

Where `deleteDialog` implements `app.partsvibe.uicomponents.confirmation.ConfirmationDialogModel`.
Current security rule: `actionMethod` must be `POST`.
Supported slots: `title`, `body`, `fields`, `confirm`, `cancel`.
The component structure is defined in `templates/ui/components/confirmation-dialog.html`.

Optional IDE support:
- XSD is provided at `META-INF/partsvibe-ui-components.xsd`.

## Extension

- Add a new processor class in `app.partsvibe.uicomponents.thymeleaf`.
- Register it in `UiComponentsDialect`.
- Add the fragment template under `templates/ui/components/`.
