package io.stormbird.wallet.ui;

public class HelpResponse {
    String FIRSTNAME;
    String LASTNAME;

    public String getFIRSTNAME() {
        return FIRSTNAME;
    }

    public void setFIRSTNAME(String firstName) {
        this.FIRSTNAME = firstName;
    }

    public String getLASTNAME() {
        return LASTNAME;
    }

    public void setLASTNAME(String lastName) {
        this.LASTNAME = lastName;
    }

    public HelpResponse(String firstName, String lastName) {
        this.FIRSTNAME = firstName;
        this.LASTNAME = lastName;
    }

    public HelpResponse() {
    }
}