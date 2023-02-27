package com.alphawallet.app.util;

public class Client {

    private final  String name;
    private final String surname;
    private final String hawaAlias;
    private final String address;
    private final String number;

    public Client(String name, String surname, String hawaAlias, String address, String number){
        this.name = name;
        this.surname = surname;
        this.hawaAlias = hawaAlias;
        this.address = address;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getHawaAlias() {
        return hawaAlias;
    }

    public String getAddress() {
        return address;
    }

    public String getNumber() {
        return number;
    }

}
