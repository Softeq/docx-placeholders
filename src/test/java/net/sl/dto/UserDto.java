package net.sl.dto;

/**
 * <p/>
 * Created on 1/13/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class UserDto {
    private String firstName;

    private String lastName;

    private AddressDto address;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }
}
