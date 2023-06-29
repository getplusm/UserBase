package t.me.p1azmer.velocity.user.model;

public class SQLRuntimeException extends RuntimeException {

  public SQLRuntimeException(Throwable cause) {
    this("An unexpected internal error was caught during the database SQL operations.", cause);
  }

  public SQLRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
