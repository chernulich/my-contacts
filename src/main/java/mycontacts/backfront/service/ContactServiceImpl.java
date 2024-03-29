package mycontacts.backfront.service;

import mycontacts.backfront.model.dto.AddressDto;
import mycontacts.backfront.model.dto.ContactDto;
import mycontacts.backfront.model.entity.Address;
import mycontacts.backfront.model.entity.PhoneNumber;
import mycontacts.backfront.model.entity.User;
import mycontacts.backfront.myException.PhoneNumberNotFoundException;
import mycontacts.backfront.myException.UserNotFoundException;
import mycontacts.backfront.repository.AddressRepository;
import mycontacts.backfront.repository.PhoneNumberRepository;
import mycontacts.backfront.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhoneNumberRepository phoneNumberRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Override
    public void createContact(ContactDto contactDto) {
        User user1 = User.builder()
                .fullName(contactDto.getFullName())
                .email(contactDto.getEmail())
                .createdDate(LocalDateTime.now())
                .build();
        userRepository.save(user1);
        createDataByUser(user1, contactDto);

    }

    private void createDataByUser(User user, ContactDto contactDto){
        List<Address> addresses = contactDto.getAddresses().stream().map(address -> AddressDtoToAddress(address, user))
                .collect(Collectors.toList());
        List<PhoneNumber> phoneNumbers = contactDto.getPhoneNumbers().stream()
                .map(phoneNumber -> new PhoneNumber(0L, user, LocalDateTime.now(), phoneNumber))
                .collect(Collectors.toList());
        phoneNumberRepository.saveAll(phoneNumbers);
        addressRepository.saveAll(addresses);
    }

    private Address AddressDtoToAddress(AddressDto addressDto, User user){
        return new Address(0L, user, LocalDateTime.now(), addressDto.getCountry(), addressDto.getCity(),
                addressDto.getStreet(), addressDto.getHouseNumber(), addressDto.getApartment());
    }

    @Override
    public void deleteContact(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User doesn't exist."));
        addressRepository.deleteAllByUser(user);
        phoneNumberRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

    @Override
    public void updateContact(ContactDto contactDto) {
        User user = userRepository.findUserByFullName(contactDto.getFullName());
        addressRepository.deleteAllByUser(user);
        phoneNumberRepository.deleteAllByUser(user);
        Long id = user.getId();
        userRepository.delete(user);

        User newUser = User.builder()
                .id(id)
                .fullName(contactDto.getFullName())
                .email(contactDto.getEmail())
                .createdDate(LocalDateTime.now())
                .build();

        userRepository.save(newUser);
        createDataByUser(newUser, contactDto);


    }


    @Override
    public List<ContactDto> getAllContacts() {
        List<User> users = userRepository.findAll();
        List<ContactDto> contacts = new ArrayList<>();
        for(User user : users){
            contacts.add(uniteContactDto(user, addressRepository.findAllByUser(user)));
        }
        return contacts;
    }

    private ContactDto uniteContactDto(User user, List<Address> addresses) {
        List<PhoneNumber> phoneNumbers = phoneNumberRepository.findAllByUser(user);
        List<AddressDto> addressesDto = new ArrayList<>();
        for(Address address : addresses){
            addressesDto.add(AddressDto.builder()
                    .country(address.getCountry())
                    .city(address.getCity())
                    .street(address.getStreet())
                    .apartment(address.getApartment())
                    .houseNumber(address.getHouseNumber())
                    .build());
        }
        return ContactDto.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumbers(phoneNumbers.stream().map(PhoneNumber::getPhoneNumber).collect(Collectors.toList()))
                .addresses(addressesDto)
                .build();
    }

    @Override
    public ContactDto getContactByPhoneNumber(String phoneNumber) {
        //List<PhoneNumber> phoneNumbers = phoneNumberRepository.findAll();
        PhoneNumber activeNumber = phoneNumberRepository.findByPhoneNumber(phoneNumber);
        User user = activeNumber.getUser();
        ContactDto contactDto = uniteContactDto(user, addressRepository.findAllByUser(user));
        return contactDto;
    }

    @Override
    public ContactDto getContactByName(String name) {
        User user = userRepository.findUserByFullName(name);
        ContactDto contactDto = uniteContactDto(user, addressRepository.findAllByUser(user));
        return contactDto;
    }

    @Override
    public List<User> getAllNames() {
        return userRepository.findAll();
    }
}
