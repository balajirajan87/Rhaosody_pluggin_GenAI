package com.bosch.rhapsody.constants;


public enum DocType {
                     REQUIREMENT_DOC("Requirement doc"),
                     REFERENCE_DOC("Reference doc"),
                     GUIDELINE_DOC("Guideline doc");

  private final String value;

  DocType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
