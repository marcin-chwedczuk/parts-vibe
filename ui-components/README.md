# ui-components

Reusable Thymeleaf UI components for `parts-vibe`.

## Current components

- `app:pagination` renders the pagination component from `templates/ui/components/pagination.html`.

## Usage

Declare the dialect namespace on the template root:

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:app="http://partsvibe.app/ui-components">
```

Render pagination:

```html
<app:pagination app:data="${pageInfo}" />
```

Where `pageInfo` is an object exposing:
- `firstUrl`
- `lastUrl`
- `pageLinks`
- `totalPages`
- `currentPage`
- `pageNumbers`
- `startRow`
- `endRow`
- `totalRows`

Optional IDE support:
- XSD is provided at `META-INF/partsvibe-ui-components.xsd`.

## Extension

- Add a new processor class in `app.partsvibe.uicomponents.thymeleaf`.
- Register it in `UiComponentsDialect`.
- Add the fragment template under `templates/ui/components/`.
