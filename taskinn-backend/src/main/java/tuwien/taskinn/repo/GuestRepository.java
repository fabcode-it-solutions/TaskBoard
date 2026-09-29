package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.Guest;

import java.time.LocalDate;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    /**
     * Find unique guest by combination of first name, last name and date of birth.
     *
     * @param firstName the first name
     * @param lastName the last name
     * @param dateOfBirth the date of birth
     * @return Return unique guest if found or nothing otherwise.
     */
    Optional<Guest> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(String firstName, String lastName,
            LocalDate dateOfBirth);

    /**
     * Find a guest by their email
     *
     * @param email the email address
     * @return an Optional containing the Guest if found, otherwise empty
     */
    Optional<Guest> findByEmail(String email);

    /**
     * Check if a guest exists by their email
     *
     * @param email the email address
     * @return true if a guest with the given email exists, otherwise false
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a guest with the given email exists, excluding the guest with the specified ID.
     *
     * @param email the email address to check for existence
     * @param id the guest ID to exclude from the search (typically the ID of the guest being updated)
     * @return {@code true} if another guest (with different ID) already uses this email, {@code false} otherwise
     */
    boolean existsByEmailAndIdNot(String email, Long id);
}
