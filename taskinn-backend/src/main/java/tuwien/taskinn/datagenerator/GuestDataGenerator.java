package tuwien.taskinn.datagenerator;

import com.github.javafaker.Faker;
import java.text.Normalizer;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.Guest;
import tuwien.taskinn.repo.GuestRepository;

/**
 * Generates a specified number of sample Guests on application startup with realistic, locale-specific data. This class
 * is designed to be called by a master data generator.
 */
@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class GuestDataGenerator {

    private final GuestRepository guestRepository;
    // --- Use the German (Austria) locale for Faker ---
    private final Faker faker = new Faker(new Locale("de-AT"));
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Generates a specified number of realistic guest profiles. This method is idempotent and will not run if guests
     * already exist.
     *
     * @param count The total number of guests to generate.
     * @return A list of all Guest entities in the database (either pre-existing or newly created).
     */
    public List<Guest> generateData(int count) {
        if (guestRepository.count() > 0) {
            log.info("Guests already exist. Skipping generation.");
            return guestRepository.findAll();
        }
        if (count <= 0) {
            return new ArrayList<>();
        }

        log.info("Generating {} new sample guests with Austrian locale...", count);
        List<Guest> guestsToCreate = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            guestsToCreate.add(createRandomGuest());
        }

        List<Guest> savedGuests = guestRepository.saveAll(guestsToCreate);
        log.info("Guest data generation complete. Created {} new guests.", savedGuests.size());

        return savedGuests;
    }

    /**
     * A private helper method to build a single Guest object with realistic, fake data.
     */
    private Guest createRandomGuest() {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();

        return Guest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(faker.date().birthday(18, 80).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .email(generateEmail(firstName, lastName))
                .phone(faker.phoneNumber().cellPhone())
                .build();
    }

    private String generateEmail(String firstName, String lastName) {
        String[] domains = {"gmail.com", "yahoo.com", "outlook.com", "gmx.at", "aon.at"};

        String fn = normalizeString(firstName);
        String ln = normalizeString(lastName);

        return String
                .format("%s.%s%d@%s", fn.substring(0, 1), ln, faker.number().numberBetween(10, 999),
                        faker.options().option(domains))
                .toLowerCase();
    }

    private String normalizeString(String input) {
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        return NON_LATIN.matcher(normalized).replaceAll("");
    }
}
