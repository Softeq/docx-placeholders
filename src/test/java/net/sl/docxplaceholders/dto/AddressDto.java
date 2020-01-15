package net.sl.docxplaceholders.dto;

/**
 * <p/>
 * Created on 1/13/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class AddressDto {
    private String country;

    private String city;

    private String street;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }
}
