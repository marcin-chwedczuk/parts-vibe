package app.partsvibe.storage.api;

public enum StorageObjectType {
    USER_AVATAR_IMAGE,
    CATEGORY_IMAGE,
    PART_IMAGE,
    PART_GALLERY_IMAGE,
    PART_ATTACHMENT;

    public boolean isImage() {
        return this != PART_ATTACHMENT;
    }
}
