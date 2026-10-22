package org.c3cavite.core.domain;

/** An organizer action required for a one-based group number in the result. */
public final class AssignmentBlocker {
  public enum Code {
    MISSING_LEADER,
    INVALID_LEADER_ROLE,
    GROUP_CAP_EXCEEDED,
    MINIMUM_GROUP_SIZE
  }

  private final Code code;
  private final int groupNumber;
  private final String message;

  AssignmentBlocker(Code code, int groupNumber, String message) {
    this.code = code;
    this.groupNumber = groupNumber;
    this.message = message;
  }

  public Code getCode() {
    return code;
  }

  public int getGroupNumber() {
    return groupNumber;
  }

  public String getMessage() {
    return message;
  }
}
