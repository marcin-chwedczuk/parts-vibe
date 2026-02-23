package app.partsvibe.storage.api;

public record DeleteFileResult(Status status) {
    public enum Status {
        DELETED,
        NOT_FOUND,
        FAILED
    }

    public static DeleteFileResult deleted() {
        return new DeleteFileResult(Status.DELETED);
    }

    public static DeleteFileResult notFound() {
        return new DeleteFileResult(Status.NOT_FOUND);
    }

    public static DeleteFileResult failed() {
        return new DeleteFileResult(Status.FAILED);
    }
}
