package com.bosch.rhapsody.constants;

/**
 * @author dhp4cob
 */
public class ProcessingException extends Exception {

  private static final long serialVersionUID = 2037820832580274661L;

  /**
   * Default constructor
   */
  public ProcessingException() {
    super();
  }

  /**
   * Constructor that accepts a message
   * 
   * @param message {@link String}
   */
  public ProcessingException(String message) {
    super(message);
  }

  /**
   * Constructor that accepts a message and a cause
   * 
   * @param message {@link String}
   * @param cause   {@link Throwable}
   */
  public ProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructor that accepts a cause
   * 
   * @param cause {@link Throwable}
   */
  public ProcessingException(Throwable cause) {
    super(cause);
  }
}
